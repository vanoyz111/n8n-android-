package com.vano.n8nmobile.engine

import com.vano.n8nmobile.model.Workflow

data class NodeExecutionLog(
    val nodeId: String,
    val nodeType: String,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val outputSummary: String = ""
)

data class ExecutionResult(
    val outputs: Map<String, List<Map<String, Any?>>>,
    val log: List<NodeExecutionLog>,
    val fatalError: String? = null
)

class WorkflowExecutionEngine(private val registry: NodeRegistry) {

    suspend fun run(workflow: Workflow): ExecutionResult {
        val order = try {
            topologicalSort(workflow)
        } catch (e: Exception) {
            return ExecutionResult(emptyMap(), emptyList(), e.message ?: "Gagal urutan eksekusi")
        }

        val outputs = mutableMapOf<String, List<Map<String, Any?>>>()
        val log = mutableListOf<NodeExecutionLog>()

        for (nodeId in order) {
            val node = workflow.nodes.first { it.id == nodeId }
            val incomingEdges = workflow.edges.filter { it.toNodeId == nodeId }
            val input: List<Map<String, Any?>> = if (incomingEdges.isEmpty()) {
                emptyList()
            } else {
                incomingEdges.flatMap { outputs[it.fromNodeId] ?: emptyList() }
            }
            val executor = registry.get(node.type)
            val startTime = System.currentTimeMillis()

            if (executor == null) {
                val duration = System.currentTimeMillis() - startTime
                val message = "Node type tidak dikenal: ${node.type}"
                log.add(NodeExecutionLog(node.id, node.type, duration, false, message))
                return ExecutionResult(outputs, log, message)
            }

            try {
                val result = executor.execute(node, input)
                outputs[node.id] = result
                val duration = System.currentTimeMillis() - startTime
                log.add(NodeExecutionLog(node.id, node.type, duration, true, null, result.toString().take(150)))
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                log.add(NodeExecutionLog(node.id, node.type, duration, false, e.message ?: "Error tidak diketahui"))
                return ExecutionResult(outputs, log, e.message ?: "Error tidak diketahui saat menjalankan node ${node.type}")
            }
        }
        return ExecutionResult(outputs, log, null)
    }

    private fun topologicalSort(workflow: Workflow): List<String> {
        val nodeIds = workflow.nodes.map { it.id }
        val inDegree = nodeIds.associateWith { id -> workflow.edges.count { it.toNodeId == id } }.toMutableMap()
        val adjacency = nodeIds.associateWith { id -> workflow.edges.filter { it.fromNodeId == id }.map { it.toNodeId } }
        val queue = ArrayDeque(nodeIds.filter { inDegree[it] == 0 })
        val result = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            adjacency[current]?.forEach { next ->
                inDegree[next] = (inDegree[next] ?: 0) - 1
                if (inDegree[next] == 0) queue.add(next)
            }
        }

        if (result.size != nodeIds.size) {
            throw IllegalStateException("Workflow punya cycle (loop tak berujung), gak bisa dieksekusi")
        }
        return result
    }
}
