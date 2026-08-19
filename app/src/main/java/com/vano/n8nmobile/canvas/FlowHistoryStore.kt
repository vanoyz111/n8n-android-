package com.vano.n8nmobile.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

object FlowHistoryStore {
    private const val PREFS_NAME = "n8n_mobile_flow_history"
    private const val MAX_HISTORY = 15

    private fun key(flowId: String) = "history_$flowId"

    fun pushSnapshot(context: Context, flowId: String, nodes: List<CanvasNode>, edges: List<CanvasEdge>, positions: Map<String, Offset>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingRaw = prefs.getString(key(flowId), null)
        val array = if (existingRaw != null) try { JSONArray(existingRaw) } catch (e: Exception) { JSONArray() } else JSONArray()

        val snapshot = JSONObject().apply {
            put("nodes", JSONArray().apply { nodes.forEach { n -> put(JSONObject().apply { put("id", n.id); put("type", n.type); put("configText", n.configText) }) } })
            put("edges", JSONArray().apply { edges.forEach { e -> put(JSONObject().apply { put("fromId", e.fromId); put("toId", e.toId) }) } })
            put("positions", JSONObject().apply { positions.forEach { (id, off) -> put(id, JSONObject().apply { put("x", off.x.toDouble()); put("y", off.y.toDouble()) }) } })
        }

        val newArray = JSONArray()
        val startIdx = if (array.length() >= MAX_HISTORY) array.length() - MAX_HISTORY + 1 else 0
        for (i in startIdx until array.length()) newArray.put(array.getJSONObject(i))
        newArray.put(snapshot)

        prefs.edit().putString(key(flowId), newArray.toString()).apply()
    }

    fun hasHistory(context: Context, flowId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key(flowId), null) ?: return false
        return try { JSONArray(raw).length() > 0 } catch (e: Exception) { false }
    }

    data class Snapshot(val nodes: List<CanvasNode>, val edges: List<CanvasEdge>, val positions: Map<String, Offset>)

    fun popSnapshot(context: Context, flowId: String): Snapshot? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key(flowId), null) ?: return null
        val array = try { JSONArray(raw) } catch (e: Exception) { return null }
        if (array.length() == 0) return null

        val lastIdx = array.length() - 1
        val snapshotObj = array.getJSONObject(lastIdx)

        val nodesArr = snapshotObj.getJSONArray("nodes")
        val nodes = (0 until nodesArr.length()).map { i ->
            val n = nodesArr.getJSONObject(i)
            CanvasNode(n.getString("id"), n.getString("type"), n.optString("configText", ""))
        }
        val edgesArr = snapshotObj.getJSONArray("edges")
        val edges = (0 until edgesArr.length()).map { i ->
            val e = edgesArr.getJSONObject(i)
            CanvasEdge(e.getString("fromId"), e.getString("toId"))
        }
        val posObj = snapshotObj.getJSONObject("positions")
        val positions = mutableMapOf<String, Offset>()
        posObj.keys().forEach { k -> val p = posObj.getJSONObject(k); positions[k] = Offset(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()) }

        val newArray = JSONArray()
        for (i in 0 until lastIdx) newArray.put(array.getJSONObject(i))
        prefs.edit().putString(key(flowId), newArray.toString()).apply()

        return Snapshot(nodes, edges, positions)
    }
}
