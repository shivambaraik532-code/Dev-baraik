package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class Repository(
    private val db: AppDatabase
) {
    val postsFlow: Flow<List<PostEntity>> = db.postDao().getAllPosts()
    val storiesFlow: Flow<List<StoryEntity>> = db.storyDao().getAllStories()

    suspend fun insertPost(post: PostEntity) = db.postDao().insertPost(post)
    suspend fun updatePost(post: PostEntity) = db.postDao().updatePost(post)

    suspend fun insertStory(story: StoryEntity) = db.storyDao().insertStory(story)
    suspend fun updateStory(story: StoryEntity) = db.storyDao().updateStory(story)

    fun getMessagesBetween(user1: String, user2: String): Flow<List<MessageEntity>> =
        db.messageDao().getMessagesBetween(user1, user2)

    fun getContactsFlow(currentUser: String): Flow<List<Contact>> {
        return db.messageDao().getAllUserMessages(currentUser).map { messages ->
            val partners = messages.map { if (it.sender == currentUser) it.receiver else it.sender }
                .distinct()
                .filter { it != currentUser }
            
            // If we have nobody we talked to, make sure the preset contacts are displayed
            val allContactsList = if (partners.isEmpty()) {
                listOf("Alice", "Bob", "Charlie", "Diana")
            } else {
                (partners + listOf("Alice", "Bob", "Charlie", "Diana")).distinct()
            }

            allContactsList.map { partner ->
                val partnerMessages = messages.filter { 
                    (it.sender == partner && it.receiver == currentUser) || 
                    (it.sender == currentUser && it.receiver == partner) 
                }
                val latest = partnerMessages.maxByOrNull { it.timestamp }
                val lastText = latest?.text ?: "Swipe left to chat!"
                val lastTime = latest?.timestamp ?: (System.currentTimeMillis() - 3600000 * 2)
                
                Contact(
                    username = partner,
                    avatarUrl = getAvatarForUser(partner),
                    lastMessageText = lastText,
                    lastMessageTime = lastTime,
                    hasUnread = latest != null && latest.sender == partner && latest.timestamp > System.currentTimeMillis() - 5000
                )
            }.sortedByDescending { it.lastMessageTime }
        }
    }

    suspend fun insertMessage(message: MessageEntity) = db.messageDao().insertMessage(message)

    fun getAvatarForUser(user: String): String {
        return when (user.lowercase()) {
            "alice" -> "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=200"
            "bob" -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200"
            "charlie" -> "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&q=80&w=200"
            "diana" -> "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&q=80&w=200"
            "shivam", "me" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200"
            else -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=200"
        }
    }

    suspend fun checkAndSeedDatabase() {
        val posts = db.postDao().getAllPosts().first()
        if (posts.isEmpty()) {
            seedDatabase()
        }
    }

    private suspend fun seedDatabase() {
        // Seed Posts
        val seedPosts = listOf(
            PostEntity(
                username = "Alice",
                userAvatar = getAvatarForUser("Alice"),
                imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&q=80&w=800",
                caption = "Made this beautiful gourmet summer bowl! Cooking is healing. ✨ #healthy #aesthetic",
                likesCount = 142,
                isLiked = true,
                filterApplied = "Warm",
                timestamp = System.currentTimeMillis() - 3600000 * 2
            ),
            PostEntity(
                username = "Bob",
                userAvatar = getAvatarForUser("Bob"),
                imageUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&q=80&w=800",
                caption = "Lost in the wilderness. The morning mist here is just magic. 🌲🌄",
                likesCount = 89,
                isLiked = false,
                filterApplied = "Vintage",
                timestamp = System.currentTimeMillis() - 3600000 * 4
            ),
            PostEntity(
                username = "Charlie",
                userAvatar = getAvatarForUser("Charlie"),
                imageUrl = "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?auto=format&fit=crop&q=80&w=800",
                caption = "Clean geometric designs in city architecture. Minimalism at its peak. 🏢",
                likesCount = 205,
                isLiked = false,
                filterApplied = "Mono",
                timestamp = System.currentTimeMillis() - 3600000 * 8
            ),
            PostEntity(
                username = "Diana",
                userAvatar = getAvatarForUser("Diana"),
                imageUrl = "https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&q=80&w=800",
                caption = "Friday night lights! Capturing color flares. 🎇💜 #mood #photography",
                likesCount = 312,
                isLiked = true,
                filterApplied = "Clarendon",
                timestamp = System.currentTimeMillis() - 3600000 * 12
            )
        )
        db.postDao().insertAll(seedPosts)

        // Seed Stories
        val seedStories = listOf(
            StoryEntity(
                username = "Alice",
                userAvatar = getAvatarForUser("Alice"),
                imageUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&q=80&w=400",
                caption = "Team meeting lunch!",
                timestamp = System.currentTimeMillis()
            ),
            StoryEntity(
                username = "Bob",
                userAvatar = getAvatarForUser("Bob"),
                imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=400",
                caption = "On early trains...",
                timestamp = System.currentTimeMillis() - 1200000
            ),
            StoryEntity(
                username = "Charlie",
                userAvatar = getAvatarForUser("Charlie"),
                imageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=400",
                caption = "Studio setups setup.",
                timestamp = System.currentTimeMillis() - 2400000
            ),
            StoryEntity(
                username = "Diana",
                userAvatar = getAvatarForUser("Diana"),
                imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400",
                caption = "Golden hour is real",
                timestamp = System.currentTimeMillis() - 5000000
            )
        )
        db.storyDao().insertAll(seedStories)

        // Seed Messages
        val seedMessages = listOf(
            MessageEntity(
                sender = "Alice",
                receiver = "me",
                text = "Hey! Did you check out the new photo filters I used on my recipe post?",
                timestamp = System.currentTimeMillis() - 360000 * 5
            ),
            MessageEntity(
                sender = "me",
                receiver = "Alice",
                text = "Yes, they look super warm and delightful! Absolutely loved the gourmet bowl.",
                timestamp = System.currentTimeMillis() - 360000 * 4
            ),
            MessageEntity(
                sender = "Alice",
                receiver = "me",
                text = "Awesome!! Try sending me a photo with the Clarendon filter sometime!",
                timestamp = System.currentTimeMillis() - 360000 * 3
            ),
            MessageEntity(
                sender = "Bob",
                receiver = "me",
                text = "Yo! Let's go hiking this weekend. I want to take some scenic foggy shots.",
                timestamp = System.currentTimeMillis() - 360000 * 10
            )
        )
        db.messageDao().insertAll(seedMessages)
    }
}
