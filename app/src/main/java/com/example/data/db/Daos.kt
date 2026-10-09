package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ChatEntity
import com.example.data.model.ChatMemberEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) != LOWER(:currentUsername) ORDER BY displayName ASC")
    fun getAllOtherUsersFlow(currentUsername: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE LOWER(username) != LOWER(:currentUsername) ORDER BY displayName ASC")
    suspend fun getAllOtherUsers(currentUsername: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isOnline = :isOnline WHERE username = :username")
    suspend fun updateOnlineStatus(username: String, isOnline: Boolean)

    @Query("UPDATE users SET statusText = :statusText WHERE username = :username")
    suspend fun updateStatusText(username: String, statusText: String)

    @Query("UPDATE users SET avatarUri = :avatarUri WHERE username = :username")
    suspend fun updateAvatarUri(username: String, avatarUri: String?)

    @Query("UPDATE users SET passwordHash = :newPassword WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePasswordByEmail(email: String, newPassword: String): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE isGroup = 0 AND LOWER(directRecipientUsername) = LOWER(:recipientUsername) LIMIT 1")
    suspend fun getDirectChat(recipientUsername: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageTime = :time, lastSenderUsername = :sender WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, time: Long, sender: String)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)

    // Chat Members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ChatMemberEntity>)

    @Query("SELECT username FROM chat_members WHERE chatId = :chatId")
    suspend fun getMembersForChat(chatId: String): List<String>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET reaction = :reaction WHERE messageId = :messageId")
    suspend fun updateReaction(messageId: String, reaction: String?)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE ownerUsername = :ownerUsername")
    fun getContactsFlow(ownerUsername: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE ownerUsername = :ownerUsername AND isBlocked = 1")
    fun getBlockedContactsFlow(ownerUsername: String): Flow<List<ContactEntity>>

    @Query("SELECT isBlocked FROM contacts WHERE ownerUsername = :ownerUsername AND LOWER(contactUsername) = LOWER(:contactUsername) LIMIT 1")
    suspend fun isBlocked(ownerUsername: String, contactUsername: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE ownerUsername = :ownerUsername AND LOWER(contactUsername) = LOWER(:contactUsername)")
    suspend fun setBlockedStatus(ownerUsername: String, contactUsername: String, blocked: Boolean)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE username = :username LIMIT 1")
    suspend fun getSettingsForUser(username: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE username = :username LIMIT 1")
    fun getSettingsFlow(username: String): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}
