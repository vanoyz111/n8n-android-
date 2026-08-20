package com.vano.n8nmobile.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ImageAttachment(val base64: String, val mimeType: String)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
    val isPinned: Boolean = false,
    val isTitleManual: Boolean = false
)

object ChatStore {
    private const val PREFS_NAME = "n8n_mobile_chat"
    private const val KEY_CONVERSATIONS = "conversations"

    fun newId(): String = UUID.randomUUID().toString()

    fun loadAll(context: Context): List<Conversation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val msgsArray = obj.getJSONArray("messages")
                val msgs = (0 until msgsArray.length()).map { j -> parseMessage(msgsArray.getJSONObject(j)) }
                Conversation(
                    id = obj.getString("id"),
                    title = obj.optString("title", "Percakapan"),
                    updatedAt = obj.optLong("updatedAt", 0L),
                    messages = msgs,
                    isPinned = obj.optBoolean("isPinned", false),
                    isTitleManual = obj.optBoolean("isTitleManual", false)
                )
            }.sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.updatedAt })
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMessage(m: JSONObject): ChatMessage {
        val imageAttachments = mutableListOf<ImageAttachment>()
        val fileNames = mutableListOf<String>()

        val imagesArr = m.optJSONArray("imageAttachments")
        if (imagesArr != null) {
            for (i in 0 until imagesArr.length()) {
                val img = imagesArr.getJSONObject(i)
                imageAttachments.add(ImageAttachment(img.getString("base64"), img.getString("mimeType")))
            }
        } else if (m.has("imageBase64")) {
            val b64 = m.optString("imageBase64", "")
            if (b64.isNotBlank()) imageAttachments.add(ImageAttachment(b64, m.optString("imageMimeType", "image/jpeg")))
        }

        val filesArr = m.optJSONArray("fileAttachmentNames")
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) fileNames.add(filesArr.getString(i))
        } else if (m.has("attachmentName")) {
            val name = m.optString("attachmentName", "")
            if (name.isNotBlank()) fileNames.add(name)
        }

        return ChatMessage(
            role = m.getString("role"),
            text = m.optString("text", ""),
            imageAttachments = imageAttachments,
            fileAttachmentNames = fileNames
        )
    }

    private fun persistAll(context: Context, conversations: List<Conversation>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        conversations.forEach { conv ->
            val msgsArray = JSONArray()
            conv.messages.forEach { msg ->
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                msgObj.put("text", msg.text)
                if (msg.imageAttachments.isNotEmpty()) {
                    val imgArr = JSONArray()
                    msg.imageAttachments.forEach { img ->
                        imgArr.put(JSONObject().apply { put("base64", img.base64); put("mimeType", img.mimeType) })
                    }
                    msgObj.put("imageAttachments", imgArr)
                }
                if (msg.fileAttachmentNames.isNotEmpty()) {
                    msgObj.put("fileAttachmentNames", JSONArray(msg.fileAttachmentNames))
                }
                msgsArray.put(msgObj)
            }
            array.put(JSONObject().apply {
                put("id", conv.id); put("title", conv.title)
                put("updatedAt", conv.updatedAt); put("messages", msgsArray)
                put("isPinned", conv.isPinned); put("isTitleManual", conv.isTitleManual)
            })
        }
        prefs.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    fun save(context: Context, id: String, title: String, messages: List<ChatMessage>) {
        val existing = loadAll(context)
        val existingConv = existing.firstOrNull { it.id == id }
        val finalTitle = if (existingConv?.isTitleManual == true) existingConv.title else title
        val isPinned = existingConv?.isPinned ?: false
        val isTitleManual = existingConv?.isTitleManual ?: false
        val filtered = existing.filterNot { it.id == id }
        persistAll(context, filtered + Conversation(id, finalTitle, System.currentTimeMillis(), messages, isPinned, isTitleManual))
    }

    fun renameConversation(context: Context, id: String, newTitle: String) {
        val list = loadAll(context)
        persistAll(context, list.map { if (it.id == id) it.copy(title = newTitle, isTitleManual = true) else it })
    }

    fun setPinned(context: Context, id: String, pinned: Boolean) {
        val list = loadAll(context)
        persistAll(context, list.map { if (it.id == id) it.copy(isPinned = pinned) else it })
    }

    fun delete(context: Context, id: String) {
        persistAll(context, loadAll(context).filterNot { it.id == id })
    }

    fun cleanupOldConversations(context: Context) {
        if (!ChatRetentionStore.isEnabled(context)) return
        val days = ChatRetentionStore.getDays(context)
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60_000L
        val all = loadAll(context)
        val toKeep = all.filter { it.isPinned || it.updatedAt >= cutoff }
        if (toKeep.size != all.size) {
            persistAll(context, toKeep)
        }
    }

    fun exportAsText(conversation: Conversation): String {
        val sb = StringBuilder()
        sb.append("Percakapan: ${conversation.title}\n\n")
        conversation.messages.forEach { msg ->
            val speaker = if (msg.role == "user") "Kamu" else "AI"
            sb.append("$speaker: ${msg.text}\n\n")
        }
        return sb.toString()
    }
}
