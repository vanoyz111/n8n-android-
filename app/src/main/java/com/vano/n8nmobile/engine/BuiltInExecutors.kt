package com.vano.n8nmobile.engine

import android.content.Context
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
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

class AiAgentExecutor(private val context: Context) : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val template = node.config["prompt"] ?: ""
        val mode = node.config["mode"]?.ifBlank { "auto" } ?: "auto"
        val items = if (input.isEmpty()) listOf(emptyMap()) else input
        return items.map { item ->
            val resolvedPrompt = resolvePrompt(template, item)
            val reply = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", resolvedPrompt)), mode)
            item + mapOf("aiResponse" to reply)
        }
    }

    private fun resolvePrompt(template: String, item: Map<String, Any?>): String {
        var result = template
        item.forEach { (key, value) ->
            result = result.replace("\$$key", value?.toString() ?: "")
        }
        return result
    }
}

class ScheduleTriggerExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        return listOf(emptyMap())
    }
}
