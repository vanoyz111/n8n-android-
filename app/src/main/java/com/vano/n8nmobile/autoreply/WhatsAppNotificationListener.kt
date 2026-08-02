package com.vano.n8nmobile.autoreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val watchedPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    private val lock = Any()
    private val inFlightSenders = mutableSetOf<String>()
    private val lastReplyAt = mutableMapOf<String, Long>()
    private val lastProcessedContent = mutableMapOf<String, String>()
    private val recentReplyTimestamps = mutableListOf<Long>()

    private val errorPrefixes = listOf(
        "Terjadi error", "Gagal manggil Gemini", "API key Gemini belum diisi",
        "Gemini gak ngasih jawaban", "Base URL AI belum diisi", "Gagal manggil AI",
        "AI gak ngasih jawaban", "Model lokal belum didownload", "Gagal memuat model lokal",
        "Gagal menjalankan AI lokal", "Model LiteRT belum didownload"
    )

    companion object {
        private const val SENDER_COOLDOWN_MS = 15_000L
        private const val CIRCUIT_BREAKER_WINDOW_MS = 60_000L
        private const val CIRCUIT_BREAKER_MAX_REPLIES = 5
        private val PHONE_NUMBER_REGEX = Regex("^[+0-9\\s\\-()]{6,}$")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AutoReplyStore.isEnabled(applicationContext)) return
        if (sbn.packageName !in watchedPackages) return

        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown"
        var messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val messagesArray: Array<Parcelable>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }
        if (!messagesArray.isNullOrEmpty()) {
            val lastMsgBundle = messagesArray.last() as? Bundle
            val text = lastMsgBundle?.getCharSequence("text")?.toString()
            if (!text.isNullOrBlank()) messageText = text
        }

        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        if (isGroup && !AutoReplyStore.isGroupEnabled(applicationContext)) {
            AppLog.add("AUTOREPLY", "Dilewati: pesan grup dari $sender, grup belum diaktifkan")
            return
        }

        if (!isSenderAllowed(sender)) {
            AppLog.add("AUTOREPLY", "Dilewati: $sender gak lolos filter kontak")
            return
        }

        if (messageText.isBlank()) return

        val replyAction = notification.actions?.firstOrNull { action ->
            !action.remoteInputs.isNullOrEmpty()
        }
        if (replyAction == null) {
            AppLog.add("AUTOREPLY", "Gak ada aksi balas cepat dari $sender")
            return
        }

        val allowed = synchronized(lock) {
            if (lastProcessedContent[sender] == messageText) {
                false
            } else if (inFlightSenders.contains(sender)) {
                AppLog.add("AUTOREPLY", "Dilewati: masih proses balasan sebelumnya buat $sender")
                false
            } else {
                val lastAt = lastReplyAt[sender] ?: 0L
                val now = System.currentTimeMillis()
                if (now - lastAt < SENDER_COOLDOWN_MS) {
                    AppLog.add("AUTOREPLY", "Dilewati: masih cooldown buat $sender")
                    false
                } else {
                    inFlightSenders.add(sender)
                    lastProcessedContent[sender] = messageText
                    true
                }
            }
        }
        if (!allowed) return

        AppLog.add("AUTOREPLY", "Pesan masuk dari $sender: ${messageText.take(60)}")

        val rules = AutoReplyStore.getRules(applicationContext)
        val matchedRule = rules.firstOrNull { rule ->
            rule.keyword.isNotBlank() && messageText.contains(rule.keyword, ignoreCase = true)
        }

        if (matchedRule != null) {
            trySendReply(replyAction, matchedRule.reply, sender)
            return
        }

        if (AutoReplyStore.isAiFallbackEnabled(applicationContext)) {
            serviceScope.launch {
                try {
                    val persona = AutoReplyStore.getPersonaPrompt(applicationContext)
                    val prompt = if (persona.isNotBlank()) {
                        "$persona\n\nPesan masuk: $messageText"
                    } else {
                        "Balas pesan WhatsApp berikut secara singkat dan ramah atas nama saya: $messageText"
                    }
                    val mode = AutoReplyStore.getAiMode(applicationContext)
                    val reply = AiClient.sendMessageWithMode(applicationContext, listOf(ChatMessage("user", prompt)), mode)
                    if (errorPrefixes.any { reply.startsWith(it) }) {
                        AppLog.add("AUTOREPLY_ERROR", "AI gagal, TIDAK dikirim ke WhatsApp: ${reply.take(100)}")
                        releaseSender(sender, applyCooldown = false)
                    } else {
                        trySendReply(replyAction, reply, sender)
                    }
                } catch (e: Exception) {
                    AppLog.add("AUTOREPLY_ERROR", "Exception, TIDAK dikirim ke WhatsApp: ${e.message}")
                    releaseSender(sender, applyCooldown = false)
                }
            }
        } else {
            AppLog.add("AUTOREPLY", "Gak ada keyword cocok, AI fallback mati. Dilewati.")
            releaseSender(sender, applyCooldown = false)
        }
    }

    private fun isSenderAllowed(sender: String): Boolean {
        val mode = AutoReplyStore.getContactFilterMode(applicationContext)
        val contactList = AutoReplyStore.getContactList(applicationContext)
        return when (mode) {
            "WHITELIST" -> contactList.any { it.equals(sender, ignoreCase = true) }
            "BLACKLIST" -> contactList.none { it.equals(sender, ignoreCase = true) }
            "EXCEPT_PHONE_CONTACTS" -> PHONE_NUMBER_REGEX.matches(sender.trim())
            else -> true
        }
    }

    private fun trySendReply(action: Notification.Action, replyText: String, sender: String) {
        val tripped = synchronized(lock) {
            val now = System.currentTimeMillis()
            recentReplyTimestamps.removeAll { now - it > CIRCUIT_BREAKER_WINDOW_MS }
            if (recentReplyTimestamps.size >= CIRCUIT_BREAKER_MAX_REPLIES) {
                AutoReplyStore.setEnabled(applicationContext, false)
                AppLog.add(
                    "AUTOREPLY_CRITICAL",
                    "Circuit breaker aktif: Auto-Reply DIMATIKAN otomatis (kebanyakan balasan dalam 1 menit)"
                )
                true
            } else {
                recentReplyTimestamps.add(now)
                false
            }
        }
        if (tripped) {
            releaseSender(sender, applyCooldown = true)
            return
        }

        try {
            val remoteInputs = action.remoteInputs ?: run {
                releaseSender(sender, applyCooldown = false)
                return
            }
            val intent = Intent()
            val bundle = Bundle()
            remoteInputs.forEach { remoteInput ->
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            action.actionIntent.send(applicationContext, 0, intent)
            AppLog.add("AUTOREPLY", "Balas ke $sender: ${replyText.take(60)}")
        } catch (e: PendingIntent.CanceledException) {
            AppLog.add("AUTOREPLY_ERROR", "Gagal kirim balasan: ${e.message}")
        } finally {
            releaseSender(sender, applyCooldown = true)
        }
    }

    private fun releaseSender(sender: String, applyCooldown: Boolean) {
        synchronized(lock) {
            inFlightSenders.remove(sender)
            if (applyCooldown) {
                lastReplyAt[sender] = System.currentTimeMillis()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
