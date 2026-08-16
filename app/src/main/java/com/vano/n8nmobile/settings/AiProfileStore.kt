package com.vano.n8nmobile.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)

object AiProfileStore {
    private const val PREFS_NAME = "n8n_mobile_ai_profiles"
    private const val KEY_PROFILES = "profiles"

    fun newId(): String = UUID.randomUUID().toString()

    fun getProfiles(context: Context): List<AiProfile> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PROFILES, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AiProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    baseUrl = obj.getString("baseUrl"),
                    apiKey = obj.getString("apiKey"),
                    model = obj.getString("model")
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
                put("apiKey", p.apiKey)
                put("model", p.model)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun getProfile(context: Context, id: String): AiProfile? = getProfiles(context).firstOrNull { it.id == id }
}
