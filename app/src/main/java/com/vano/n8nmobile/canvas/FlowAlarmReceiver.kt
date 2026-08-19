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
                val flowId = FlowScheduler.getScheduledFlowId(context)
                val flowState = flowId?.let { FlowStore.loadFlow(context, it) }
                if (flowState == null) {
                    AppLog.add("SCHEDULE", "Gak ada flow terjadwal yang aktif")
                } else {
                    flowId?.let { FlowRunStatsStore.recordRun(context, it) }
                    AppLog.add("SCHEDULE", "Menjalankan flow terjadwal: ${flowState.name}")
                    val nodes = flowState.nodes.map {
                        WorkflowNode(id = it.id, type = it.type, config = FlowScheduler.parseConfig(it.configText))
                    }
                    val edges = flowState.edges.map { WorkflowEdge(it.fromId, it.toId) }
                    val workflow = Workflow(nodes, edges)
                    val registry = NodeRegistry.default(context)
                    val engine = WorkflowExecutionEngine(registry)
                    val result = engine.run(workflow)
                    if (result.fatalError != null) {
                        AppLog.add("SCHEDULE_ERROR", result.fatalError)
                    } else {
                        AppLog.add("SCHEDULE", "Flow terjadwal selesai")
                    }
                }
            } catch (e: Exception) {
                AppLog.add("SCHEDULE_ERROR", e.message ?: "unknown")
            } finally {
                FlowScheduler.scheduleIfNeeded(context)
                pendingResult.finish()
            }
        }
    }
}
