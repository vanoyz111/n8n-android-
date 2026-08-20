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
    private const val KEY_CONTACT_FILTER_MODE = "contact_filter_mode"
    private const val KEY_GROUP_ENABLED = "group_enabled"
    private const val KEY_CONTACT_LIST = "contact_list"
    private const val KEY_AI_MODE = "ai_mode"
        private const val KEY_BH_ENABLED = "business_hours_enabled"
        private const val KEY_BH_START = "business_hours_start"
        private const val KEY_BH_END = "business_hours_end"

    fun newId(): String = UUID.randomUUID().toString()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isTtsReadoutEnabled(context: Context): Boolean = prefs(context).getBoolean("tts_readout_enabled", false)
    fun setTtsReadoutEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("tts_readout_enabled", v).apply() }

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

    // "EVERYONE", "WHITELIST", "BLACKLIST", "EXCEPT_PHONE_CONTACTS"
    fun getContactFilterMode(context: Context): String =
        prefs(context).getString(KEY_CONTACT_FILTER_MODE, "EVERYONE") ?: "EVERYONE"

    fun setContactFilterMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_CONTACT_FILTER_MODE, mode).apply()
    }

    fun isGroupEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_GROUP_ENABLED, false)

    fun setGroupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GROUP_ENABLED, enabled).apply()
    }

    fun getContactList(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_CONTACT_LIST, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setContactList(context: Context, contacts: List<String>) {
        val array = JSONArray()
        contacts.forEach { array.put(it) }
        prefs(context).edit().putString(KEY_CONTACT_LIST, array.toString()).apply()
    }

    fun isBusinessHoursEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BH_ENABLED, false)
    fun setBusinessHoursEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean(KEY_BH_ENABLED, v).apply() }

    fun getBusinessHoursStart(context: Context): Int = prefs(context).getInt(KEY_BH_START, 9)
    fun setBusinessHoursStart(context: Context, v: Int) { prefs(context).edit().putInt(KEY_BH_START, v).apply() }

    fun getBusinessHoursEnd(context: Context): Int = prefs(context).getInt(KEY_BH_END, 17)
    fun setBusinessHoursEnd(context: Context, v: Int) { prefs(context).edit().putInt(KEY_BH_END, v).apply() }

    fun isPreviewModeEnabled(context: Context): Boolean = prefs(context).getBoolean("preview_mode_enabled", false)
    fun setPreviewModeEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("preview_mode_enabled", v).apply() }

    // "auto", "local_gguf", "local_litert", "online"
    fun getAiMode(context: Context): String = prefs(context).getString(KEY_AI_MODE, "auto") ?: "auto"

    fun setAiMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_AI_MODE, mode).apply()
    }
}
