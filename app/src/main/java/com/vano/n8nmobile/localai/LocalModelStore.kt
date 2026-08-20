package com.vano.n8nmobile.localai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ModelEntry(val id: String, val name: String, val path: String)

object LocalModelStore {
    private const val PREFS_NAME = "n8n_mobile_local_ai"
    private const val KEY_MODELS = "models_list"
    private const val KEY_ACTIVE_ID = "active_model_id"
    private const val KEY_MIGRATED = "migrated_v2"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureMigrated(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_MIGRATED, false)) return
        val legacyPath = p.getString("model_path", null)
        val legacyName = p.getString("model_name", null)
        if (legacyPath != null && File(legacyPath).exists()) {
            val id = UUID.randomUUID().toString()
            val array = JSONArray()
            array.put(JSONObject().apply {
                put("id", id); put("name", legacyName ?: "Model Lama"); put("path", legacyPath)
            })
            p.edit()
                .putString(KEY_MODELS, array.toString())
                .putString(KEY_ACTIVE_ID, id)
                .putBoolean(KEY_MIGRATED, true)
                .apply()
        } else {
            p.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    fun getModels(context: Context): List<ModelEntry> {
        ensureMigrated(context)
        val raw = prefs(context).getString(KEY_MODELS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val entry = ModelEntry(obj.getString("id"), obj.getString("name"), obj.getString("path"))
                if (File(entry.path).exists()) entry else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun setModels(context: Context, models: List<ModelEntry>) {
        val array = JSONArray()
        models.forEach { m -> array.put(JSONObject().apply { put("id", m.id); put("name", m.name); put("path", m.path) }) }
        prefs(context).edit().putString(KEY_MODELS, array.toString()).apply()
    }

    fun addModel(context: Context, name: String, path: String): String {
        ensureMigrated(context)
        val id = UUID.randomUUID().toString()
        setModels(context, getModels(context) + ModelEntry(id, name, path))
        setActiveModelId(context, id)
        return id
    }

    fun removeModel(context: Context, id: String) {
        val models = getModels(context)
        models.firstOrNull { it.id == id }?.let { try { File(it.path).delete() } catch (e: Exception) { } }
        val updated = models.filterNot { it.id == id }
        setModels(context, updated)
        if (getActiveModelId(context) == id) {
            setActiveModelId(context, updated.firstOrNull()?.id)
        }
    }

    fun getActiveModelId(context: Context): String? {
        ensureMigrated(context)
        return prefs(context).getString(KEY_ACTIVE_ID, null)
    }

    fun setActiveModelId(context: Context, id: String?) {
        prefs(context).edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    fun getActiveModelPath(context: Context): String? {
        val id = getActiveModelId(context) ?: return null
        return getModels(context).firstOrNull { it.id == id }?.path
    }

    fun getActiveModelName(context: Context): String? {
        val id = getActiveModelId(context) ?: return null
        return getModels(context).firstOrNull { it.id == id }?.name
    }
}
