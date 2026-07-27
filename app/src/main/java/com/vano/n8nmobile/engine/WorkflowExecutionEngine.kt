package com.vano.n8nmobile.engine

import com.vano.n8nmobile.model.Workflow

class WorkflowExecutionEngine(private val registry: NodeRegistry) {

    suspend fun run(workflow: Workflow): Map<String, List<Map<String, Any?>>> {
        val order = topologicalSort(workflow)
        val outputs = mutableMapOf<String, List<Map<String, Any?>>>()

        for (nodeId in order) {
            val node = workflow.nodes.first { it.id == nodeId }
            val incomingEdges = workflow.edges.filter { it.toNodeId == nodeId }
            val input: List<Map<String, Any?>> = if (incomingEdges.isEmpty()) {
                emptyList()
            } else {
                incomingEdges.flatMap { outputs[it.fromNodeId] ?: emptyList() }
            }
            val executor = registry.get(node.type)
                ?: throw IllegalStateException("Node type tidak dikenal: ${node.type}")
            outputs[node.id] = executor.execute(node, input)
        }
        return outputs
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
