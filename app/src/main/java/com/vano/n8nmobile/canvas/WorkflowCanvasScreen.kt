package com.vano.n8nmobile.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vano.n8nmobile.engine.ExecutionResult
import com.vano.n8nmobile.engine.NodeRegistry
import com.vano.n8nmobile.engine.WorkflowExecutionEngine
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.model.Workflow
import com.vano.n8nmobile.model.WorkflowEdge
import com.vano.n8nmobile.model.WorkflowNode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val NODE_WIDTH = 170.dp
private val NODE_HEIGHT = 64.dp

@Composable
fun WorkflowCanvasScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val nodeWidthPx = with(density) { NODE_WIDTH.toPx() }
    val nodeHeightPx = with(density) { NODE_HEIGHT.toPx() }

    var flowsList by remember { mutableStateOf(FlowStore.listFlows(context)) }
    var currentFlowId by remember { mutableStateOf(flowsList.firstOrNull()?.id ?: FlowStore.newId()) }
    var flowName by remember { mutableStateOf("Flow Baru") }

    val nodes = remember { mutableStateListOf<CanvasNode>() }
    val edges = remember { mutableStateListOf<CanvasEdge>() }
    val positions = remember { mutableStateMapOf<String, Offset>() }
    var nextId by remember { mutableStateOf(1) }

    fun loadFlow(id: String) {
        val state = FlowStore.loadFlow(context, id)
        nodes.clear(); if (state != null) nodes.addAll(state.nodes)
        edges.clear(); if (state != null) edges.addAll(state.edges)
        positions.clear(); if (state != null) positions.putAll(state.positions)
        nextId = state?.nextId ?: 1
        flowName = state?.name ?: "Flow Baru"
        currentFlowId = id
    }

    LaunchedEffect(Unit) {
        if (flowsList.isEmpty()) {
            FlowStore.save(context, currentFlowId, "Flow Baru", emptyList(), emptyList(), emptyMap(), 1)
            flowsList = FlowStore.listFlows(context)
            flowName = "Flow Baru"
        } else {
            loadFlow(currentFlowId)
        }
    }

    var connectSourceId by remember { mutableStateOf<String?>(null) }
    var editingNode by remember { mutableStateOf<CanvasNode?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showFlowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var runResult by remember { mutableStateOf<ExecutionResult?>(null) }

    fun persist() {
        val oldState = FlowStore.loadFlow(context, currentFlowId)
        if (oldState != null) {
            FlowHistoryStore.pushSnapshot(context, currentFlowId, oldState.nodes, oldState.edges, oldState.positions)
        }
        FlowStore.save(context, currentFlowId, flowName, nodes, edges, positions, nextId)
        FlowScheduler.onFlowSaved(context, currentFlowId, nodes)
        flowsList = FlowStore.listFlows(context)
    }

    fun undo() {
        val snapshot = FlowHistoryStore.popSnapshot(context, currentFlowId) ?: return
        nodes.clear(); nodes.addAll(snapshot.nodes)
        edges.clear(); edges.addAll(snapshot.edges)
        positions.clear(); positions.putAll(snapshot.positions)
        FlowStore.save(context, currentFlowId, flowName, nodes, edges, positions, nextId)
        FlowScheduler.onFlowSaved(context, currentFlowId, nodes)
        flowsList = FlowStore.listFlows(context)
    }

    fun addNode(info: NodeTypeInfo) {
        val id = "n$nextId"
        nextId += 1
        nodes.add(CanvasNode(id, info.type, info.defaultConfig))
        val index = nodes.size - 1
        val col = index % 2
        val row = index / 2
        positions[id] = Offset(40f + col * (nodeWidthPx + 40f), 40f + row * (nodeHeightPx + 40f))
        persist()
    }

    fun deleteNode(id: String) {
        nodes.removeAll { it.id == id }
        positions.remove(id)
        edges.removeAll { it.fromId == id || it.toId == id }
        if (connectSourceId == id) connectSourceId = null
        persist()
    }

    fun runWorkflow() {
        FlowRunStatsStore.recordRun(context, currentFlowId)
        AppLog.add("FLOW", "Menjalankan workflow \"$flowName\" (${nodes.size} node)")
        val workflowNodes = nodes.map { n -> WorkflowNode(id = n.id, type = n.type, config = parseConfigText(n.configText)) }
        val workflowEdges = edges.map { WorkflowEdge(it.fromId, it.toId) }
        val workflow = Workflow(workflowNodes, workflowEdges)
        val registry = NodeRegistry.default(context)
        val engine = WorkflowExecutionEngine(registry)
        scope.launch {
            val result = engine.run(workflow)
            runResult = result
            if (result.fatalError == null) {
                AppLog.add("FLOW", "Workflow selesai")
            } else {
                AppLog.add("FLOW_ERROR", result.fatalError)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            Box {
                Row(
                    modifier = Modifier.clickable { showFlowMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(flowName, style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = showFlowMenu, onDismissRequest = { showFlowMenu = false }) {
                    flowsList.forEach { summary ->
                        DropdownMenuItem(
                            text = { Text(summary.name) },
                            onClick = {
                                loadFlow(summary.id)
                                showFlowMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ Flow Baru") },
                        onClick = {
                            val newId = FlowStore.newId()
                            FlowStore.save(context, newId, "Flow Baru", emptyList(), emptyList(), emptyMap(), 1)
                            flowsList = FlowStore.listFlows(context)
                            loadFlow(newId)
                            showFlowMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplikat Flow Ini") },
                        onClick = {
                            val newId = FlowStore.newId()
                            FlowStore.save(context, newId, "$flowName (salinan)", nodes.toList(), edges.toList(), positions.toMap(), nextId)
                            flowsList = FlowStore.listFlows(context)
                            loadFlow(newId)
                            showFlowMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ganti Nama") },
                        onClick = {
                            showFlowMenu = false
                            showRenameDialog = true
                        }
                    )
                    if (flowsList.size > 1) {
                        DropdownMenuItem(
                            text = { Text("Hapus Flow Ini", color = Color.Red) },
                            onClick = {
                                FlowStore.delete(context, currentFlowId)
                                flowsList = FlowStore.listFlows(context)
                                loadFlow(flowsList.first().id)
                                showFlowMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (connectSourceId != null) {
            Text(
                "Mode sambung aktif — tap 🔗 di node tujuan",
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color(0xFFFF7043)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF0F0F0))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                edges.forEach { edge ->
                    val from = positions[edge.fromId] ?: return@forEach
                    val to = positions[edge.toId] ?: return@forEach
                    val start = Offset(from.x + nodeWidthPx / 2, from.y + nodeHeightPx)
                    val end = Offset(to.x + nodeWidthPx / 2, to.y)
                    drawLine(color = Color(0xFF5C6BC0), start = start, end = end, strokeWidth = 5f, cap = StrokeCap.Round)
                    drawCircle(color = Color(0xFF5C6BC0), radius = 10f, center = end)
                }
            }

            nodes.forEach { node ->
                val pos = positions[node.id] ?: Offset.Zero
                val isConnectSource = connectSourceId == node.id

                var cardModifier = Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .width(NODE_WIDTH)
                    .height(NODE_HEIGHT)
                    .pointerInput(node.id) {
                        detectDragGestures(
                            onDragEnd = { persist() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val current = positions[node.id] ?: Offset.Zero
                                positions[node.id] = current + dragAmount
                            }
                        )
                    }
                if (isConnectSource) {
                    cardModifier = cardModifier.border(2.dp, Color(0xFFFF7043), RoundedCornerShape(12.dp))
                }

                Card(
                    modifier = cardModifier,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.type, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color.Black)
                            Text(node.id, fontSize = 11.sp, color = Color(0xFF555555))
                        }
                        Row {
                            IconButton(onClick = { editingNode = node }, modifier = Modifier.size(28.dp)) {
                                Text("✎", fontSize = 14.sp, color = Color.Black)
                            }
                            IconButton(
                                onClick = {
                                    connectSourceId = when {
                                        connectSourceId == node.id -> null
                                        connectSourceId == null -> node.id
                                        else -> {
                                            val sourceId = connectSourceId!!
                                            val exists = edges.any { it.fromId == sourceId && it.toId == node.id }
                                            if (sourceId != node.id && !exists) {
                                                edges.add(CanvasEdge(sourceId, node.id))
                                                persist()
                                            }
                                            null
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("🔗", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Button(onClick = { showAddMenu = true }) {
                    Text("+ Node")
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    availableNodeTypes.forEach { info ->
                        DropdownMenuItem(
                            text = { Text(info.label) },
                            onClick = {
                                addNode(info)
                                showAddMenu = false
                            }
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = { undo() }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                Button(onClick = { runWorkflow() }) {
                    Text("▶ Jalankan")
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(flowName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ganti Nama Flow") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    flowName = newName
                    persist()
                    showRenameDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
            }
        )
    }

    editingNode?.let { node ->
        var text by remember(node.id) { mutableStateOf(node.configText) }
        AlertDialog(
            onDismissRequest = { editingNode = null },
            title = { Text("Edit ${node.type}") },
            text = {
                Column {
                    Text("Format: key=value, satu baris per key", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val index = nodes.indexOfFirst { it.id == node.id }
                    if (index >= 0) nodes[index] = nodes[index].copy(configText = text)
                    persist()
                    editingNode = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteNode(node.id)
                    editingNode = null
                }) { Text("Hapus Node", color = Color.Red) }
            }
        )
    }

    runResult?.let { result ->
        AlertDialog(
            onDismissRequest = { runResult = null },
            title = { Text(if (result.fatalError != null) "Workflow Gagal" else "Hasil Workflow") },
            text = {
                Box(modifier = Modifier.height(360.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        result.log.forEach { entry ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(if (entry.success) "✅" else "❌", modifier = Modifier.padding(end = 6.dp))
                                Column {
                                    Text(
                                        "${entry.nodeType} (${entry.nodeId}) — ${entry.durationMs}ms",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (entry.success) {
                                        Text(entry.outputSummary, fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        Text(entry.errorMessage ?: "Error", fontSize = 12.sp, color = Color.Red)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        result.fatalError?.let {
                            Text("Gagal: $it", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { runResult = null }) { Text("Tutup") }
            }
        )
    }
}

private fun parseConfigText(text: String): Map<String, String> {
    return text.lines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed.contains("=")) return@mapNotNull null
            val idx = trimmed.indexOf("=")
            val key = trimmed.substring(0, idx).trim()
            val value = trimmed.substring(idx + 1).trim()
            if (key.isEmpty()) null else key to value
        }
        .toMap()
}
