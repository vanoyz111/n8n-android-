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
    private val lastRepliedText = mutableMapOf<String, String>()
    private val watchedPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

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

        if (messageText.isBlank()) return
        if (lastRepliedText[sender] == messageText) {
            AppLog.add("AUTOREPLY", "Lewati duplikat dari $sender")
            return
        }

        val replyAction = notification.actions?.firstOrNull { action ->
            !action.remoteInputs.isNullOrEmpty()
        }
        if (replyAction == null) {
            AppLog.add("AUTOREPLY", "Gak ada aksi balas cepat di notifikasi dari $sender")
            return
        }

        AppLog.add("AUTOREPLY", "Pesan masuk dari $sender: ${messageText.take(60)}")

        val rules = AutoReplyStore.getRules(applicationContext)
        val matchedRule = rules.firstOrNull { rule ->
            rule.keyword.isNotBlank() && messageText.contains(rule.keyword, ignoreCase = true)
        }

        if (matchedRule != null) {
            sendReply(replyAction, matchedRule.reply, sender, messageText)
            return
        }

        if (AutoReplyStore.isAiFallbackEnabled(applicationContext)) {
            serviceScope.launch {
                val persona = AutoReplyStore.getPersonaPrompt(applicationContext)
                val prompt = if (persona.isNotBlank()) {
                    "$persona\n\nPesan masuk: $messageText"
                } else {
                    "Balas pesan WhatsApp berikut secara singkat dan ramah atas nama saya: $messageText"
                }
                val reply = AiClient.sendMessage(applicationContext, listOf(ChatMessage("user", prompt)))
                sendReply(replyAction, reply, sender, messageText)
            }
        } else {
            AppLog.add("AUTOREPLY", "Gak ada keyword cocok, AI fallback mati. Dilewati.")
        }
    }

    private fun sendReply(action: Notification.Action, replyText: String, sender: String, originalMessage: String) {
        try {
            val remoteInputs = action.remoteInputs ?: return
            val intent = Intent()
            val bundle = Bundle()
            remoteInputs.forEach { remoteInput ->
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            action.actionIntent.send(applicationContext, 0, intent)

            lastRepliedText[sender] = originalMessage
            AppLog.add("AUTOREPLY", "Balas ke $sender: ${replyText.take(60)}")
        } catch (e: PendingIntent.CanceledException) {
            AppLog.add("AUTOREPLY_ERROR", "Gagal kirim balasan: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
