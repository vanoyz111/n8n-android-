package com.vano.n8nmobile.engine

class NodeRegistry {
    private val executors = mutableMapOf<String, NodeExecutor>()

    fun register(type: String, executor: NodeExecutor) {
        executors[type] = executor
    }

    fun get(type: String): NodeExecutor? = executors[type]

    companion object {
        fun default(): NodeRegistry {
            val registry = NodeRegistry()
            registry.register("manualTrigger", ManualTriggerExecutor())
            registry.register("setData", SetDataExecutor())
            registry.register("delay", DelayExecutor())
            registry.register("condition", ConditionExecutor())
            return registry
        }
    }
}
