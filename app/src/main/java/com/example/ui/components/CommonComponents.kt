package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.OfflineGrey
import com.example.ui.theme.OnlineGreen
import java.io.File

@Composable
fun UserAvatar(
    avatarUri: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    isGroup: Boolean = false
) {
    Box(
        modifier = modifier.size(size)
    ) {
        if (!avatarUri.isNullOrBlank() && File(avatarUri).exists()) {
            AsyncImage(
                model = File(avatarUri),
                contentDescription = "$displayName avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Initials Fallback with Gradient
            val initial = if (isGroup) "👥" else displayName.trim().take(1).uppercase().ifEmpty { "U" }
            val bgGradient = if (isGroup) {
                listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
            } else {
                val colors = listOf(
                    listOf(Color(0xFF00A86B), Color(0xFF065F46)),
                    listOf(Color(0xFF2563EB), Color(0xFF1E40AF)),
                    listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                    listOf(Color(0xFFE11D48), Color(0xFF9F1239)),
                    listOf(Color(0xFFD97706), Color(0xFF92400E))
                )
                colors[Math.abs(displayName.hashCode()) % colors.size]
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(bgGradient)),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.42f).sp
                    )
                }
            }
        }

        // Presence indicator dot if specified
        if (isOnline != null && !isGroup) {
            val badgeSize = (size.value * 0.3f).coerceIn(10f, 16f).dp
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else OfflineGrey)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val bg = when {
        status.contains("Available", true) -> OnlineGreen.copy(alpha = 0.15f)
        status.contains("Busy", true) || status.contains("Meeting", true) -> Color(0xFFEF4444).copy(alpha = 0.15f)
        status.contains("Office", true) -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> OfflineGrey.copy(alpha = 0.15f)
    }

    val textColor = when {
        status.contains("Available", true) -> OnlineGreen
        status.contains("Busy", true) || status.contains("Meeting", true) -> Color(0xFFEF4444)
        status.contains("Office", true) -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = status,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
