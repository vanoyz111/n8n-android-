package com.vano.n8nmobile.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PromptTemplate(val id: String, val name: String, val content: String)

object PromptTemplateStore {
    private const val PREFS_NAME = "n8n_mobile_prompt_templates"
    private const val KEY_TEMPLATES = "templates"

    fun getTemplates(context: Context): List<PromptTemplate> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TEMPLATES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PromptTemplate(obj.getString("id"), obj.getString("name"), obj.getString("content"))
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun setTemplates(context: Context, templates: List<PromptTemplate>) {
        val array = JSONArray()
        templates.forEach { t -> array.put(JSONObject().apply { put("id", t.id); put("name", t.name); put("content", t.content) }) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    fun add(context: Context, name: String, content: String) {
        setTemplates(context, getTemplates(context) + PromptTemplate(UUID.randomUUID().toString(), name, content))
    }

    fun remove(context: Context, id: String) {
        setTemplates(context, getTemplates(context).filterNot { it.id == id })
    }
}
