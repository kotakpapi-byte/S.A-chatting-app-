package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val SaEmeraldPrimary = Color(0xFF00A86B)
val SaEmeraldDark = Color(0xFF0B6641)
val SaTeal = Color(0xFF0D9488)
val SaSapphire = Color(0xFF2563EB)
val SaViolet = Color(0xFF7C3AED)
val SaRose = Color(0xFFE11D48)
val SaMidnight = Color(0xFF1E293B)

val DarkSurface = Color(0xFF0F172A)
val DarkBackground = Color(0xFF0B1120)
val DarkCard = Color(0xFF1E293B)
val DarkBorder = Color(0xFF334155)

val LightSurface = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF8FAFC)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE2E8F0)

val OnlineGreen = Color(0xFF22C55E)
val OfflineGrey = Color(0xFF94A3B8)
val BlockedRed = Color(0xFFEF4444)

data class AppThemeOption(
    val name: String,
    val primaryColor: Color,
    val hex: String
)

val AvailableThemes = listOf(
    AppThemeOption("Emerald S.A", Color(0xFF00A86B), "#00A86B"),
    AppThemeOption("Deep Teal", Color(0xFF0D9488), "#0D9488"),
    AppThemeOption("Sapphire Blue", Color(0xFF2563EB), "#2563EB"),
    AppThemeOption("Royal Violet", Color(0xFF7C3AED), "#7C3AED"),
    AppThemeOption("Rose Coral", Color(0xFFE11D48), "#E11D48"),
    AppThemeOption("Midnight Navy", Color(0xFF1E293B), "#1E293B")
)

fun parseHexColor(hex: String, fallback: Color = SaEmeraldPrimary): Color {
    return try {
        val clean = hex.removePrefix("#")
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else fallback
    } catch (e: Exception) {
        fallback
    }
}
