package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val userAvatar: String,
    val text: String,
    val locationSimulated: String = "Mesh Network",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val username: String,
    val avatarUrl: String,
    val ipAddress: String, // IP if real device, or "Simulation" 
    val connectionType: String, // "Wi-Fi LAN", "Bluetooth Mesh", "Virtual Node"
    val distance: String, // e.g. "8m away", "24m away"
    val statusText: String, // Status / status message
    val isOnline: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val receiver: String,
    val text: String,
    val isBroadcast: Boolean = false,
    val isSent: Boolean = true,
    val isReceived: Boolean = true,
    val hopCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class Contact(
    val username: String,
    val avatarUrl: String,
    val connectionType: String,
    val distance: String,
    val isOnline: Boolean,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val hasUnread: Boolean = false
)
