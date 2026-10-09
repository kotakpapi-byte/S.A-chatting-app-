package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirestoreManager
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ChatEntity
import com.example.data.model.ChatMemberEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AppScreen {
    CHATS,
    CONVERSATION,
    PROFILE_SETTINGS,
    THEME_SETTINGS,
    CHAT_WALLPAPER_SETTINGS,
    BLOCKED_USERS,
    OFFICE_DIRECTORY
}

enum class AuthScreen {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val firestoreManager = FirestoreManager(application)
    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val contactDao = db.contactDao()
    private val settingsDao = db.settingsDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.CHATS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenBackStack = mutableListOf<AppScreen>()

    private val _authScreen = MutableStateFlow(AuthScreen.LOGIN)
    val authScreen: StateFlow<AuthScreen> = _authScreen.asStateFlow()

    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    private val _activeRecipient = MutableStateFlow<UserEntity?>(null)
    val activeRecipient: StateFlow<UserEntity?> = _activeRecipient.asStateFlow()

    private val _isRecipientBlocked = MutableStateFlow(false)
    val isRecipientBlocked: StateFlow<Boolean> = _isRecipientBlocked.asStateFlow()

    // Chats Flow
    val allChats: StateFlow<List<ChatEntity>> = chatDao.getAllChatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Messages Flow
    val activeMessages: StateFlow<List<MessageEntity>> = _activeChat
        .flatMapLatest { chat ->
            if (chat != null) messageDao.getMessagesForChatFlow(chat.chatId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Settings
    private val _appSettings = MutableStateFlow(AppSettingsEntity(username = "guest"))
    val appSettings: StateFlow<AppSettingsEntity> = _appSettings.asStateFlow()

    // Office Colleagues
    val officeColleagues: StateFlow<List<UserEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) userDao.getAllOtherUsersFlow(user.username)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Blocked Users
    val blockedUsers: StateFlow<List<UserEntity>> = combine(
        _currentUser,
        officeColleagues
    ) { user, colleagues ->
        if (user == null) emptyList()
        else {
            val blockedList = mutableListOf<UserEntity>()
            for (c in colleagues) {
                val blocked = contactDao.isBlocked(user.username, c.username)
                if (blocked == true) {
                    blockedList.add(c)
                }
            }
            blockedList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Temporary verification code for Password Reset
    private val _generatedResetCode = MutableStateFlow<String?>(null)
    val generatedResetCode: StateFlow<String?> = _generatedResetCode.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if demo user can be auto-loaded or wait for login
            // Seed DB first if needed
            val existing = userDao.getUserByUsername("ceo_sudais")
            if (existing == null) {
                AppDatabase.seedInitialData(db)
            }
        }
    }

    // Navigation
    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (_screenBackStack.isNotEmpty()) {
            _currentScreen.value = _screenBackStack.removeAt(_screenBackStack.size - 1)
            true
        } else false
    }

    fun setAuthScreen(screen: AuthScreen) {
        _authScreen.value = screen
    }

    // --- Authentication ---

    suspend fun login(identifier: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val trimmedId = identifier.trim()
            val trimmedPass = password.trim()

            if (trimmedId.isBlank() || trimmedPass.isBlank()) {
                return@withContext Result.failure(Exception("Please enter both email/username and password."))
            }

            // Find by username or email
            val user = userDao.getUserByUsername(trimmedId) ?: userDao.getUserByEmail(trimmedId)
            if (user == null) {
                return@withContext Result.failure(Exception("No account found for $trimmedId. Ensure email ends with @sudais.com or sign up."))
            }

            if (user.passwordHash != trimmedPass) {
                return@withContext Result.failure(Exception("Incorrect password. Try again or use 'Forgot Password'."))
            }

            // Successfully logged in
            _currentUser.value = user
            loadUserSettings(user.username)
            Result.success("Welcome back, ${user.displayName}!")
        }
    }

    suspend fun signup(
        displayName: String,
        username: String,
        emailInput: String,
        password: String,
        department: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val cleanName = displayName.trim()
            var cleanUser = username.trim().lowercase().removePrefix("@")
            var cleanEmail = emailInput.trim().lowercase()
            val cleanPass = password.trim()
            val cleanDept = department.trim().ifEmpty { "Office Staff" }

            if (cleanName.isBlank()) return@withContext Result.failure(Exception("Please enter your full name."))
            if (cleanUser.length < 3) return@withContext Result.failure(Exception("Username must be at least 3 characters."))
            if (cleanPass.length < 4) return@withContext Result.failure(Exception("Password must be at least 4 characters."))

            // Enforce @sudais.com domain
            if (!cleanEmail.contains("@")) {
                cleanEmail = "$cleanEmail@sudais.com"
            } else if (!cleanEmail.endsWith("@sudais.com")) {
                return@withContext Result.failure(Exception("Email must end with @sudais.com for company office access."))
            }

            // Check uniqueness
            if (userDao.getUserByUsername(cleanUser) != null) {
                return@withContext Result.failure(Exception("Username @$cleanUser is already taken."))
            }
            if (userDao.getUserByEmail(cleanEmail) != null) {
                return@withContext Result.failure(Exception("An account already exists with email $cleanEmail."))
            }

            val newUser = UserEntity(
                username = cleanUser,
                email = cleanEmail,
                passwordHash = cleanPass,
                displayName = cleanName,
                statusText = "Available",
                isOnline = true,
                department = cleanDept
            )
            userDao.insertUser(newUser)

            // Initialize default settings
            val initialSettings = AppSettingsEntity(username = cleanUser)
            settingsDao.saveSettings(initialSettings)

            _currentUser.value = newUser
            _appSettings.value = initialSettings
            Result.success("Account created successfully!")
        }
    }

    suspend fun requestResetCode(emailInput: String): Result<String> {
        return withContext(Dispatchers.IO) {
            var email = emailInput.trim().lowercase()
            if (!email.contains("@")) email = "$email@sudais.com"
            if (!email.endsWith("@sudais.com")) {
                return@withContext Result.failure(Exception("Please enter your @sudais.com company email address."))
            }

            val user = userDao.getUserByEmail(email)
                ?: return@withContext Result.failure(Exception("No account registered with $email."))

            val code = (1000..9999).random().toString()
            _generatedResetCode.value = code
            Result.success(code)
        }
    }

    suspend fun confirmPasswordReset(
        emailInput: String,
        enteredCode: String,
        newPassword: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            var email = emailInput.trim().lowercase()
            if (!email.contains("@")) email = "$email@sudais.com"

            if (_generatedResetCode.value == null || enteredCode.trim() != _generatedResetCode.value) {
                return@withContext Result.failure(Exception("Invalid verification code. Please check the code."))
            }
            if (newPassword.trim().length < 4) {
                return@withContext Result.failure(Exception("New password must be at least 4 characters."))
            }

            val rows = userDao.updatePasswordByEmail(email, newPassword.trim())
            if (rows > 0) {
                _generatedResetCode.value = null
                Result.success("Password reset successfully! Please log in.")
            } else {
                Result.failure(Exception("Failed to reset password. Please try again."))
            }
        }
    }

    fun signInWithGoogleAccount(activity: Activity, onComplete: (Boolean, String?) -> Unit) {
        firestoreManager.performGoogleSignIn(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { uid, email, name ->
                viewModelScope.launch(Dispatchers.IO) {
                    val cleanUser = email.substringBefore("@").lowercase().replace(".", "_").filter { it.isLetterOrDigit() || it == '_' }
                    val corporateEmail = if (email.endsWith("@sudais.com")) email else "$cleanUser@sudais.com"
                    var existingUser = userDao.getUserByUsername(cleanUser) ?: userDao.getUserByEmail(corporateEmail)
                    if (existingUser == null) {
                        val newUser = UserEntity(
                            username = cleanUser,
                            email = corporateEmail,
                            passwordHash = "google_auth",
                            displayName = name,
                            statusText = "Available",
                            isOnline = true,
                            department = "Corporate Staff"
                        )
                        userDao.insertUser(newUser)
                        existingUser = newUser
                        val initialSettings = AppSettingsEntity(username = cleanUser)
                        settingsDao.saveSettings(initialSettings)
                        _appSettings.value = initialSettings
                    } else {
                        loadUserSettings(existingUser.username)
                    }

                    // Sync to Firestore
                    firestoreManager.syncUserProfile(
                        userId = uid,
                        username = existingUser.username,
                        email = existingUser.email,
                        displayName = existingUser.displayName,
                        avatarUrl = existingUser.avatarUri,
                        department = existingUser.department,
                        statusText = existingUser.statusText,
                        isOnline = true
                    )

                    _currentUser.value = existingUser
                    withContext(Dispatchers.Main) {
                        onComplete(true, null)
                    }
                }
            },
            onError = { errorMsg ->
                onComplete(false, errorMsg)
            },
            onCancelled = {
                onComplete(false, null)
            }
        )
    }

    fun logout() {
        firestoreManager.signOut(viewModelScope) {}
        _currentUser.value = null
        _activeChat.value = null
        _currentScreen.value = AppScreen.CHATS
        _screenBackStack.clear()
        _authScreen.value = AuthScreen.LOGIN
    }

    // --- Settings & Profile ---

    private fun loadUserSettings(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = settingsDao.getSettingsForUser(username)
                ?: AppSettingsEntity(username = username)
            _appSettings.value = settings
        }
    }

    fun updateProfile(
        displayName: String,
        statusText: String,
        isOnline: Boolean,
        department: String
    ) {
        val user = _currentUser.value ?: return
        val updated = user.copy(
            displayName = displayName.trim().ifEmpty { user.displayName },
            statusText = statusText.trim().ifEmpty { user.statusText },
            isOnline = isOnline,
            department = department.trim().ifEmpty { user.department }
        )
        _currentUser.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateUser(updated)
        }
    }

    fun updateAvatar(avatarPath: String) {
        val user = _currentUser.value ?: return
        val updated = user.copy(avatarUri = avatarPath)
        _currentUser.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            userDao.updateAvatarUri(user.username, avatarPath)
        }
    }

    fun updateThemeColor(hex: String) {
        val current = _appSettings.value
        val updated = current.copy(themeColorHex = hex)
        _appSettings.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.saveSettings(updated)
        }
    }

    fun updateAppWallpaper(path: String?) {
        val current = _appSettings.value
        val updated = current.copy(customAppWallpaperUri = path)
        _appSettings.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.saveSettings(updated)
        }
    }

    fun updateChatWallpaper(path: String?) {
        val current = _appSettings.value
        val updated = current.copy(customChatWallpaperUri = path)
        _appSettings.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.saveSettings(updated)
        }
    }

    fun toggleDarkMode() {
        val current = _appSettings.value
        val updated = current.copy(isDarkMode = !current.isDarkMode)
        _appSettings.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.saveSettings(updated)
        }
    }

    // --- Chat Operations ---

    fun openChat(chat: ChatEntity) {
        _activeChat.value = chat
        viewModelScope.launch(Dispatchers.IO) {
            if (!chat.isGroup && chat.directRecipientUsername != null) {
                val recipient = userDao.getUserByUsername(chat.directRecipientUsername)
                _activeRecipient.value = recipient
                val currUser = _currentUser.value
                if (currUser != null) {
                    val isBlocked = contactDao.isBlocked(currUser.username, chat.directRecipientUsername) ?: false
                    _isRecipientBlocked.value = isBlocked
                }
            } else {
                _activeRecipient.value = null
                _isRecipientBlocked.value = false
            }
        }
        navigateTo(AppScreen.CONVERSATION)
    }

    fun startDirectChatWithUsername(rawUsername: String, onComplete: (Boolean, String) -> Unit) {
        val myUser = _currentUser.value ?: return
        val targetUsername = rawUsername.trim().lowercase().removePrefix("@")

        if (targetUsername == myUser.username.lowercase()) {
            onComplete(false, "You cannot chat with yourself.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val targetUser = userDao.getUserByUsername(targetUsername)
            if (targetUser == null) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "User @$targetUsername not found in office directory.")
                }
                return@launch
            }

            // Check if chat already exists
            val existing = chatDao.getDirectChat(targetUsername)
            val chatToOpen = if (existing != null) {
                existing
            } else {
                val newChat = ChatEntity(
                    chatId = "direct_${myUser.username}_$targetUsername",
                    isGroup = false,
                    title = targetUser.displayName,
                    groupAvatarUri = targetUser.avatarUri,
                    directRecipientUsername = targetUser.username,
                    lastMessageText = "Chat started with @${targetUser.username}",
                    lastMessageTime = System.currentTimeMillis(),
                    lastSenderUsername = myUser.username
                )
                chatDao.insertChat(newChat)
                chatDao.insertMembers(
                    listOf(
                        ChatMemberEntity(newChat.chatId, myUser.username),
                        ChatMemberEntity(newChat.chatId, targetUser.username)
                    )
                )
                newChat
            }

            withContext(Dispatchers.Main) {
                openChat(chatToOpen)
                onComplete(true, "Chat opened with @${targetUser.username}")
            }
        }
    }

    fun createGroupChat(title: String, memberUsernames: List<String>, onComplete: (Boolean) -> Unit) {
        val myUser = _currentUser.value ?: return
        val cleanTitle = title.trim().ifEmpty { "Office Group" }

        viewModelScope.launch(Dispatchers.IO) {
            val groupId = "group_${UUID.randomUUID().toString().take(8)}"
            val groupChat = ChatEntity(
                chatId = groupId,
                isGroup = true,
                title = cleanTitle,
                lastMessageText = "${myUser.displayName} created group '$cleanTitle'",
                lastMessageTime = System.currentTimeMillis(),
                lastSenderUsername = myUser.username
            )
            chatDao.insertChat(groupChat)

            val members = (memberUsernames + myUser.username).distinct().map {
                ChatMemberEntity(groupId, it)
            }
            chatDao.insertMembers(members)

            val initialMsg = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = groupId,
                senderUsername = myUser.username,
                text = "Welcome to $cleanTitle! S.A Office group chat created.",
                timestamp = System.currentTimeMillis()
            )
            messageDao.insertMessage(initialMsg)

            withContext(Dispatchers.Main) {
                openChat(groupChat)
                onComplete(true)
            }
        }
    }

    fun sendMessage(text: String, imageUri: String? = null) {
        val myUser = _currentUser.value ?: return
        val chat = _activeChat.value ?: return

        if (_isRecipientBlocked.value) return
        if (text.isBlank() && imageUri == null) return

        val messageText = text.trim()
        val now = System.currentTimeMillis()
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chat.chatId,
            senderUsername = myUser.username,
            text = messageText,
            imageUri = imageUri,
            timestamp = now
        )

        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(message)
            firestoreManager.sendCloudMessage(
                chatId = chat.chatId,
                messageId = message.messageId,
                senderId = firestoreManager.auth.currentUser?.uid ?: myUser.username,
                senderUsername = myUser.username,
                text = messageText,
                imageUri = imageUri
            )
            val previewText = if (imageUri != null && messageText.isBlank()) "📷 Photo" else messageText
            chatDao.updateLastMessage(chat.chatId, previewText, now, myUser.username)

            // Realistic office colleague auto-reply for direct chats
            if (!chat.isGroup && chat.directRecipientUsername != null) {
                val colleague = chat.directRecipientUsername
                // Colleague bots
                val officeReplies = mapOf(
                    "ceo_sudais" to listOf(
                        "Noted! Excellent work. Keep me updated on the project milestones.",
                        "Approved from executive office. Let's proceed as planned.",
                        "Thanks for touching base. Please share the summary with the team.",
                        "Understood. Great initiative!"
                    ),
                    "fatima_hr" to listOf(
                        "Thank you for informing HR! Let me log this in the office records.",
                        "All set! Let me know if you need any office resources or documentation.",
                        "Got it! Have a productive day at the office.",
                        "Noted. I'll include this in our weekly team circular."
                    ),
                    "omar_tech" to listOf(
                        "Got it! Systems look completely healthy on my end.",
                        "Checking the codebase and logs now. Will update you shortly.",
                        "Deployed and verified! Everything is running smoothly.",
                        "Great, thanks for the update! All endpoints are responsive."
                    ),
                    "yasmin_sales" to listOf(
                        "Thanks! Client feedback has been very positive so far.",
                        "Following up with the partners right now. Will share the brief soon.",
                        "Perfect timing! Let's align on this in our next call.",
                        "Noted! Appreciate the quick update."
                    ),
                    "zayd_ops" to listOf(
                        "Operations team is on it! Logistics confirmed.",
                        "All office facilities and equipment are ready for this week.",
                        "Thanks! Everything scheduled as requested.",
                        "Copy that. Moving forward with the plan."
                    )
                )

                val replies = officeReplies[colleague]
                if (replies != null) {
                    delay(1200)
                    val replyText = replies.random()
                    val replyMsg = MessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        chatId = chat.chatId,
                        senderUsername = colleague,
                        text = replyText,
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insertMessage(replyMsg)
                    chatDao.updateLastMessage(chat.chatId, replyText, System.currentTimeMillis(), colleague)
                }
            }
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.updateReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.deleteMessage(messageId)
        }
    }

    fun clearActiveChat() {
        val chat = _activeChat.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.clearMessagesForChat(chat.chatId)
            chatDao.updateLastMessage(chat.chatId, "Chat history cleared", System.currentTimeMillis(), "")
        }
    }

    fun toggleBlockActiveRecipient() {
        val myUser = _currentUser.value ?: return
        val recipient = _activeRecipient.value ?: return
        val newBlocked = !_isRecipientBlocked.value

        viewModelScope.launch(Dispatchers.IO) {
            contactDao.insertOrUpdateContact(
                ContactEntity(
                    ownerUsername = myUser.username,
                    contactUsername = recipient.username,
                    isBlocked = newBlocked
                )
            )
            _isRecipientBlocked.value = newBlocked
        }
    }

    fun unblockUser(username: String) {
        val myUser = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            contactDao.setBlockedStatus(myUser.username, username, false)
            if (_activeRecipient.value?.username == username) {
                _isRecipientBlocked.value = false
            }
        }
    }
}
