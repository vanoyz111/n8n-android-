package com.vano.n8nmobile.canvas

data class CanvasNode(
    val id: String,
    val type: String,
    val configText: String
)

data class CanvasEdge(
    val fromId: String,
    val toId: String
)

data class NodeTypeInfo(
    val type: String,
    val label: String,
    val defaultConfig: String
)

val availableNodeTypes = listOf(
    NodeTypeInfo("manualTrigger", "Manual Trigger", ""),
    NodeTypeInfo("httpRequest", "HTTP Request", "url=https://jsonplaceholder.typicode.com/todos/1\nmethod=GET"),
    NodeTypeInfo("setData", "Set Data", "pesan=halo dari node ini"),
    NodeTypeInfo("delay", "Delay", "ms=1000"),
    NodeTypeInfo("condition", "Condition", "key=httpStatus\nequals=200"),
    NodeTypeInfo("notification", "Notification", "title=Workflow\ntext=Selesai")
)
