package com.vano.n8nmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.canvas.CanvasEdge
import com.vano.n8nmobile.canvas.CanvasNode
import com.vano.n8nmobile.canvas.FlowStore
import com.vano.n8nmobile.canvas.WorkflowCanvasScreen
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.chat.ChatScreen
import com.vano.n8nmobile.chat.ChatStore
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

            var currentConversationId by remember { mutableStateOf(ChatStore.newId()) }
            val chatMessages = remember { mutableStateListOf<ChatMessage>() }
            var conversationsList by remember { mutableStateOf(ChatStore.loadAll(context)) }

            fun persistCurrentConversation() {
                if (chatMessages.isNotEmpty()) {
                    val title = chatMessages.firstOrNull { it.role == "user" && it.text.isNotBlank() }
                        ?.text?.take(40) ?: "Percakapan baru"
                    ChatStore.save(context, currentConversationId, title, chatMessages.toList())
                    conversationsList = ChatStore.loadAll(context)
                }
            }

            val savedFlow = remember { FlowStore.load(context) }
            val flowNodes = remember { mutableStateListOf<CanvasNode>().apply { addAll(savedFlow.nodes) } }
            val flowEdges = remember { mutableStateListOf<CanvasEdge>().apply { addAll(savedFlow.edges) } }
            val flowPositions = remember { mutableStateMapOf<String, Offset>().apply { putAll(savedFlow.positions) } }
            val flowNextId = remember { mutableStateOf(savedFlow.nextId) }

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
                                    "Aiwa",
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
                                    "Riwayat Chat",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                if (conversationsList.isEmpty()) {
                                    Text(
                                        "Belum ada riwayat",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                } else {
                                    conversationsList.forEach { conv ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    chatMessages.clear()
                                                    chatMessages.addAll(conv.messages)
                                                    currentConversationId = conv.id
                                                    currentScreen = AppScreen.CHAT
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(conv.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(onClick = {
                                                ChatStore.delete(context, conv.id)
                                                conversationsList = ChatStore.loadAll(context)
                                                if (conv.id == currentConversationId) {
                                                    chatMessages.clear()
                                                    currentConversationId = ChatStore.newId()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Hapus percakapan")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        when (currentScreen) {
                            AppScreen.CHAT -> ChatScreen(
                                messages = chatMessages,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNewChat = {
                                    chatMessages.clear()
                                    currentConversationId = ChatStore.newId()
                                },
                                onMessagesChanged = { persistCurrentConversation() }
                            )
                            AppScreen.FLOW -> WorkflowCanvasScreen(
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                nodes = flowNodes,
                                edges = flowEdges,
                                positions = flowPositions,
                                nextIdState = flowNextId
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
