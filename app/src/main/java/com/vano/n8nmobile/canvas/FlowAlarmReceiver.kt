package com.vano.n8nmobile.canvas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vano.n8nmobile.engine.NodeRegistry
import com.vano.n8nmobile.engine.WorkflowExecutionEngine
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.model.Workflow
import com.vano.n8nmobile.model.WorkflowEdge
import com.vano.n8nmobile.model.WorkflowNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppLog.add("SCHEDULE", "Menjalankan flow terjadwal...")
                val flowState = FlowStore.load(context)
                val nodes = flowState.nodes.map {
                    WorkflowNode(id = it.id, type = it.type, config = FlowScheduler.parseConfig(it.configText))
                }
                val edges = flowState.edges.map { WorkflowEdge(it.fromId, it.toId) }
                val workflow = Workflow(nodes, edges)
                val registry = NodeRegistry.default(context)
                val engine = WorkflowExecutionEngine(registry)
                engine.run(workflow)
                AppLog.add("SCHEDULE", "Flow terjadwal selesai")
            } catch (e: Exception) {
                AppLog.add("SCHEDULE_ERROR", e.message ?: "unknown")
            } finally {
                FlowScheduler.scheduleIfNeeded(context)
                pendingResult.finish()
            }
        }
    }
}
