package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ChatEntity
import com.example.data.model.ChatMemberEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        ChatMemberEntity::class,
        MessageEntity::class,
        ContactEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sa_chatting_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(db: AppDatabase) {
            val userDao = db.userDao()
            val chatDao = db.chatDao()
            val messageDao = db.messageDao()

            // Seed Office Colleagues
            val defaultUsers = listOf(
                UserEntity(
                    username = "ceo_sudais",
                    email = "ceo@sudais.com",
                    passwordHash = "123456",
                    displayName = "Sudais Ahmed",
                    avatarUri = null,
                    statusText = "Available",
                    isOnline = true,
                    department = "Executive Office"
                ),
                UserEntity(
                    username = "fatima_hr",
                    email = "fatima@sudais.com",
                    passwordHash = "123456",
                    displayName = "Fatima Al-Zahra",
                    avatarUri = null,
                    statusText = "In Office",
                    isOnline = true,
                    department = "People & HR"
                ),
                UserEntity(
                    username = "omar_tech",
                    email = "omar@sudais.com",
                    passwordHash = "123456",
                    displayName = "Omar Farooq",
                    avatarUri = null,
                    statusText = "Available",
                    isOnline = true,
                    department = "Tech Lead"
                ),
                UserEntity(
                    username = "yasmin_sales",
                    email = "yasmin@sudais.com",
                    passwordHash = "123456",
                    displayName = "Yasmin Khan",
                    avatarUri = null,
                    statusText = "In a Meeting",
                    isOnline = false,
                    department = "Client Relations"
                ),
                UserEntity(
                    username = "zayd_ops",
                    email = "zayd@sudais.com",
                    passwordHash = "123456",
                    displayName = "Zayd Mansoor",
                    avatarUri = null,
                    statusText = "Available",
                    isOnline = true,
                    department = "HQ Operations"
                )
            )

            for (user in defaultUsers) {
                userDao.insertUser(user)
            }

            // Seed Office Group Chats
            val generalChatId = "chat_office_general"
            val announcementsChatId = "chat_office_announcements"
            val techChatId = "chat_office_tech"

            val initialChats = listOf(
                ChatEntity(
                    chatId = generalChatId,
                    isGroup = true,
                    title = "🏢 Sudais HQ Office",
                    lastMessageText = "Welcome to the new S.A Chatting office workspace!",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 30,
                    lastSenderUsername = "ceo_sudais"
                ),
                ChatEntity(
                    chatId = announcementsChatId,
                    isGroup = true,
                    title = "📢 Office Announcements",
                    lastMessageText = "Quarterly review meeting scheduled for Thursday 10:00 AM.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                    lastSenderUsername = "fatima_hr"
                ),
                ChatEntity(
                    chatId = techChatId,
                    isGroup = true,
                    title = "💻 Systems & Engineering",
                    lastMessageText = "Server migration complete. All office systems operational.",
                    lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 4,
                    lastSenderUsername = "omar_tech"
                )
            )

            for (chat in initialChats) {
                chatDao.insertChat(chat)
            }

            // Seed messages in General Chat
            val messages = listOf(
                MessageEntity(
                    messageId = "msg_1",
                    chatId = generalChatId,
                    senderUsername = "ceo_sudais",
                    text = "As-salamu alaykum team! Welcome to our official S.A Chatting enterprise platform.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
                    reaction = "👏"
                ),
                MessageEntity(
                    messageId = "msg_2",
                    chatId = generalChatId,
                    senderUsername = "fatima_hr",
                    text = "Welcome everyone! Feel free to update your profile photo, status and department settings.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 32,
                    reaction = "👍"
                ),
                MessageEntity(
                    messageId = "msg_3",
                    chatId = generalChatId,
                    senderUsername = "omar_tech",
                    text = "All staff accounts @sudais.com are active. You can find any colleague using their @username.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 30,
                    reaction = "🔥"
                ),
                MessageEntity(
                    messageId = "msg_4",
                    chatId = announcementsChatId,
                    senderUsername = "fatima_hr",
                    text = "Quarterly review meeting scheduled for Thursday 10:00 AM. Please prepare your department updates.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                    reaction = "🏢"
                ),
                MessageEntity(
                    messageId = "msg_5",
                    chatId = techChatId,
                    senderUsername = "omar_tech",
                    text = "Server migration complete. All office systems operational.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 4,
                    reaction = "🚀"
                )
            )

            for (msg in messages) {
                messageDao.insertMessage(msg)
            }
        }
    }
}
