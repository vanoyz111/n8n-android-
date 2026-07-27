package com.vano.n8nmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.canvas.WorkflowCanvasScreen
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.chat.ChatScreen
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.settings.SettingsScreen
import com.vano.n8nmobile.settings.SettingsStore
import kotlinx.coroutines.launch

private enum class AppScreen { CHAT, FLOW, SETTINGS }

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val context = LocalContext.current
            val settingsStore = remember { SettingsStore(context) }
            var isDark by remember { mutableStateOf(settingsStore.darkTheme) }
            val colors = if (isDark) darkColorScheme() else lightColorScheme()
            val chatMessages = remember { mutableStateListOf<ChatMessage>() }

            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Text(
                                    "n8n Mobile",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                                NavigationDrawerItem(
                                    label = { Text("Chat") },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                    selected = currentScreen == AppScreen.CHAT,
                                    onClick = {
                                        currentScreen = AppScreen.CHAT
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                NavigationDrawerItem(
                                    label = { Text("Flow") },
                                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                                    selected = currentScreen == AppScreen.FLOW,
                                    onClick = {
                                        currentScreen = AppScreen.FLOW
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                NavigationDrawerItem(
                                    label = { Text("Settings") },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    selected = currentScreen == AppScreen.SETTINGS,
                                    onClick = {
                                        currentScreen = AppScreen.SETTINGS
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    "Riwayat",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                if (AppLog.entries.isEmpty()) {
                                    Text(
                                        "Belum ada riwayat",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                } else {
                                    AppLog.entries.take(15).forEach { entry ->
                                        Text(
                                            "[${entry.tag}] ${entry.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        when (currentScreen) {
                            AppScreen.CHAT -> ChatScreen(
                                messages = chatMessages,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                            AppScreen.FLOW -> WorkflowCanvasScreen(
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onThemeChanged = { newDark -> isDark = newDark }
                            )
                        }
                    }
                }
            }
        }
    }
}
