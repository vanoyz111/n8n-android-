package com.vano.n8nmobile.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

object FlowStore {
    private const val PREFS_NAME = "n8n_mobile_flow"
    private const val KEY_NODES = "nodes"
    private const val KEY_EDGES = "edges"
    private const val KEY_POSITIONS = "positions"
    private const val KEY_NEXT_ID = "next_id"

    data class FlowState(
        val nodes: List<CanvasNode>,
        val edges: List<CanvasEdge>,
        val positions: Map<String, Offset>,
        val nextId: Int
    )

    fun load(context: Context): FlowState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val nodes = try {
            val array = JSONArray(prefs.getString(KEY_NODES, "[]"))
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CanvasNode(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    configText = obj.optString("configText", "")
                )
            }
        } catch (e: Exception) { emptyList() }

        val edges = try {
            val array = JSONArray(prefs.getString(KEY_EDGES, "[]"))
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CanvasEdge(fromId = obj.getString("fromId"), toId = obj.getString("toId"))
            }
        } catch (e: Exception) { emptyList() }

        val positions = try {
            val obj = JSONObject(prefs.getString(KEY_POSITIONS, "{}"))
            val map = mutableMapOf<String, Offset>()
            obj.keys().forEach { key ->
                val posObj = obj.getJSONObject(key)
                map[key] = Offset(posObj.getDouble("x").toFloat(), posObj.getDouble("y").toFloat())
            }
            map
        } catch (e: Exception) { emptyMap() }

        val nextId = prefs.getInt(KEY_NEXT_ID, 1)
        return FlowState(nodes, edges, positions, nextId)
    }

    fun save(
        context: Context,
        nodes: List<CanvasNode>,
        edges: List<CanvasEdge>,
        positions: Map<String, Offset>,
        nextId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val nodesArray = JSONArray()
        nodes.forEach { n ->
            nodesArray.put(JSONObject().apply {
                put("id", n.id)
                put("type", n.type)
                put("configText", n.configText)
            })
        }

        val edgesArray = JSONArray()
        edges.forEach { e ->
            edgesArray.put(JSONObject().apply {
                put("fromId", e.fromId)
                put("toId", e.toId)
            })
        }

        val positionsObj = JSONObject()
        positions.forEach { (id, offset) ->
            positionsObj.put(id, JSONObject().apply {
                put("x", offset.x.toDouble())
                put("y", offset.y.toDouble())
            })
        }

        prefs.edit()
            .putString(KEY_NODES, nodesArray.toString())
            .putString(KEY_EDGES, edgesArray.toString())
            .putString(KEY_POSITIONS, positionsObj.toString())
            .putInt(KEY_NEXT_ID, nextId)
            .apply()
    }
}
