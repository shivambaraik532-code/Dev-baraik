package com.example.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.random.Random

sealed class Screen {
    object Peers : Screen()
    object BulletinBoard : Screen()
    data class Chat(val partner: String) : Screen()
    object NetworkDetails : Screen()
    object Profile : Screen()
}

class MainViewModel(private val repository: Repository) : ViewModel() {

    private val TAG = "OffGridViewModel"
    private val PORT = 8888

    // Screen navigation stack state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Peers)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val navigationStack = mutableListOf<Screen>(Screen.Peers)

    fun navigateTo(screen: Screen) {
        if (screen != _currentScreen.value) {
            navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (navigationStack.isNotEmpty()) {
            _currentScreen.value = navigationStack.removeAt(navigationStack.size - 1)
        } else {
            _currentScreen.value = Screen.Peers
        }
    }

    // Node & Profile management
    private val _myUsername = MutableStateFlow("shivam_mesh")
    val myUsername = _myUsername.asStateFlow()

    private val _myStatus = MutableStateFlow("Broadcasting without data 📡")
    val myStatus = _myStatus.asStateFlow()

    private val _localIpAddress = MutableStateFlow("0.0.0.0")
    val localIpAddress = _localIpAddress.asStateFlow()

    private val _isPortBound = MutableStateFlow(false)
    val isPortBound = _isPortBound.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(true)
    val isSimulatorMode = _isSimulatorMode.asStateFlow()

    private val _networkLog = MutableStateFlow<List<String>>(listOf("System started: OffGrid local protocol initialized."))
    val networkLog = _networkLog.asStateFlow()

    // Peer & database flows
    val peers: StateFlow<List<PeerEntity>> = repository.peersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bulletins: StateFlow<List<PostEntity>> = repository.postsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts = repository.getContactsFlow("me")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChatPartner = MutableStateFlow("")
    val activeChatPartner = _activeChatPartner.asStateFlow()

    fun selectChatPartner(partnerName: String) {
        _activeChatPartner.value = partnerName
        navigateTo(Screen.Chat(partnerName))
    }

    val chatMessages: StateFlow<List<MessageEntity>> = _activeChatPartner
        .flatMapLatest { partner ->
            repository.getMessagesBetween("me", partner)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UDP Peer-to-Peer Socket variables
    private var datagramSocket: DatagramSocket? = null
    private var listeningJob: Job? = null
    private var discoveryJob: Job? = null

    init {
        // Build seed database entries on initialization
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
        
        // Start UDP Socket Listener and Heartbeat
        startUdpSocketEngine()
    }

    fun updateProfile(username: String, statusText: String) {
        _myUsername.value = username.trim().replace(" ", "_")
        _myStatus.value = statusText
        logNetwork("Profile renamed to @${_myUsername.value}")
        broadcastDiscoveryHeartbeat()
    }

    fun toggleSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        logNetwork("Simulation mesh mode toggled: ${if (enabled) "ON" else "OFF"}")
    }

    private fun logNetwork(log: String) {
        val list = _networkLog.value.toMutableList()
        list.add(0, "[${System.currentTimeMillis() % 100000}] $log")
        _networkLog.value = list.take(60) // Keep last 60 network diagnostic logs
        Log.d(TAG, log)
    }

    // Starts socket engine safely inside try/catches
    fun startUdpSocketEngine() {
        listeningJob?.cancel()
        discoveryJob?.cancel()
        datagramSocket?.close()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try binding to socket port 8888 for receiving broadcast datagrams
                val socket = DatagramSocket(PORT).apply {
                    broadcast = true
                }
                datagramSocket = socket
                _isPortBound.value = true
                logNetwork("Successfully bound UDP port listener on $PORT. Listening for nearby OffGrid peers...")

                // Start receiver loop
                startListening(socket)
                
                // Start periodic peer discovery broadcast
                startPeriodicDiscovery()

            } catch (e: Exception) {
                _isPortBound.value = false
                logNetwork("P2P Socket Binding Failed: ${e.localizedMessage}. Falling back to virtual local mesh node simulation.")
                // If standard port binding is restricted by emulator network sandbox, rely purely on simulator heartbeats!
                startSimulatedNetworkHeartbeats()
            }
        }
    }

    // Refresh layout details manually
    fun refreshNetworkState(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipAddressInt = wifiManager.connectionInfo.ipAddress
                val ipString = Formatter.formatIpAddress(ipAddressInt)
                _localIpAddress.value = if (ipString == "0.0.0.0") "Local Hotspot (P2P Mesh Area)" else ipString
                logNetwork("Network checked: Local IP resolves to ${_localIpAddress.value}")
            } catch (e: Exception) {
                _localIpAddress.value = "P2P Wireless Mode"
            }
        }
    }

    // Datagram Listener
    private fun startListening(socket: DatagramSocket) {
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(2048)
            while (isActive && !socket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    
                    val text = String(packet.data, 0, packet.length).trim()
                    val senderIp = packet.address.hostAddress ?: ""
                    
                    if (senderIp == _localIpAddress.value) continue // Ignore self broadcasts

                    handleReceivedPacket(text, senderIp)
                } catch (e: SocketException) {
                    break // Socket closed
                } catch (e: Exception) {
                    logNetwork("Packet parse error: ${e.localizedMessage}")
                }
            }
        }
    }

    // OffGrid Packet Handler Protocol
    // Format options:
    // HELLO: OFFGRID|HELLO|username|status_text
    // MSG: OFFGRID|MSG|sender|receiver|message_text
    // BULLETIN: OFFGRID|BULLETIN|sender|bulletin_text
    private suspend fun handleReceivedPacket(rawText: String, ipAddress: String) {
        if (!rawText.startsWith("OFFGRID|")) return

        val parts = rawText.split("|")
        if (parts.size < 4) return

        val type = parts[1]
        when (type) {
            "HELLO" -> {
                val peerName = parts[2]
                val peerStatus = parts[3]
                logNetwork("Discovered active peer: @$peerName on wireless node IP: $ipAddress")
                
                // Save or update discovered hardware peers
                val exists = repository.getPeerByUsername(peerName)
                val newPeer = PeerEntity(
                    username = peerName,
                    avatarUrl = repository.getAvatarForUser(peerName),
                    ipAddress = ipAddress,
                    connectionType = "Wi-Fi P2P",
                    distance = "Wireless range",
                    statusText = peerStatus,
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                )
                repository.insertPeer(newPeer)

                // Handshake reply unicast (so they instantly register us back)
                if (parts.size > 4 && parts[4] == "REQUEST_REPLY") {
                    sendUdpPacketDirectly(ipAddress, "OFFGRID|HELLO|${_myUsername.value}|${_myStatus.value}|RESPONSE")
                }
            }
            "MSG" -> {
                val senderName = parts[2]
                val receiverName = parts[3]
                val content = parts[4]

                if (receiverName == _myUsername.value || receiverName == "me" || receiverName == "global") {
                    logNetwork("Received private datagram from @$senderName: \"$content\"")
                    val incoming = MessageEntity(
                        sender = senderName,
                        receiver = "me",
                        text = content,
                        isBroadcast = false,
                        isSent = true,
                        isReceived = true,
                        hopCount = 1,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertMessage(incoming)
                }
            }
            "BULLETIN" -> {
                val senderName = parts[2]
                val text = parts[3]
                logNetwork("Received public bulletin from @$senderName: \"$text\"")

                val newBulletin = PostEntity(
                    username = senderName,
                    userAvatar = repository.getAvatarForUser(senderName),
                    text = text,
                    locationSimulated = "Transmitted over LAN",
                    likesCount = 0,
                    isLiked = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPost(newBulletin)
            }
        }
    }

    // Sends packet over WAN/LAN using raw IP or subnet broadcasts
    private fun sendLocalUdpBroadcast(payload: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = datagramSocket ?: DatagramSocket().apply { broadcast = true }
                val data = payload.toByteArray()
                // Broadcast address
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddress, PORT)
                socket.send(packet)
                Log.d(TAG, "Sent local network broadcast: $payload")
            } catch (e: Exception) {
                Log.e(TAG, "Failed broadcasting UDP packet: ${e.localizedMessage}")
            }
        }
    }

    private fun sendUdpPacketDirectly(targetIp: String, payload: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = datagramSocket ?: DatagramSocket()
                val data = payload.toByteArray()
                val destAddress = InetAddress.getByName(targetIp)
                val packet = DatagramPacket(data, data.size, destAddress, PORT)
                socket.send(packet)
                Log.d(TAG, "Sent unicast packet to $targetIp: $payload")
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending packet to direct IP $targetIp: ${e.localizedMessage}")
            }
        }
    }

    // Heartbeats
    private fun startPeriodicDiscovery() {
        discoveryJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                broadcastDiscoveryHeartbeat()
                delay(12000) // Discovery broadcast wave every 12s
            }
        }
    }

    fun broadcastDiscoveryHeartbeat() {
        val payload = "OFFGRID|HELLO|${_myUsername.value}|${_myStatus.value}|REQUEST_REPLY"
        sendLocalUdpBroadcast(payload)
        logNetwork("Announced own mesh ID to wireless node subnet.")
    }

    // Simulated heartbeats if alone on emulator/IDE
    private fun startSimulatedNetworkHeartbeats() {
        viewModelScope.launch {
            while (isActive) {
                if (_isSimulatorMode.value) {
                    delay(Random.nextLong(20000, 35000))
                    val virtualPeers = listOf("Alice", "Bob", "Charlie", "Diana")
                    val randomPeer = virtualPeers.random()
                    logNetwork("Simulating discovery beacon from virtual node: @$randomPeer")
                    
                    val existingPeer = repository.getPeerByUsername(randomPeer)
                    if (existingPeer != null) {
                        repository.updatePeer(existingPeer.copy(
                            isOnline = true,
                            lastSeen = System.currentTimeMillis()
                        ))
                    }
                } else {
                    delay(5000)
                }
            }
        }
    }

    // ACTIONS
    fun sendMessage(partner: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Document sent message in local database
            val sentMsg = MessageEntity(
                sender = "me",
                receiver = partner,
                text = text,
                isBroadcast = false,
                isSent = true,
                isReceived = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(sentMsg)
            logNetwork("Saved outgoing message to local off-grid buffer for @$partner")

            // Determine transport method: is it a real WiFi LAN peer or a Virtual Simulator peer?
            val destinationPeer = repository.getPeerByUsername(partner)
            
            if (destinationPeer != null && destinationPeer.connectionType == "Wi-Fi P2P" && destinationPeer.ipAddress != "Simulation") {
                // REAL hard physical transport
                logNetwork("Broadcasting datagram of private message to peer IP ${destinationPeer.ipAddress}")
                val payload = "OFFGRID|MSG|${_myUsername.value}|$partner|$text"
                sendUdpPacketDirectly(destinationPeer.ipAddress, payload)
            } else {
                // SIMULATOR backup triggers responsive reply
                if (_isSimulatorMode.value) {
                    triggerSimulatedDelayedReply(partner, text)
                }
            }
        }
    }

    private fun triggerSimulatedDelayedReply(username: String, userText: String) {
        viewModelScope.launch {
            delay(1200) // Delay to mimic radio routing hops
            val replyText = getResponsiveSimulatedReply(username, userText)
            val replyMsg = MessageEntity(
                sender = username,
                receiver = "me",
                text = replyText,
                isBroadcast = false,
                isSent = true,
                isReceived = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(replyMsg)
            logNetwork("Received off-grid packet back from @$username: \"$replyText\"")
        }
    }

    private fun getResponsiveSimulatedReply(username: String, userText: String): String {
        val query = userText.lowercase().trim()
        return when {
            query.contains("hello") || query.contains("hey") || query.contains("hi") -> {
                when (username.lowercase()) {
                    "alice" -> "Hey! Welcome to the mesh network! Finding peers without a phone bill feels so fresh, right? 🌐⚡"
                    "bob" -> "What's up! Just configuring local radio modules here. Glad you're on the node."
                    "charlie" -> "Hello. OffGrid packet verified. How's wireless range on your terminal?"
                    "diana" -> "Hey peer. Need help or info on the local offline services?"
                    else -> "Hi! Direct offline connection stable."
                }
            }
            query.contains("recharge") || query.contains("unpaid") || query.contains("data") || query.contains("internet") || query.contains("cellular") -> {
                "That's the best part! OffGrid works entirely over local Wi-Fi hotspots and Bluetooth mesh direct nodes. Zero mobile recharge or credit card details needed! 100% free offline privacy."
            }
            query.contains("work") || query.contains("how does") || query.contains("mesh") -> {
                "It uses UDP broadcasts and direct sockets over local P2P. Our phones act as mini-towers, bouncing packages to neighbors. If Bob is out of range, Charlie relays the packet! It's peer-to-peer routing!"
            }
            query.contains("coffee") || query.contains("meet") -> {
                "I should be near the Central Gazebo. Let's trace nodes there! Coffee sounds stellar ☕"
            }
            else -> {
                val responses = listOf(
                    "Signal mesh is highly responsive right now! Let's build a long range hop chain.",
                    "Packet routing confirmed. Staying on standby to relay local bulletins.",
                    "Understood. OffGrid offline connection stays connected as long as our wireless covers are active!",
                    "Fantastic. Make sure to share OffGrid with some friends in our neighborhood to expand coverage!"
                )
                responses.random()
            }
        }
    }

    // Public Bulletin functions
    fun publishBulletin(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val post = PostEntity(
                username = _myUsername.value,
                userAvatar = repository.getAvatarForUser("me"),
                text = text,
                locationSimulated = "P2P Transmitter Local",
                likesCount = 0,
                isLiked = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertPost(post)
            logNetwork("Published offline bulletin locally")

            // Real Wi-Fi network packet dispatch
            val payload = "OFFGRID|BULLETIN|${_myUsername.value}|$text"
            sendLocalUdpBroadcast(payload)

            // Simulator reaction
            if (_isSimulatorMode.value) {
                delay(3000)
                triggerSimulatedBulletinReply(text)
            }
        }
    }

    private fun triggerSimulatedBulletinReply(userBulletinText: String) {
        viewModelScope.launch {
            val commenters = listOf("Alice", "Bob", "Charlie")
            val randomPeer = commenters.random()
            val commentText = when {
                userBulletinText.contains("recharge") || userBulletinText.contains("internet") -> {
                    "Totally agree! No recharge and no active internet data required is an absolute lifesaver. Off-grid mesh rules!"
                }
                userBulletinText.contains("help") || userBulletinText.contains("anyone") -> {
                    "Just saw this on my bulletin stream. Relay active, I'm here if you need packet diagnostic support!"
                }
                else -> {
                    listOf(
                        "Upvoted this post via my local antenna! Great to have more nodes online.",
                        "Direct broadcast received loud and clear on my street. Mesh working excellent!",
                        "Mesh hop sequence successful. Keep expanding OffGrid nodes!"
                    ).random()
                }
            }

            // Upvote the user post randomly to make the bulletin board feel dynamic
            val currentBulletins = repository.postsFlow.first()
            if (currentBulletins.isNotEmpty()) {
                val latestPost = currentBulletins.maxByOrNull { it.timestamp }
                if (latestPost != null && latestPost.username == _myUsername.value) {
                    repository.updatePost(latestPost.copy(
                        likesCount = latestPost.likesCount + 1
                    ))
                    logNetwork("@$randomPeer liked your offline bulletin")
                }
            }

            // Add simulated bulletin comment as a separate bulletin board post to keep the offline board lively
            val seedCommentPost = PostEntity(
                username = randomPeer,
                userAvatar = repository.getAvatarForUser(randomPeer),
                text = "@${_myUsername.value} $commentText",
                locationSimulated = "Simulated Router Node",
                likesCount = 1,
                isLiked = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertPost(seedCommentPost)
            logNetwork("@$randomPeer posted a comments on the offline bulletin stream.")
        }
    }

    fun toggleLikePost(post: PostEntity) {
        viewModelScope.launch {
            val updated = post.copy(
                isLiked = !post.isLiked,
                likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
            )
            repository.updatePost(updated)
            logNetwork("Upvoted post ID: ${post.id}")
        }
    }

    fun getAvatarForUser(user: String): String {
        return repository.getAvatarForUser(user)
    }

    override fun onCleared() {
        listeningJob?.cancel()
        discoveryJob?.cancel()
        datagramSocket?.close()
        super.onCleared()
    }
}

class MainViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
