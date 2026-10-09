package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthContainer
import com.example.ui.chat.ChatListScreen
import com.example.ui.chat.ConversationScreen
import com.example.ui.profile.BlockedUsersScreen
import com.example.ui.profile.ChatWallpaperScreen
import com.example.ui.profile.OfficeDirectoryScreen
import com.example.ui.profile.ProfileSettingsScreen
import com.example.ui.profile.ThemeCustomizerScreen
import com.example.ui.theme.SAChatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by viewModel.appSettings.collectAsState()

            SAChatTheme(
                primaryColorHex = appSettings.themeColorHex,
                customWallpaperPath = appSettings.customAppWallpaperUri,
                darkTheme = appSettings.isDarkMode
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentUser != null,
        label = "AuthSessionCrossfade"
    ) { isLoggedIn ->
        if (!isLoggedIn) {
            AuthContainer(viewModel = viewModel)
        } else {
            Crossfade(
                targetState = currentScreen,
                label = "ScreenCrossfade"
            ) { screen ->
                when (screen) {
                    AppScreen.CHATS -> ChatListScreen(viewModel = viewModel)
                    AppScreen.CONVERSATION -> ConversationScreen(viewModel = viewModel)
                    AppScreen.PROFILE_SETTINGS -> ProfileSettingsScreen(viewModel = viewModel)
                    AppScreen.THEME_SETTINGS -> ThemeCustomizerScreen(viewModel = viewModel)
                    AppScreen.CHAT_WALLPAPER_SETTINGS -> ChatWallpaperScreen(viewModel = viewModel)
                    AppScreen.BLOCKED_USERS -> BlockedUsersScreen(viewModel = viewModel)
                    AppScreen.OFFICE_DIRECTORY -> OfficeDirectoryScreen(viewModel = viewModel)
                }
            }
        }
    }
}
