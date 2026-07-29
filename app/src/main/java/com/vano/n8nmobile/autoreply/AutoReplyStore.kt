package com.vano.n8nmobile.autoreply

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object AutoReplyStore {
    private const val PREFS_NAME = "n8n_mobile_autoreply"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_AI_FALLBACK = "ai_fallback"
    private const val KEY_PERSONA_PROMPT = "persona_prompt"
    private const val KEY_RULES = "rules"

    fun newId(): String = UUID.randomUUID().toString()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isAiFallbackEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AI_FALLBACK, false)

    fun setAiFallbackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AI_FALLBACK, enabled).apply()
    }

    fun getPersonaPrompt(context: Context): String = prefs(context).getString(KEY_PERSONA_PROMPT, "") ?: ""

    fun setPersonaPrompt(context: Context, prompt: String) {
        prefs(context).edit().putString(KEY_PERSONA_PROMPT, prompt).apply()
    }

    fun getRules(context: Context): List<AutoReplyRule> {
        val raw = prefs(context).getString(KEY_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AutoReplyRule(id = obj.getString("id"), keyword = obj.getString("keyword"), reply = obj.getString("reply"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setRules(context: Context, rules: List<AutoReplyRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(JSONObject().apply {
                put("id", rule.id)
                put("keyword", rule.keyword)
                put("reply", rule.reply)
            })
        }
        prefs(context).edit().putString(KEY_RULES, array.toString()).apply()
    }
}
