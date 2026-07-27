package com.vano.n8nmobile.model

data class WorkflowNode(
    val id: String,
    val type: String,
    val name: String = type,
    val config: Map<String, String> = emptyMap()
)

data class WorkflowEdge(
    val fromNodeId: String,
    val toNodeId: String
)

data class Workflow(
    val nodes: List<WorkflowNode>,
    val edges: List<WorkflowEdge>
)
