package com.vano.n8nmobile.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Persona(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val mode: String
)

object PersonaStore {
    private const val PREFS_NAME = "n8n_mobile_personas"
    private const val KEY_PERSONAS = "personas"

    fun newId(): String = UUID.randomUUID().toString()

    fun getPersonas(context: Context): List<Persona> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PERSONAS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Persona(obj.getString("id"), obj.getString("name"), obj.optString("systemPrompt", ""), obj.optString("mode", "auto"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setPersonas(context: Context, personas: List<Persona>) {
        val array = JSONArray()
        personas.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("systemPrompt", p.systemPrompt); put("mode", p.mode)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_PERSONAS, array.toString()).apply()
    }
}
