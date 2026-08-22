package com.vano.n8nmobile.docchat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StoredDocument(val id: String, val name: String, val path: String, val chunkCount: Int)

object DocumentStore {
    private const val PREFS_NAME = "n8n_mobile_docchat"
    private const val KEY_DOCS = "docs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDocuments(context: Context): List<StoredDocument> {
        val raw = prefs(context).getString(KEY_DOCS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val doc = StoredDocument(obj.getString("id"), obj.getString("name"), obj.getString("path"), obj.optInt("chunkCount", 0))
                if (File(doc.path).exists()) doc else null
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun setDocuments(context: Context, docs: List<StoredDocument>) {
        val array = JSONArray()
        docs.forEach { d -> array.put(JSONObject().apply { put("id", d.id); put("name", d.name); put("path", d.path); put("chunkCount", d.chunkCount) }) }
        prefs(context).edit().putString(KEY_DOCS, array.toString()).apply()
    }

    fun addDocument(context: Context, name: String, content: String): StoredDocument {
        val id = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "docchat")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$id.txt")
        file.writeText(content)
        val chunkCount = DocumentChunker.chunk(content).size
        val doc = StoredDocument(id, name, file.absolutePath, chunkCount)
        setDocuments(context, getDocuments(context) + doc)
        return doc
    }

    fun removeDocument(context: Context, id: String) {
        val docs = getDocuments(context)
        docs.firstOrNull { it.id == id }?.let { try { File(it.path).delete() } catch (e: Exception) { } }
        setDocuments(context, docs.filterNot { it.id == id })
    }

    fun getContent(doc: StoredDocument): String = try { File(doc.path).readText() } catch (e: Exception) { "" }
}
