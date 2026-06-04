package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.Contact
import com.example.data.MessageEntity
import com.example.data.PostEntity
import com.example.data.Repository
import com.example.data.StoryEntity
import com.example.ui.FilterUtils
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = Repository(db)
        val viewModel = MainViewModel(repository)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainLayout(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    // Nice Instagram-styled Brand Gradient for Story boundaries
    val storyGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF8134AF), // Deep Purple
            Color(0xFFDD2A7B), // Magenta Pink
            Color(0xFFF58529), // Bright Orange
            Color(0xFFFCD116), // Gold Yellow
            Color(0xFF8134AF)  // Loop back
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Handle transitions safely using AnimatedContent
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState is Screen.Chat || targetState is Screen.StoryViewer) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) with
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300))
                } else {
                    fadeIn(animationSpec = tween(200)) with fadeOut(animationSpec = tween(200))
                }
            },
            label = "screen_navigation"
        ) { screen ->
            when (screen) {
                is Screen.Home -> FeedScreen(viewModel = viewModel, storyGradient = storyGradient)
                is Screen.DirectMessages -> InboxScreen(viewModel = viewModel)
                is Screen.Chat -> ChatDetailScreen(viewModel = viewModel, partnerName = screen.partner)
                is Screen.StoryViewer -> StoryViewerScreen(viewModel = viewModel, initialIndex = screen.activeIndex)
                is Screen.AddPost -> AddPostScreen(viewModel = viewModel)
                is Screen.Profile -> ProfileScreen(viewModel = viewModel)
            }
        }

        // Floating dynamic bottom tabs bar, only shown when not viewing Fullscreen Stories or active Chat
        val showBottomBar = currentScreen !is Screen.StoryViewer && currentScreen !is Screen.Chat
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("nav_feed_tab")
                    ) {
                        Icon(
                            imageVector = if (currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home Feed",
                            tint = if (currentScreen is Screen.Home) activeColor else inactiveColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AddPost) },
                        modifier = Modifier.testTag("nav_add_tab")
                    ) {
                        Icon(
                            imageVector = if (currentScreen is Screen.AddPost) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                            contentDescription = "New Post",
                            tint = if (currentScreen is Screen.AddPost) activeColor else inactiveColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.DirectMessages) },
                        modifier = Modifier.testTag("nav_inbox_tab")
                    ) {
                        BadgedBox(
                            badge = {
                                // Add a subtle badge indicating live chats
                                Badge(
                                    containerColor = Color(0xFFDD2A7B),
                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                ) {}
                            }
                        ) {
                            Icon(
                                imageVector = if (currentScreen is Screen.DirectMessages) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Direct Messages",
                                tint = if (currentScreen is Screen.DirectMessages) activeColor else inactiveColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Profile) },
                        modifier = Modifier.testTag("nav_profile_tab")
                    ) {
                        Icon(
                            imageVector = if (currentScreen is Screen.Profile) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "My Profile",
                            tint = if (currentScreen is Screen.Profile) activeColor else inactiveColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// FEED SCREEN (HOME)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: MainViewModel,
    storyGradient: Brush
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
    ) {
        // App Header
        TopAppBar(
            title = {
                Text(
                    text = "Instagraph",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("app_brand_logo")
                )
            },
            actions = {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.DirectMessages) },
                    modifier = Modifier.testTag("header_dm_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "DMs",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Stories Header Carousel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // User's own active Quick Story addition
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    viewModel.navigateTo(Screen.AddPost)
                                }
                            ) {
                                Box(
                                    modifier = Modifier.size(68.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
                                        contentDescription = "My avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDD2A7B))
                                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add story",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your Story",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Loaded stories
                        items(stories.size) { index ->
                            val story = stories[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .testTag("story_bubble_${story.username}")
                                    .clickable {
                                        viewModel.navigateTo(Screen.StoryViewer(index))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .border(
                                            border = BorderStroke(
                                                width = 2.5.dp,
                                                brush = if (story.hasUnseen) storyGradient else Brush.linearGradient(
                                                    listOf(Color.LightGray, Color.LightGray)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                ) {
                                    AsyncImage(
                                        model = story.userAvatar,
                                        contentDescription = "${story.username} avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.username,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Feed posts list
            if (posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLikeClicked = { viewModel.toggleLike(post) },
                        onChatClicked = { viewModel.selectChatPartner(post.username) }
                    )
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostEntity,
    onLikeClicked: () -> Unit,
    onChatClicked: () -> Unit
) {
    var isLikeBouncing by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (isLikeBouncing) 1.3f else 1.0f,
        animationSpec = tween(150),
        finishedListener = { isLikeBouncing = false }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.username}")
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            // Header Row (Avatar, Name, Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.userAvatar,
                    contentDescription = "${post.username} avatar",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (post.filterApplied != "Normal") {
                        Text(
                            text = "Filter: ${post.filterApplied}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Image Container with ColorFilter Applied live!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post content photo",
                    colorFilter = FilterUtils.getColorFilter(post.filterApplied),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Interactive Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isLikeBouncing = true
                        onLikeClicked()
                    },
                    modifier = Modifier.scale(scaleFactor)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like button",
                        tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onChatClicked) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = "Comment and Direct Message",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))

                // Custom badge highlighting filter applied
                if (post.filterApplied != "Normal") {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.filterApplied,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Likes and Captions section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${post.likesCount} likes",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${post.username} ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = post.caption,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STORY VIEWER SCREEN
// -------------------------------------------------------------
@Composable
fun StoryViewerScreen(
    viewModel: MainViewModel,
    initialIndex: Int
) {
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    var currentIndex by remember { mutableStateOf(initialIndex) }
    var userTextReply by remember { mutableStateOf("") }

    if (stories.isEmpty() || currentIndex !in stories.indices) {
        viewModel.navigateBack()
        return
    }

    val story = stories[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Main Story Image Fit Fullscreen
        AsyncImage(
            model = story.imageUrl,
            contentDescription = "Story active slide",
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            contentScale = ContentScale.Fit
        )

        // Overlay Interactive zones for simple tap-to-navigate (Left and Right halves)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.0f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex > 0) {
                            currentIndex--
                        } else {
                            viewModel.navigateBack()
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.0f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentIndex < stories.size - 1) {
                            currentIndex++
                        } else {
                            viewModel.navigateBack()
                        }
                    }
            )
        }

        // Top UI items (Progress bar overlay & Sender Profile)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            // Segments of progress lines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val isViewed = idx < currentIndex
                    val isActive = idx == currentIndex
                    LinearProgressIndicator(
                        progress = if (isViewed) 1.0f else if (isActive) 0.6f else 0f,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Sender Avatar, Name and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = story.userAvatar,
                    contentDescription = "${story.username} story avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = story.username,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    if (story.caption.isNotEmpty()) {
                        Text(
                            text = story.caption,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("story_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close story viewer",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom UI section: Send a quick reply straight to user's DMs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            OutlinedTextField(
                value = userTextReply,
                onValueChange = { userTextReply = it },
                placeholder = { Text("Send reply to ${story.username}...", color = Color.White.copy(0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("story_reply_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(0.5f),
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (userTextReply.isNotBlank()) {
                            viewModel.sendMessage(story.username, userTextReply)
                            userTextReply = ""
                            // Go back with visual dynamic feedback
                            viewModel.navigateBack()
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (userTextReply.isNotBlank()) {
                                viewModel.sendMessage(story.username, userTextReply)
                                userTextReply = ""
                                viewModel.navigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send story DM reply",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// ADD POST SCREEN (WITH PHOTO SELECTION AND LIVE FILTERS)
// -------------------------------------------------------------
@Composable
fun AddPostScreen(
    viewModel: MainViewModel
) {
    val selectedImage by viewModel.selectedImageToUpload.collectAsStateWithLifecycle()
    val customUrl by viewModel.customImageUrl.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val captionText by viewModel.captionText.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "New Post",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live active photo preview with selected Filter Applied
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedImage,
                        contentDescription = "Selected photo preview",
                        colorFilter = FilterUtils.getColorFilter(activeFilter),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Active filter label badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Filter: $activeFilter",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Filters selector carousel (The key filter chips display)
            item {
                Column {
                    Text(
                        text = "Visual Filters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FilterUtils.FilterList) { filterName ->
                            val isSelected = filterName == activeFilter
                            Box(
                                modifier = Modifier
                                    .testTag("filter_chip_$filterName")
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectFilter(filterName) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filterName,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Camera presets carousel
            item {
                Column {
                    Text(
                        text = "Capture / Select Photo Preset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.presetImages) { url ->
                            val isChosen = url == selectedImage
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectPresetImage(url) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "preset choice",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Custom URL entry
            item {
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { viewModel.setCustomImageUrl(it) },
                    label = { Text("Or paste image URL link") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Caption details
            item {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { viewModel.updateCaption(it) },
                    label = { Text("Write a gorgeous caption...") },
                    placeholder = { Text("What details are behind this story? #warm #instagraph") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_post_caption_input")
                        .height(100.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Publish Button triggers sharing
            item {
                Button(
                    onClick = { viewModel.sharePost() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_post_btn")
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Share to Feed and Story",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIRECT MESSAGES (INBOX SCREEN)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: MainViewModel
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "Messages Inbox",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            if (contacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active conversations",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(contacts, key = { it.username }) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { viewModel.selectChatPartner(contact.username) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: Contact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inbox_row_${contact.username}")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red unseen indicator
        Box(
            modifier = Modifier.size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (contact.hasUnread) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDD2A7B))
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        // Avatar
        AsyncImage(
            model = contact.avatarUrl,
            contentDescription = "${contact.username} avatar",
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Text details (last message, username)
        Column(
            modifier = Modifier.weight(1.0f)
        ) {
            Text(
                text = contact.username,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.lastMessageText,
                fontSize = 12.sp,
                color = if (contact.hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (contact.hasUnread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Open chat",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// -------------------------------------------------------------
// CHAT DETAIL SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    partnerName: String
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to latest message on receive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = viewModel.getAvatarForUser(partnerName),
                        contentDescription = "$partnerName avatar",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = partnerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back navigation"
                    )
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Voice Call",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        )

        // Messaging list
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isMe = message.sender == "me"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 16.dp
                                )
                            )
                            .background(
                                if (isMe) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Column {
                            if (message.mediaUrl != null) {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Chat media attached",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .padding(bottom = 4.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (message.text.isNotEmpty()) {
                                Text(
                                    text = message.text,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Quick Photo Presets attachment bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tap to Send Photo DM:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            viewModel.presetImages.take(4).forEach { url ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            viewModel.sendMessage(partnerName, "Sent an attachment!", url)
                        }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Quick send attachment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Bottom text field entry row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Write message here...") },
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(26.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(partnerName, textInput)
                                textInput = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(partnerName, textInput)
                            textInput = ""
                        }
                    }
                )
            )
        }
    }
}

// -------------------------------------------------------------
// PROFILE GALLERY AND DETAIL SCREEN
// -------------------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: MainViewModel
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userBio by viewModel.userBio.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }

    // User's own posts list
    val myPosts = posts.filter { it.username == "me" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top identity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Details header area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
                contentDescription = "Own profile avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(36.dp))

            // Stat columns
            Row(
                modifier = Modifier.weight(1.0f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProfileStatColumn(count = myPosts.size.toString(), label = "Posts")
                ProfileStatColumn(count = "1.8K", label = "Followers")
                ProfileStatColumn(count = "460", label = "Following")
            }
        }

        // Bio section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = userName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = userBio,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Edit Profile Info",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))

        // Gallery Grid of Post Items
        if (myPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "no posts",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No shared posts yet",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_posts_grid"),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(myPosts) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.0f)
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "grid item",
                            colorFilter = FilterUtils.getColorFilter(post.filterApplied),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Filter label watermark
                        if (post.filterApplied != "Normal") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(0.4f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = post.filterApplied,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Profile Dialog Form
    if (showEditDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempBio by remember { mutableStateOf(userBio) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Identity profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Biography details") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateProfile(tempName, tempBio)
                                showEditDialog = false
                            },
                            modifier = Modifier.testTag("save_profile_btn")
                        ) {
                            Text("Save details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatColumn(
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
