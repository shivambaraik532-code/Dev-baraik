package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MessageEntity
import com.example.data.PostEntity
import com.example.data.Repository
import com.example.data.StoryEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object DirectMessages : Screen()
    data class Chat(val partner: String) : Screen()
    data class StoryViewer(val activeIndex: Int) : Screen()
    object AddPost : Screen()
    object Profile : Screen()
}

class MainViewModel(private val repository: Repository) : ViewModel() {

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Screen stack to support proper back navigation!
    private val navigationStack = mutableListOf<Screen>(Screen.Home)

    fun navigateTo(screen: Screen) {
        if (screen is Screen.StoryViewer || screen is Screen.Chat || screen != _currentScreen.value) {
            navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            _currentScreen.value = navigationStack.removeAt(navigationStack.size - 1)
        } else {
            _currentScreen.value = Screen.Home
        }
    }

    // Initialize database
    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    // Posts stream
    val posts: StateFlow<List<PostEntity>> = repository.postsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stories stream
    val stories: StateFlow<List<StoryEntity>> = repository.storiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Messages Contacts inbox list
    val contacts = repository.getContactsFlow("me")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat recipient
    private val _activeChatPartner = MutableStateFlow<String>("")
    val activeChatPartner = _activeChatPartner.asStateFlow()

    fun selectChatPartner(partner: String) {
        _activeChatPartner.value = partner
        navigateTo(Screen.Chat(partner))
    }

    // Active Chat messages stream
    val chatMessages: StateFlow<List<MessageEntity>> = _activeChatPartner
        .flatMapLatest { partner ->
            repository.getMessagesBetween("me", partner)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Creating post states
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&q=80&w=600", // Sunset
        "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&q=80&w=600", // Cyberpunk Neon
        "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?auto=format&fit=crop&q=80&w=600", // Match cup
        "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&q=80&w=600", // Cozy books
        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=600", // Kitty cat
        "https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&q=80&w=600"  // Synthwave arcade
    )

    private val _selectedImageToUpload = MutableStateFlow(presetImages[0])
    val selectedImageToUpload = _selectedImageToUpload.asStateFlow()

    private val _customImageUrl = MutableStateFlow("")
    val customImageUrl = _customImageUrl.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Normal")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _captionText = MutableStateFlow("")
    val captionText = _captionText.asStateFlow()

    fun selectPresetImage(url: String) {
        _selectedImageToUpload.value = url
        _customImageUrl.value = ""
    }

    fun setCustomImageUrl(url: String) {
        _customImageUrl.value = url
        if (url.isNotEmpty()) {
            _selectedImageToUpload.value = url
        }
    }

    fun selectFilter(filterName: String) {
        _selectedFilter.value = filterName
    }

    fun updateCaption(text: String) {
        _captionText.value = text
    }

    fun sharePost() {
        val imageUrl = _selectedImageToUpload.value
        val caption = _captionText.value
        val filter = _selectedFilter.value

        viewModelScope.launch {
            val newPost = PostEntity(
                username = "me",
                userAvatar = repository.getAvatarForUser("me"),
                imageUrl = imageUrl,
                caption = caption,
                filterApplied = filter,
                likesCount = 0,
                isLiked = false
            )
            repository.insertPost(newPost)
            
            // Logically also auto-add as a story to give direct visual updates!
            repository.insertStory(
                StoryEntity(
                    username = "me",
                    userAvatar = repository.getAvatarForUser("me"),
                    imageUrl = imageUrl,
                    caption = caption
                )
            )

            // Reset states and return to feed
            _captionText.value = ""
            _selectedFilter.value = "Normal"
            _selectedImageToUpload.value = presetImages[0]
            _customImageUrl.value = ""
            _currentScreen.value = Screen.Home
        }
    }

    // Like system
    fun toggleLike(post: PostEntity) {
        viewModelScope.launch {
            val updated = post.copy(
                isLiked = !post.isLiked,
                likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
            )
            repository.updatePost(updated)
        }
    }

    // Messages action
    fun sendMessage(partner: String, text: String, mediaUrl: String? = null) {
        if (text.isBlank() && mediaUrl == null) return

        viewModelScope.launch {
            val userMsg = MessageEntity(
                sender = "me",
                receiver = partner,
                text = text,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(userMsg)

            // Auto-trigger realistic simulated reply from the message partner 1.5 seconds later
            delay(1500)
            val responseText = getSimulatedReplyForUser(partner, text, mediaUrl != null)
            val replyMsg = MessageEntity(
                sender = partner,
                receiver = "me",
                text = responseText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(replyMsg)
        }
    }

    private fun getSimulatedReplyForUser(username: String, userText: String, wasMedia: Boolean): String {
        if (wasMedia) {
            return when (username.lowercase()) {
                "alice" -> "OMG! Love this photo so much! The filter applied is perfect! 🤩💖"
                "bob" -> "Wow. That makes a killer wallpaper. Unreal view."
                "charlie" -> "Technically pristine! Capturing contrast brilliantly."
                "diana" -> "Vibes are immaculate! Makes me want to go out!"
                else -> "Awesome photo! Let's get coffee soon."
            }
        }

        val textLower = userText.lowercase()
        return when {
            textLower.contains("hello") || textLower.contains("hey") || textLower.contains("hi") -> {
                when (username.lowercase()) {
                    "alice" -> "Hey sweetie! How's your week going? Ready for coffee? ☕️"
                    "bob" -> "Yo! What's up? Planning the next hike."
                    "charlie" -> "Hello. Working on some graphics templates. How can I help?"
                    "diana" -> "Hey! Just saw your feed update, looks great."
                    else -> "Hey there!"
                }
            }
            textLower.contains("coffee") || textLower.contains("meet") || textLower.contains("cafe") -> {
                "Let's totally do coffee this Friday afternoon! I know an absolutely aesthetic cafe in downtown. ✨"
            }
            textLower.contains("filter") || textLower.contains("photo") -> {
                "I usually prefer Clarendon or Vintage to make the colors pop out of the frame! Try them."
            }
            else -> {
                when (username.lowercase()) {
                    "alice" -> "That sounds absolutely wonderful! Talk soon! 💕"
                    "bob" -> "Understood. Talk to you later, catch you on the trails."
                    "charlie" -> "Fascinating perspective. I'll make sure to reflect that design model."
                    "diana" -> "Super cool! Let me know when you post next!"
                    else -> "Nice! Talk to you soon."
                }
            }
        }
    }

    // Profile state details (simulated editable biography)
    private val _userBio = MutableStateFlow("Mobile Designer & Coffee Enthusiast. Applying vintage filters to everyday life. ☕️✨")
    val userBio = _userBio.asStateFlow()

    private val _userName = MutableStateFlow("shivam_co")
    val userName = _userName.asStateFlow()

    fun updateProfile(name: String, bio: String) {
        _userName.value = name
        _userBio.value = bio
    }

    fun getAvatarForUser(user: String): String {
        return repository.getAvatarForUser(user)
    }
}

class MainViewModelFactory(private val repository: Repository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
