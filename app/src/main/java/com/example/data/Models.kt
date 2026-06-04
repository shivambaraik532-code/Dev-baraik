package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val userAvatar: String,
    val imageUrl: String,
    val caption: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val filterApplied: String = "Normal",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val userAvatar: String,
    val imageUrl: String,
    val hasUnseen: Boolean = true,
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val receiver: String,
    val text: String,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class Contact(
    val username: String,
    val avatarUrl: String,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val hasUnread: Boolean = false
)
