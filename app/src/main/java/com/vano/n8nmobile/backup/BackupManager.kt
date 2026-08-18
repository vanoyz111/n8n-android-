package com.vano.n8nmobile.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    private val prefsFiles = listOf(
        "n8n_mobile_chat",
        "n8n_mobile_flow",
        "n8n_mobile_settings",
        "n8n_mobile_autoreply",
        "n8n_mobile_local_ai",
        "n8n_mobile_litert",
        "n8n_mobile_local_ai_settings",
        "n8n_mobile_imagegen",
        "n8n_mobile_ai_profiles",
        "n8n_mobile_chat_mode",
        "n8n_mobile_theme",
        "n8n_mobile_local_server",
        "n8n_mobile_applock"
    )

    fun exportAll(context: Context): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val filesObj = JSONObject()
        prefsFiles.forEach { fileName ->
            val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            val fileObj = JSONObject()
            prefs.all.forEach { (key, value) ->
                val entry = JSONObject()
                when (value) {
                    is String -> { entry.put("type", "string"); entry.put("value", value) }
                    is Int -> { entry.put("type", "int"); entry.put("value", value) }
                    is Long -> { entry.put("type", "long"); entry.put("value", value) }
                    is Float -> { entry.put("type", "float"); entry.put("value", value.toDouble()) }
                    is Boolean -> { entry.put("type", "boolean"); entry.put("value", value) }
                    is Set<*> -> {
                        entry.put("type", "stringSet")
                        val arr = JSONArray()
                        value.forEach { arr.put(it.toString()) }
                        entry.put("value", arr)
                    }
                    else -> return@forEach
                }
                fileObj.put(key, entry)
            }
            filesObj.put(fileName, fileObj)
        }
        root.put("prefs", filesObj)
        return root.toString()
    }

    fun importAll(context: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val filesObj = root.getJSONObject("prefs")
            prefsFiles.forEach { fileName ->
                if (!filesObj.has(fileName)) return@forEach
                val fileObj = filesObj.getJSONObject(fileName)
                val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.clear()
                fileObj.keys().forEach { key ->
                    val entry = fileObj.getJSONObject(key)
                    when (entry.getString("type")) {
                        "string" -> editor.putString(key, entry.getString("value"))
                        "int" -> editor.putInt(key, entry.getInt("value"))
                        "long" -> editor.putLong(key, entry.getLong("value"))
                        "float" -> editor.putFloat(key, entry.getDouble("value").toFloat())
                        "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                        "stringSet" -> {
                            val arr = entry.getJSONArray("value")
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) set.add(arr.getString(i))
                            editor.putStringSet(key, set)
                        }
                    }
                }
                editor.apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
