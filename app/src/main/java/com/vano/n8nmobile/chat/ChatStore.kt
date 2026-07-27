package com.vano.n8nmobile.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChatStore {
    private const val PREFS_NAME = "n8n_mobile_chat"
    private const val KEY_MESSAGES = "messages"

    fun load(context: Context): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ChatMessage(role = obj.getString("role"), text = obj.getString("text"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        messages.forEach { msg ->
            array.put(JSONObject().apply {
                put("role", msg.role)
                put("text", msg.text)
            })
        }
        prefs.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }
}
