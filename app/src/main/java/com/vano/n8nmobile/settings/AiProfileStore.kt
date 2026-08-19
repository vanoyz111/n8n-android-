package com.vano.n8nmobile.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKeys: List<String>,
    val model: String,
    val tier: Int
)

object AiProfileStore {
    private const val PREFS_NAME = "n8n_mobile_ai_profiles"
    private const val KEY_PROFILES = "profiles"

    fun newId(): String = UUID.randomUUID().toString()

    fun getProfiles(context: Context): List<AiProfile> {
        val raw = com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).getString(KEY_PROFILES, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val keysArray = obj.optJSONArray("apiKeys")
                val keys = if (keysArray != null) {
                    (0 until keysArray.length()).map { keysArray.getString(it) }
                } else {
                    val oldKey = obj.optString("apiKey", "")
                    if (oldKey.isNotBlank()) listOf(oldKey) else emptyList()
                }
                AiProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    baseUrl = obj.getString("baseUrl"),
                    apiKeys = keys,
                    model = obj.getString("model"),
                    tier = obj.optInt("tier", 1)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setProfiles(context: Context, profiles: List<AiProfile>) {
        val array = JSONArray()
        profiles.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("baseUrl", p.baseUrl)
                put("apiKeys", JSONArray(p.apiKeys))
                put("model", p.model)
                put("tier", p.tier)
            })
        }
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun getProfile(context: Context, id: String): AiProfile? = getProfiles(context).firstOrNull { it.id == id }
}
