package com.vano.n8nmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vano.n8nmobile.canvas.CanvasEdge
import com.vano.n8nmobile.canvas.CanvasNode
import com.vano.n8nmobile.canvas.FlowStore
import com.vano.n8nmobile.canvas.WorkflowCanvasScreen
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.chat.ChatScreen
import com.vano.n8nmobile.chat.ChatStore
import com.vano.n8nmobile.autoreply.AutoReplyScreen
import com.vano.n8nmobile.autoreply.AutoReplyStore
import com.vano.n8nmobile.autoreply.WhatsAppNotificationListener
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import com.vano.n8nmobile.localai.LocalModelScreen
import com.vano.n8nmobile.localai.ModelSettingsScreen
import com.vano.n8nmobile.imagegen.ImageGenScreen
import com.vano.n8nmobile.settings.AiProvidersScreen
import com.vano.n8nmobile.server.LocalServerScreen
import com.vano.n8nmobile.security.AppLockScreen
import com.vano.n8nmobile.security.AppLockSettingsScreen
import com.vano.n8nmobile.security.AppLockStore
import com.vano.n8nmobile.backup.BackupRestoreScreen
import com.vano.n8nmobile.canvas.FlowScheduler
import com.vano.n8nmobile.server.HealthCheckScheduler
import com.vano.n8nmobile.settings.SettingsScreen
import com.vano.n8nmobile.settings.SettingsStore
import com.vano.n8nmobile.ui.AiwaColorScheme
import com.vano.n8nmobile.ui.AiwaColors
import com.vano.n8nmobile.ui.AiwaDecorativeFont
import com.vano.n8nmobile.ui.AiwaHeaderGradient
import com.vano.n8nmobile.ui.AiwaPillGradient
import com.vano.n8nmobile.ui.AiwaThemeStore
import com.vano.n8nmobile.ui.ThemeCustomizationScreen
import kotlinx.coroutines.launch

private enum class AppScreen { CHAT, FLOW, SETTINGS, AUTOREPLY, LOCAL_AI, THEME_CUSTOM, MODEL_SETTINGS, IMAGE_GEN, AI_PROVIDERS, LOCAL_SERVER, APP_LOCK, BACKUP }

class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (AutoReplyStore.isEnabled(this)) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(this, WhatsAppNotificationListener::class.java)
                )
            } catch (e: Exception) { }
        }

        FlowScheduler.scheduleIfNeeded(this)
        HealthCheckScheduler.scheduleNext(this)

        setContent {
            val context = LocalContext.current
            remember { AiwaThemeStore(context).loadIntoMemory() }

            var isUnlocked by remember { mutableStateOf(!AppLockStore.isLockEnabled(context)) }
            val settingsStore = remember { SettingsStore(context) }
            var isDark by remember { mutableStateOf(settingsStore.darkTheme) }
            val colors = if (isDark) AiwaColorScheme else lightColorScheme()

            val initialConversation = remember { ChatStore.loadAll(context).firstOrNull() }
            var currentConversationId by remember { mutableStateOf(initialConversation?.id ?: ChatStore.newId()) }
            val chatMessages = remember {
                mutableStateListOf<ChatMessage>().apply {
                    initialConversation?.let { addAll(it.messages) }
                }
            }

            val savedFlow = remember { FlowStore.load(context) }
            val flowNodes = remember { mutableStateListOf<CanvasNode>().apply { addAll(savedFlow.nodes) } }
            val flowEdges = remember { mutableStateListOf<CanvasEdge>().apply { addAll(savedFlow.edges) } }
            val flowPositions = remember { mutableStateMapOf<String, Offset>().apply { putAll(savedFlow.positions) } }
            val flowNextId = remember { mutableStateOf(savedFlow.nextId) }

            MaterialTheme(colorScheme = colors) {
                if (!isUnlocked) {
                    AppLockScreen(onUnlocked = { isUnlocked = true })
                    return@MaterialTheme
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var conversationsList by remember { mutableStateOf(ChatStore.loadAll(context)) }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color.Transparent
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AiwaHeaderGradient)
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.mipmap.ic_launcher),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "AIWA",
                                            color = Color.White,
                                            fontFamily = AiwaDecorativeFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 26.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    AiwaDrawerButton(Icons.Default.Chat, "Chat") {
                                        currentScreen = AppScreen.CHAT
                                        scope.launch { drawerState.close() }
                                    }
                                    AiwaDrawerButton(Icons.Default.AccountTree, "Flow") {
                                        currentScreen = AppScreen.FLOW
                                        scope.launch { drawerState.close() }
                                    }
                                    AiwaDrawerButton(Icons.Default.Settings, "Setting") {
                                        currentScreen = AppScreen.SETTINGS
                                        scope.launch { drawerState.close() }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Riwayat chat",
                                        color = Color.White,
                                        fontFamily = AiwaDecorativeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(AiwaColors.PanelBlack)
                                            .padding(12.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (conversationsList.isEmpty()) {
                                                Text("Belum ada riwayat", color = Color.White.copy(alpha = 0.6f))
                                            } else {
                                                conversationsList.forEach { conv ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                chatMessages.clear()
                                                                chatMessages.addAll(conv.messages)
                                                                currentConversationId = conv.id
                                                                currentScreen = AppScreen.CHAT
                                                                scope.launch { drawerState.close() }
                                                            }
                                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            conv.title,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(end = 8.dp)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(26.dp)
                                                                .clip(CircleShape)
                                                                .background(AiwaColors.Pink)
                                                                .clickable {
                                                                    ChatStore.delete(context, conv.id)
                                                                    conversationsList = ChatStore.loadAll(context)
                                                                    if (conv.id == currentConversationId) {
                                                                        chatMessages.clear()
                                                                        currentConversationId = ChatStore.newId()
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Hapus",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
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
                                    conversationsList = ChatStore.loadAll(context)
                                },
                                onMessagesChanged = {
                                    if (chatMessages.isNotEmpty()) {
                                        val title = chatMessages.firstOrNull { it.role == "user" && it.text.isNotBlank() }
                                            ?.text?.take(40) ?: "Percakapan baru"
                                        ChatStore.save(context, currentConversationId, title, chatMessages.toList())
                                        conversationsList = ChatStore.loadAll(context)
                                    }
                                },
                                onOpenAiProviders = { currentScreen = AppScreen.AI_PROVIDERS }
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
                                onThemeChanged = { newDark -> isDark = newDark },
                                onOpenAutoReply = { currentScreen = AppScreen.AUTOREPLY },
                                onOpenLocalAi = { currentScreen = AppScreen.LOCAL_AI },
                                onOpenThemeCustomization = { currentScreen = AppScreen.THEME_CUSTOM },
                                onOpenImageGen = { currentScreen = AppScreen.IMAGE_GEN },
                                onOpenAiProviders = { currentScreen = AppScreen.AI_PROVIDERS },
                                onOpenLocalServer = { currentScreen = AppScreen.LOCAL_SERVER },
                                onOpenAppLock = { currentScreen = AppScreen.APP_LOCK },
                                onOpenBackup = { currentScreen = AppScreen.BACKUP }
                            )
                            AppScreen.APP_LOCK -> AppLockSettingsScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.BACKUP -> BackupRestoreScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.AI_PROVIDERS -> AiProvidersScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.LOCAL_SERVER -> LocalServerScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.IMAGE_GEN -> ImageGenScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.AUTOREPLY -> AutoReplyScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.LOCAL_AI -> LocalModelScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS },
                                onOpenModelSettings = { currentScreen = AppScreen.MODEL_SETTINGS }
                            )
                            AppScreen.MODEL_SETTINGS -> ModelSettingsScreen(
                                onBack = { currentScreen = AppScreen.LOCAL_AI }
                            )
                            AppScreen.THEME_CUSTOM -> ThemeCustomizationScreen(
                                onBack = { currentScreen = AppScreen.SETTINGS }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AiwaDrawerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(AiwaPillGradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            color = Color.White,
            fontFamily = AiwaDecorativeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
