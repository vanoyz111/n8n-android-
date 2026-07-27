package com.vano.n8nmobile.engine

import com.vano.n8nmobile.model.WorkflowNode

interface NodeExecutor {
    suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>>
}
