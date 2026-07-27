package com.vano.n8nmobile.engine

import com.vano.n8nmobile.model.WorkflowNode
import kotlinx.coroutines.delay

class ManualTriggerExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        return listOf(emptyMap())
    }
}

class SetDataExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val base = if (input.isEmpty()) listOf(emptyMap()) else input
        return base.map { item -> item + node.config }
    }
}

class DelayExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val ms = node.config["ms"]?.toLongOrNull() ?: 1000L
        delay(ms)
        return input
    }
}

class ConditionExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val key = node.config["key"] ?: return input
        val expected = node.config["equals"]
        return input.filter { it[key]?.toString() == expected }
    }
}
