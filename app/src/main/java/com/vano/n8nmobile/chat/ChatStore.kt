package com.vano.n8nmobile.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
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
                val msgs = (0 until msgsArray.length()).map { j ->
                    val m = msgsArray.getJSONObject(j)
                    ChatMessage(
                        role = m.getString("role"),
                        text = m.optString("text", ""),
                        imageBase64 = if (m.has("imageBase64")) m.optString("imageBase64", null) else null,
                        imageMimeType = if (m.has("imageMimeType")) m.optString("imageMimeType", null) else null,
                        attachmentName = if (m.has("attachmentName")) m.optString("attachmentName", null) else null
                    )
                }
                Conversation(
                    id = obj.getString("id"),
                    title = obj.optString("title", "Percakapan"),
                    updatedAt = obj.optLong("updatedAt", 0L),
                    messages = msgs
                )
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
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
                msg.imageBase64?.let { msgObj.put("imageBase64", it) }
                msg.imageMimeType?.let { msgObj.put("imageMimeType", it) }
                msg.attachmentName?.let { msgObj.put("attachmentName", it) }
                msgsArray.put(msgObj)
            }
            array.put(JSONObject().apply {
                put("id", conv.id)
                put("title", conv.title)
                put("updatedAt", conv.updatedAt)
                put("messages", msgsArray)
            })
        }
        prefs.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    fun save(context: Context, id: String, title: String, messages: List<ChatMessage>) {
        val existing = loadAll(context).filterNot { it.id == id }
        persistAll(context, existing + Conversation(id, title, System.currentTimeMillis(), messages))
    }

    fun delete(context: Context, id: String) {
        persistAll(context, loadAll(context).filterNot { it.id == id })
    }
}
