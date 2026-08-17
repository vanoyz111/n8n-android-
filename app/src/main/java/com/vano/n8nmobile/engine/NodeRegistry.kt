package com.vano.n8nmobile.engine

import android.content.Context

class NodeRegistry {
    private val executors = mutableMapOf<String, NodeExecutor>()

    fun register(type: String, executor: NodeExecutor) {
        executors[type] = executor
    }

    fun get(type: String): NodeExecutor? = executors[type]

    companion object {
        fun default(context: Context): NodeRegistry {
            val registry = NodeRegistry()
            registry.register("manualTrigger", ManualTriggerExecutor())
            registry.register("setData", SetDataExecutor())
            registry.register("delay", DelayExecutor())
            registry.register("condition", ConditionExecutor())
            registry.register("httpRequest", HttpRequestExecutor())
            registry.register("notification", NotificationExecutor(context))
            registry.register("aiAgent", AiAgentExecutor(context))
            registry.register("scheduleTrigger", ScheduleTriggerExecutor())
            return registry
        }
    }
}
