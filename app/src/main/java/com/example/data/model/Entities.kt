package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val avatarUri: String? = null,
    val statusText: String = "Available",
    val isOnline: Boolean = true,
    val department: String = "Office Staff",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val isGroup: Boolean = false,
    val title: String,
    val groupAvatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastSenderUsername: String = "",
    val directRecipientUsername: String? = null
)

@Entity(tableName = "chat_members", primaryKeys = ["chatId", "username"])
data class ChatMemberEntity(
    val chatId: String,
    val username: String,
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderUsername: String,
    val text: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true,
    val reaction: String? = null
)

@Entity(tableName = "contacts", primaryKeys = ["ownerUsername", "contactUsername"])
data class ContactEntity(
    val ownerUsername: String,
    val contactUsername: String,
    val isBlocked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val username: String,
    val themeColorHex: String = "#00A86B", // Default S.A Emerald
    val customAppWallpaperUri: String? = null,
    val customChatWallpaperUri: String? = null,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val readReceiptsEnabled: Boolean = true
)
