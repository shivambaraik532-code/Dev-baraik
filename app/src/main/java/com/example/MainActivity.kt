package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
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
                    // Set up context refresh on start
                    val context = LocalContext.current
                    LaunchedEffect(Unit) {
                        viewModel.refreshNetworkState(context)
                    }

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
    val isPortBound by viewModel.isPortBound.collectAsStateWithLifecycle()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Periodically update IP addresses
    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refreshNetworkState(context)
            delay(10000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Custom Dashboard Status Header
            HeaderBar(
                isPortBound = isPortBound,
                isSimulatorMode = isSimulatorMode,
                viewModel = viewModel
            )

            // Dynamic screen container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState is Screen.Chat) {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        } else {
                            fadeIn(animationSpec = tween(150)) with fadeOut(animationSpec = tween(150))
                        }
                    },
                    label = "screen_navigation"
                ) { screen ->
                    when (screen) {
                        is Screen.Peers -> PeersScreen(viewModel = viewModel)
                        is Screen.BulletinBoard -> BulletinBoardScreen(viewModel = viewModel)
                        is Screen.Chat -> ChatScreen(viewModel = viewModel, partnerName = screen.partner)
                        is Screen.NetworkDetails -> NetworkDetailsScreen(viewModel = viewModel)
                        is Screen.Profile -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }

            // High Contrast Bottom M3 Tab Menu
            val showBottomBar = currentScreen !is Screen.Chat
            if (showBottomBar) {
                BottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// HEADERS & FOOTERS COMPONENTS
// -------------------------------------------------------------
@Composable
fun HeaderBar(
    isPortBound: Boolean,
    isSimulatorMode: Boolean,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OffGrid",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("app_brand_logo")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Connectivity status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPortBound) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else Color(0xFFFF9800).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isPortBound) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPortBound) "Port Bound" else "Simulating Area",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPortBound) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                    }
                }
                Text(
                    text = "Decentralized Mesh • No Recharge Needed",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { 
                        viewModel.refreshNetworkState(context)
                        viewModel.broadcastDiscoveryHeartbeat()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh offline network adapters",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Simulator tag toggle quick action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSimulatorMode) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleSimulatorMode(!isSimulatorMode) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSimulatorMode) "Sim Dynamic Match" else "Physical LAN Only",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulatorMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp)
        ) {
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

            NavigationBarItem(
                selected = currentScreen is Screen.Peers,
                onClick = { onNavigate(Screen.Peers) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Peers) Icons.Filled.NearMe else Icons.Outlined.NearMe,
                        contentDescription = "Peers Screen Launcher"
                    )
                },
                label = { Text("Nearby Peers", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    unselectedIconColor = inactiveColor,
                    selectedTextColor = activeColor,
                    unselectedTextColor = inactiveColor
                ),
                modifier = Modifier.testTag("nav_peers_tab")
            )

            NavigationBarItem(
                selected = currentScreen is Screen.BulletinBoard,
                onClick = { onNavigate(Screen.BulletinBoard) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.BulletinBoard) Icons.Filled.CellTower else Icons.Outlined.CellTower,
                        contentDescription = "OffGrid bulletin section launcher"
                    )
                },
                label = { Text("Public Board", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    unselectedIconColor = inactiveColor,
                    selectedTextColor = activeColor,
                    unselectedTextColor = inactiveColor
                ),
                modifier = Modifier.testTag("nav_board_tab")
            )

            NavigationBarItem(
                selected = currentScreen is Screen.NetworkDetails,
                onClick = { onNavigate(Screen.NetworkDetails) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.NetworkDetails) Icons.Filled.Dns else Icons.Outlined.Dns,
                        contentDescription = "Diagnostics Launcher"
                    )
                },
                label = { Text("Diagnostics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    unselectedIconColor = inactiveColor,
                    selectedTextColor = activeColor,
                    unselectedTextColor = inactiveColor
                ),
                modifier = Modifier.testTag("nav_diag_tab")
            )

            NavigationBarItem(
                selected = currentScreen is Screen.Profile,
                onClick = { onNavigate(Screen.Profile) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen is Screen.Profile) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                        contentDescription = "My profile screen launcher"
                    )
                },
                label = { Text("Profile ID", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    unselectedIconColor = inactiveColor,
                    selectedTextColor = activeColor,
                    unselectedTextColor = inactiveColor
                ),
                modifier = Modifier.testTag("nav_profile_tab")
            )
        }
    }
}

// -------------------------------------------------------------
// 1. NEARBY PEERS SCREEN (HOME / DISCOVERY)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeersScreen(
    viewModel: MainViewModel
) {
    val contactsList by viewModel.contacts.collectAsStateWithLifecycle()
    val localIp by viewModel.localIpAddress.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Instruction card explaining zero recharge operation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wifi connection info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "How to chat offline without Recharge:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Connect to the same Wi-Fi router or start a mobile hotspot on one phone and connect others to it. You don't need internet or cellular balance! The app discovers peers instantly.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Radio Terminals (${contactsList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "My IP: $localIp",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (contactsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Searching wifi peers",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning Offline Frequencies...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Check that your Wi-Fi is turned on, or connect nodes locally. Tap 'Manual Heartbeat' in Diagnostic menu.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contactsList, key = { it.username }) { contact ->
                    PeerCard(
                        contact = contact,
                        onChatClicked = { viewModel.selectChatPartner(contact.username) }
                    )
                }
            }
        }
    }
}

@Composable
fun PeerCard(
    contact: Contact,
    onChatClicked: () -> Unit
) {
    Card(
        onClick = onChatClicked,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("peer_contact_${contact.username}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = contact.avatarUrl,
                    contentDescription = contact.username,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Active status beacon
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (contact.isOnline) Color(0xFF4CAF50) else Color(0xFF757575))
                        .align(Alignment.BottomEnd)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "@${contact.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = contact.distance,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = contact.lastMessageText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Tech indicator row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = contact.connectionType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (contact.hasUnread) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF5252))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW PACKET",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat with partner",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. DECENTRALIZED BULLETIN BOARD SCREEN
// -------------------------------------------------------------
@Composable
fun BulletinBoardScreen(
    viewModel: MainViewModel
) {
    val bulletins by viewModel.bulletins.collectAsStateWithLifecycle()
    var userPostText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Post box
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Broadcast Local Shout / Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = userPostText,
                    onValueChange = { userPostText = it },
                    placeholder = { Text("What is happening nearby? Type a zero-data local broadcast packet...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_post_caption_input")
                        .height(84.dp),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (userPostText.isNotBlank()) {
                            viewModel.publishBulletin(userPostText)
                            userPostText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (userPostText.isNotBlank()) {
                            viewModel.publishBulletin(userPostText)
                            userPostText = ""
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("publish_post_btn")
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send offline packets", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Packet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Global Local Shoutbox Frequency",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (bulletins.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No public bulletins caught around this router node.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bulletins, key = { it.id }) { post ->
                    BulletinCard(
                        post = post,
                        onLikeClicked = { viewModel.toggleLikePost(post) }
                    )
                }
            }
        }
    }
}

@Composable
fun BulletinCard(
    post: PostEntity,
    onLikeClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.username}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: User details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = post.userAvatar,
                    contentDescription = post.username,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "@${post.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transmitted via: ${post.locationSimulated}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Message Text
            Text(
                text = post.text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer actions (Local mesh upvotes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Time tag
                Text(
                    text = "Hop verified: 1s ago",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )

                // Backing upvote button
                Surface(
                    color = if (post.isLiked) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onLikeClicked() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Mesh packet backing",
                            tint = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.likesCount} upvotes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. PRIVATE RADIO CHAT DETAIL SCREEN
// -------------------------------------------------------------
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    partnerName: String
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val partnerStream by viewModel.peers.collectAsStateWithLifecycle()
    val myUsername by viewModel.myUsername.collectAsStateWithLifecycle()
    
    val partnerDetails = partnerStream.find { it.username.lowercase() == partnerName.lowercase() }
    
    var typedText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to latest message on load
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat screen custom header bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back back to peers menu"
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    AsyncImage(
                        model = partnerDetails?.avatarUrl ?: viewModel.getAvatarForUser(partnerName),
                        contentDescription = partnerName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (partnerDetails?.isOnline == true) Color(0xFF4CAF50) else Color(0xFF757575))
                            .align(Alignment.BottomEnd)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "@$partnerName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${partnerDetails?.connectionType ?: "Static Mesh"} • ${partnerDetails?.distance ?: "Local Station"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isMe = message.sender.lowercase() == "me" || message.sender.lowercase() == myUsername.lowercase()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            )
                            .background(
                                if (isMe) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Column {
                            Text(
                                text = message.text,
                                fontSize = 13.sp,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            // Hop/Protocol transmission verification
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Mesh • ${message.hopCount} hop",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick replies chips row to let them test response values instantly!
        val suggestions = listOf("How does this work?", "No recharge?", "Meet up?")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { typedText = suggestion }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Active message typing entry drawer bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Write offline local message packet...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("story_reply_input"),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (typedText.isNotBlank()) {
                                viewModel.sendMessage(partnerName, typedText)
                                typedText = ""
                            }
                        }
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (typedText.isNotBlank()) {
                            viewModel.sendMessage(partnerName, typedText)
                            typedText = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send offline message packets",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. PORT DIAGNOSTICS & SYSTEM LOGS SCREEN
// -------------------------------------------------------------
@Composable
fun NetworkDetailsScreen(
    viewModel: MainViewModel
) {
    val logs by viewModel.networkLog.collectAsStateWithLifecycle()
    val isPortBound by viewModel.isPortBound.collectAsStateWithLifecycle()
    val localIp by viewModel.localIpAddress.collectAsStateWithLifecycle()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mesh Hardware & Core Diagnostics",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Live telemetry readings of local UDP broadcasts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Interactive diagnostics stats card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE INTERFACES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticStatRow(
                        label = "Core Protocol Listener Port",
                        value = "8888 (Multicast/UDP Broadcast)",
                        isGood = isPortBound
                    )
                    DiagnosticStatRow(
                        label = "P2P Socket Interface State",
                        value = if (isPortBound) "Listening (Active Receiver)" else "Offline (Sandbox Bound)",
                        isGood = isPortBound
                    )
                    DiagnosticStatRow(
                        label = "Local Base Station Address",
                        value = localIp,
                        isGood = localIp != "0.0.0.0"
                    )
                    DiagnosticStatRow(
                        label = "Virtual Peer Simulator Eng",
                        value = if (isSimulatorMode) "ENABLED (Relays active)" else "DISABLED (Physical-only)",
                        isGood = isSimulatorMode
                    )
                }
            }
        }

        // Manual beacon broadcasting controllers box
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Offline Test Handshake Trigger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Force host packet broadcasting now over current router network or hotpoints to awaken any sleeping terminals.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.broadcastDiscoveryHeartbeat() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Discovery Pulse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.startUdpSocketEngine() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Sockets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Log terminal screen
        item {
            Text(
                text = "Hex-Mesh Relay Activity Output",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(logs) { log ->
            Text(
                text = log,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = if (log.contains("Failed") || log.contains("Error")) Color(0xFFFF5252)
                        else if (log.contains("Received")) Color(0xFF4CAF50)
                        else if (log.contains("Discovery") || log.contains("Discovered")) Color(0xFF64B5F6)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.35f), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            )
        }
    }
}

@Composable
fun DiagnosticStatRow(
    label: String,
    value: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceOrSecondary()
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isGood) Color(0xFF4CAF50) else Color(0xFFFF9800))
        )
    }
}

@Composable
fun ColorScheme.onSurfaceOrSecondary(): Color {
    return onSurface
}

// -------------------------------------------------------------
// 5. MY PROFILE SETTINGS SCREEN
// -------------------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: MainViewModel
) {
    val myName by viewModel.myUsername.collectAsStateWithLifecycle()
    val myStatus by viewModel.myStatus.collectAsStateWithLifecycle()

    var editingName by remember { mutableStateOf(myName) }
    var editingStatus by remember { mutableStateOf(myStatus) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "My Mesh Call Sign",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        AsyncImage(
            model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250",
            contentDescription = "My avatar",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = editingName,
            onValueChange = { editingName = it },
            label = { Text("Mesh Call Sign (No Spaces)") },
            placeholder = { Text("e.g. shivam") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = editingStatus,
            onValueChange = { editingStatus = it },
            label = { Text("Broadcast Status Tag Line") },
            placeholder = { Text("Let peers know what you need or offer...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Wifi, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                viewModel.updateProfile(editingName, editingStatus)
                showSavedMessage = true
            })
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.updateProfile(editingName, editingStatus)
                showSavedMessage = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("publish_post_btn"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Update Broadcasting profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        AnimatedVisibility(visible = showSavedMessage) {
            Text(
                text = "Mesh Profile broadcast updated!",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            LaunchedEffect(showSavedMessage) {
                delay(3000)
                showSavedMessage = false
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Explain mesh network topology
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🛡️ Secure Off-Grid Routing Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unlike internet chats, OffGrid works via ad-hoc Local Subnet packets. Packets are stored on your local flash memory database and relayed automatically. No metadata is shared with servers, making your chats highly censorship-proof and resilient against central infrastructure blackouts.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f)
                )
            }
        }
    }
}
