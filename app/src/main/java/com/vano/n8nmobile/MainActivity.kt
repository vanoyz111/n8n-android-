package com.vano.n8nmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.engine.NodeRegistry
import com.vano.n8nmobile.engine.WorkflowExecutionEngine
import com.vano.n8nmobile.model.Workflow
import com.vano.n8nmobile.model.WorkflowEdge
import com.vano.n8nmobile.model.WorkflowNode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var resultText by remember { mutableStateOf("Menjalankan workflow test...") }

                    LaunchedEffect(Unit) {
                        val workflow = sampleWorkflow()
                        val engine = WorkflowExecutionEngine(NodeRegistry.default())
                        val outputs = engine.run(workflow)
                        resultText = outputs.entries.joinToString("\n\n") { (nodeId, items) ->
                            "Node: $nodeId\nOutput: $items"
                        }
                    }

                    Text(resultText)
                }
            }
        }
    }
}

private fun sampleWorkflow(): Workflow {
    val nodes = listOf(
        WorkflowNode(id = "1", type = "manualTrigger"),
        WorkflowNode(id = "2", type = "setData", config = mapOf("pesan" to "halo dari node setData")),
        WorkflowNode(id = "3", type = "delay", config = mapOf("ms" to "500"))
    )
    val edges = listOf(
        WorkflowEdge(fromNodeId = "1", toNodeId = "2"),
        WorkflowEdge(fromNodeId = "2", toNodeId = "3")
    )
    return Workflow(nodes, edges)
}
