package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class Repository(
    private val db: AppDatabase
) {
    val postsFlow: Flow<List<PostEntity>> = db.postDao().getAllPosts()
    val peersFlow: Flow<List<PeerEntity>> = db.peerDao().getAllPeers()

    suspend fun insertPost(post: PostEntity) = db.postDao().insertPost(post)
    suspend fun updatePost(post: PostEntity) = db.postDao().updatePost(post)

    suspend fun insertPeer(peer: PeerEntity) = db.peerDao().insertPeer(peer)
    suspend fun getPeerByUsername(username: String): PeerEntity? = db.peerDao().getPeerByUsername(username)
    suspend fun updatePeer(peer: PeerEntity) = db.peerDao().updatePeer(peer)

    fun getMessagesBetween(user1: String, user2: String): Flow<List<MessageEntity>> =
        db.messageDao().getMessagesBetween(user1, user2)

    fun getContactsFlow(currentUser: String): Flow<List<Contact>> {
        val messagesFlow = db.messageDao().getAllUserMessages(currentUser)
        val peersFlow = db.peerDao().getAllPeers()

        return combine(messagesFlow, peersFlow) { messages, peers ->
            // Extract all people we have chatted with
            val messagedPartners = messages.map { if (it.sender == currentUser) it.receiver else it.sender }
                .distinct()
                .filter { it != currentUser }

            // Combine with peer list
            val peerUsernames = peers.map { it.username }
            val allUniquePartners = (messagedPartners + peerUsernames).distinct()

            allUniquePartners.map { partner ->
                val peerInfo = peers.find { it.username.lowercase() == partner.lowercase() }
                
                val partnerMessages = messages.filter { 
                    (it.sender == partner && it.receiver == currentUser) || 
                    (it.sender == currentUser && it.receiver == partner) 
                }
                
                val latest = partnerMessages.maxByOrNull { it.timestamp }
                val lastText = latest?.text ?: "No messages in offline buffer"
                val lastTime = latest?.timestamp ?: (System.currentTimeMillis() - 3600000 * 2)

                Contact(
                    username = partner,
                    avatarUrl = peerInfo?.avatarUrl ?: getAvatarForUser(partner),
                    connectionType = peerInfo?.connectionType ?: "Virtual Node",
                    distance = peerInfo?.distance ?: "Local Mesh",
                    isOnline = peerInfo?.isOnline ?: true,
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
        val peers = db.peerDao().getAllPeers().first()
        if (peers.isEmpty()) {
            seedDatabase()
        }
    }

    private suspend fun seedDatabase() {
        // Clear all to perform fresh seed
        db.peerDao().deleteAll()
        db.postDao().deleteAll()
        db.messageDao().deleteAll()

        // 1. Seed Peers (Offline Neighbors / Nodes)
        val seedPeers = listOf(
            PeerEntity(
                username = "Alice",
                avatarUrl = getAvatarForUser("Alice"),
                ipAddress = "192.168.1.110",
                connectionType = "Wi-Fi LAN",
                distance = "12m away",
                statusText = "Local radio ham & mesh builder. Out of internet but never out of touch! 📻",
                isOnline = true
            ),
            PeerEntity(
                username = "Bob",
                avatarUrl = getAvatarForUser("Bob"),
                ipAddress = "192.168.1.134",
                connectionType = "Wi-Fi LAN",
                distance = "34m away",
                statusText = "P2P protocol researcher. Broadcasting on local channel.",
                isOnline = true
            ),
            PeerEntity(
                username = "Charlie",
                avatarUrl = getAvatarForUser("Charlie"),
                ipAddress = "Bluetooth-Mesh-S1",
                connectionType = "Bluetooth Node",
                distance = "8m away",
                statusText = "Keep talking. Let's chat over local radio relays. ⚡",
                isOnline = true
            ),
            PeerEntity(
                username = "Diana",
                avatarUrl = getAvatarForUser("Diana"),
                ipAddress = "OffGrid-Static-Gateway",
                connectionType = "Static Broadcast Station",
                distance = "110m away",
                statusText = "Neighborhood relay station. Post queries to the nearby bulletin board!",
                isOnline = false
            )
        )
        db.peerDao().insertAll(seedPeers)

        // 2. Seed Public Bulletin Board Posts
        val seedPosts = listOf(
            PostEntity(
                username = "Alice",
                userAvatar = getAvatarForUser("Alice"),
                text = "Hey neighborhood! The cellular tower down our street is undergoing maintenance. Chatting with nearby peers on local OffGrid channel. Spread the word! 📡💡",
                locationSimulated = "Within 15m range",
                likesCount = 8,
                isLiked = true,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30 // 30 mins ago
            ),
            PostEntity(
                username = "Bob",
                userAvatar = getAvatarForUser("Bob"),
                text = "Running a 2.4GHz custom omnidirectional antenna. Testing coverage across 2 streets. Let me know if you can hop through my node!",
                locationSimulated = "Within 40m range",
                likesCount = 4,
                isLiked = false,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2 // 2h ago
            ),
            PostEntity(
                username = "Diana",
                userAvatar = getAvatarForUser("Diana"),
                text = "COMMUNITY BULLETIN: Offline meetup scheduled at the local central square this Friday at 5 PM! We will showcase offline map-sharing over Wi-Fi hotspots! 🗺️📲",
                locationSimulated = "Local Gateway Relay",
                likesCount = 18,
                isLiked = true,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5 // 5h ago
            )
        )
        db.postDao().insertAll(seedPosts)

        // 3. Seed Private/Direct Offline Conversations
        val seedMessages = listOf(
            MessageEntity(
                sender = "Alice",
                receiver = "me",
                text = "Hey! Did your phone find my node automatically?",
                timestamp = System.currentTimeMillis() - 1000 * 3600 * 3
            ),
            MessageEntity(
                sender = "me",
                receiver = "Alice",
                text = "Yes, it discovered you on the local network router! Zero data, zero network charge required! Pretty amazing.",
                timestamp = System.currentTimeMillis() - 1000 * 3600 * 2
            ),
            MessageEntity(
                sender = "Alice",
                receiver = "me",
                text = "That is the power of offline P2P networks. Try posting something to the Public Bulletin tab too!",
                timestamp = System.currentTimeMillis() - 1000 * 3600 * 1
            ),
            MessageEntity(
                sender = "Bob",
                receiver = "me",
                text = "Hello peer! Ping me if you receive this UDP packet broadcast.",
                timestamp = System.currentTimeMillis() - 1000 * 1800
            )
        )
        db.messageDao().insertAll(seedMessages)
    }
}
