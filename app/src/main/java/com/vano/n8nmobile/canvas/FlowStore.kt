package com.vano.n8nmobile.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FlowSummary(val id: String, val name: String, val updatedAt: Long)

object FlowStore {
    private const val PREFS_NAME = "n8n_mobile_flow"
    private const val KEY_FLOW_IDS = "flow_ids"
    private const val KEY_FLOW_PREFIX = "flow_"

    data class FlowState(
        val nodes: List<CanvasNode>,
        val edges: List<CanvasEdge>,
        val positions: Map<String, Offset>,
        val nextId: Int,
        val name: String
    )

    fun newId(): String = UUID.randomUUID().toString()

    fun listFlows(context: Context): List<FlowSummary> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idsRaw = prefs.getString(KEY_FLOW_IDS, null) ?: return emptyList()
        return try {
            val ids = JSONArray(idsRaw)
            (0 until ids.length()).mapNotNull { i ->
                val id = ids.getString(i)
                val raw = prefs.getString("$KEY_FLOW_PREFIX$id", null) ?: return@mapNotNull null
                val obj = JSONObject(raw)
                FlowSummary(id, obj.optString("name", "Flow"), obj.optLong("updatedAt", 0L))
            }.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadFlow(context: Context, id: String): FlowState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("$KEY_FLOW_PREFIX$id", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val nodesArr = obj.getJSONArray("nodes")
            val nodes = (0 until nodesArr.length()).map { i ->
                val n = nodesArr.getJSONObject(i)
                CanvasNode(n.getString("id"), n.getString("type"), n.optString("configText", ""))
            }
            val edgesArr = obj.getJSONArray("edges")
            val edges = (0 until edgesArr.length()).map { i ->
                val e = edgesArr.getJSONObject(i)
                CanvasEdge(e.getString("fromId"), e.getString("toId"))
            }
            val posObj = obj.getJSONObject("positions")
            val positions = mutableMapOf<String, Offset>()
            posObj.keys().forEach { key ->
                val p = posObj.getJSONObject(key)
                positions[key] = Offset(p.getDouble("x").toFloat(), p.getDouble("y").toFloat())
            }
            FlowState(nodes, edges, positions, obj.optInt("nextId", 1), obj.optString("name", "Flow"))
        } catch (e: Exception) {
            null
        }
    }

    fun save(
        context: Context,
        id: String,
        name: String,
        nodes: List<CanvasNode>,
        edges: List<CanvasEdge>,
        positions: Map<String, Offset>,
        nextId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val nodesArray = JSONArray()
        nodes.forEach { n ->
            nodesArray.put(JSONObject().apply {
                put("id", n.id); put("type", n.type); put("configText", n.configText)
            })
        }
        val edgesArray = JSONArray()
        edges.forEach { e ->
            edgesArray.put(JSONObject().apply { put("fromId", e.fromId); put("toId", e.toId) })
        }
        val positionsObj = JSONObject()
        positions.forEach { (pid, offset) ->
            positionsObj.put(pid, JSONObject().apply {
                put("x", offset.x.toDouble()); put("y", offset.y.toDouble())
            })
        }

        val flowObj = JSONObject().apply {
            put("name", name)
            put("nodes", nodesArray)
            put("edges", edgesArray)
            put("positions", positionsObj)
            put("nextId", nextId)
            put("updatedAt", System.currentTimeMillis())
        }
        prefs.edit().putString("$KEY_FLOW_PREFIX$id", flowObj.toString()).apply()

        val existingIds = listFlows(context).map { it.id }.toMutableSet()
        existingIds.add(id)
        prefs.edit().putString(KEY_FLOW_IDS, JSONArray(existingIds.toList()).toString()).apply()
    }

    fun delete(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_FLOW_PREFIX$id").apply()
        val remainingIds = listFlows(context).map { it.id }.filterNot { it == id }
        prefs.edit().putString(KEY_FLOW_IDS, JSONArray(remainingIds).toString()).apply()
    }
}
