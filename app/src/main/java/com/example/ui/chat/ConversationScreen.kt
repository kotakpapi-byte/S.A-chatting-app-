package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.MessageEntity
import com.example.data.util.ImageStorageManager
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.OnlineGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: MainViewModel
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeChat by viewModel.activeChat.collectAsState()
    val recipient by viewModel.activeRecipient.collectAsState()
    val isBlocked by viewModel.isRecipientBlocked.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var pendingImageUri by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher for sending images in chat
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageManager.saveUriToInternalStorage(context, uri, "chat_media")
            if (savedPath != null) {
                pendingImageUri = savedPath
            }
        }
    }

    val chatTitle = if (activeChat?.isGroup == true) activeChat?.title ?: "Group Chat"
    else recipient?.displayName ?: activeChat?.title ?: "Direct Chat"

    val chatSubtitle = if (activeChat?.isGroup == true) "Office Workspace Group"
    else if (recipient != null) "@${recipient?.username} • ${if (recipient?.isOnline == true) "Online" else "Offline"} • ${recipient?.statusText}"
    else "@${activeChat?.directRecipientUsername.orEmpty()}"

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUri = recipient?.avatarUri ?: activeChat?.groupAvatarUri,
                            displayName = chatTitle,
                            size = 40.dp,
                            isOnline = recipient?.isOnline,
                            isGroup = activeChat?.isGroup == true
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = chatSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (activeChat?.isGroup == false && recipient != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isBlocked) "Unblock User" else "Block User",
                                        color = if (isBlocked) OnlineGreen else BlockedRed
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isBlocked) OnlineGreen else BlockedRed
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (!isBlocked) {
                                        showBlockConfirmDialog = true
                                    } else {
                                        viewModel.toggleBlockActiveRecipient()
                                    }
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Change Chat Background") },
                            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.navigateTo(AppScreen.CHAT_WALLPAPER_SETTINGS)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Clear Messages") },
                            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.clearActiveChat()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat Wallpaper Background
            val wallpaperPath = appSettings.customChatWallpaperUri
            if (!wallpaperPath.isNullOrBlank() && File(wallpaperPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(wallpaperPath)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (appSettings.isDarkMode) Color.Black.copy(alpha = 0.65f)
                            else Color.White.copy(alpha = 0.75f)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Blocked banner if active user is blocked
                if (isBlocked) {
                    Surface(
                        color = BlockedRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = BlockedRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You have blocked @${recipient?.username ?: "this user"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BlockedRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            TextButton(
                                onClick = { viewModel.toggleBlockActiveRecipient() }
                            ) {
                                Text("Unblock", color = BlockedRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        val isMe = msg.senderUsername.equals(currentUser?.username, ignoreCase = true)
                        MessageBubble(
                            message = msg,
                            isMe = isMe,
                            isGroup = activeChat?.isGroup == true,
                            onMessageClick = { selectedMessageForReaction = msg },
                            onDelete = { viewModel.deleteMessage(msg.messageId) }
                        )
                    }
                }

                // Pending attachment preview
                if (pendingImageUri != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = File(pendingImageUri!!),
                                    contentDescription = "Selected photo",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Photo ready to send",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = { pendingImageUri = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove photo")
                            }
                        }
                    }
                }

                // Chat Input Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery button to attach photo
                        IconButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isBlocked,
                            modifier = Modifier.testTag("attach_gallery_button")
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = "Attach photo from gallery",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(if (isBlocked) "User is blocked" else "Type an office message...")
                            },
                            enabled = !isBlocked,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank() || pendingImageUri != null) {
                                    viewModel.sendMessage(inputText, pendingImageUri)
                                    inputText = ""
                                    pendingImageUri = null
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_message_button"),
                            containerColor = if (isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Reaction Picker Dialog
    selectedMessageForReaction?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForReaction = null },
            title = { Text("React to Message", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val reactions = listOf("👍", "❤️", "🏢", "😂", "👏", "🔥")
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 30.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.addReaction(msg.messageId, emoji)
                                    selectedMessageForReaction = null
                                }
                                .padding(6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForReaction = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Block Confirmation Dialog
    if (showBlockConfirmDialog && recipient != null) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text("Block @${recipient?.username}?") },
            text = {
                Text("Blocked contacts will not be able to send you messages or view your online office status.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleBlockActiveRecipient()
                        showBlockConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlockedRed)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    isGroup: Boolean,
    onMessageClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleColors = if (isMe) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clickable { onMessageClick() },
            shape = bubbleShape,
            colors = bubbleColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Sender tag for group chats
                if (isGroup && !isMe) {
                    Text(
                        text = "@${message.senderUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Image Attachment if present
                if (!message.imageUri.isNullOrBlank() && File(message.imageUri).exists()) {
                    AsyncImage(
                        model = File(message.imageUri),
                        contentDescription = "Shared photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Text body
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                // Bottom row: Time + read receipt + reaction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji Reaction Chip if added
                    if (!message.reaction.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isMe) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = message.reaction,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
