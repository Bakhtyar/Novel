package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CanvasNodeEntity
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: StoryViewModel,
    onBack: () -> Unit
) {
    val lang = viewModel.language
    val nodes by viewModel.nodes.collectAsState()
    val allProjectNodes by viewModel.allProjectNodes.collectAsState()
    val connections by viewModel.connections.collectAsState()

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var zoomScale by remember { mutableStateOf(1f) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newNodeTitle by remember { mutableStateOf("") }
    var newNodeContent by remember { mutableStateOf("") }
    var newNodeType by remember { mutableStateOf("Event") }

    val isSubCanvas = viewModel.currentParentNodeId > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSubCanvas) "${viewModel.currentSubNodeTitle} (Sub-space)" else Strings.get("app_title", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (isSubCanvas) {
                            Text(
                                text = "Nested Canvas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSubCanvas) viewModel.closeSubCanvas()
                            else onBack()
                        },
                        modifier = Modifier.testTag("canvas_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.get("back", lang)
                        )
                    }
                },
                actions = {
                    // Minimap toggle
                    IconButton(onClick = { viewModel.isMinimapOpen = true }) {
                        Icon(Icons.Default.Map, contentDescription = Strings.get("minimap", lang))
                    }
                    // Zoom In
                    IconButton(onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    // Zoom Out
                    IconButton(onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_node_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.get("add_node", lang))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            // Giant infinite canvas space (5000x5000 virtual area)
            Box(
                modifier = Modifier
                    .size(5000.dp)
                    .offset(x = offsetX.dp, y = offsetY.dp)
            ) {
                // Draw connection lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nodeMap = allProjectNodes.associateBy { it.id }
                    for (conn in connections) {
                        val fromNode = nodeMap[conn.fromNodeId]
                        val toNode = nodeMap[conn.toNodeId]
                        if (fromNode != null && toNode != null) {
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.6f),
                                start = Offset(fromNode.xPos + 110f, fromNode.yPos + 70f),
                                end = Offset(toNode.xPos + 110f, toNode.yPos + 70f),
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                // Render nodes
                for (node in nodes) {
                    StoryNodeCard(
                        node = node,
                        lang = lang,
                        onDrag = { dx, dy ->
                            viewModel.updateNodePosition(node.id, node.xPos + dx, node.yPos + dy)
                        },
                        onClick = {
                            viewModel.selectedNodeForDetail = node
                        },
                        onConnectClick = {
                            if (viewModel.connectingSourceNodeId == null) {
                                viewModel.connectingSourceNodeId = node.id
                            } else {
                                viewModel.addConnection(viewModel.connectingSourceNodeId!!, node.id)
                                viewModel.connectingSourceNodeId = null
                            }
                        },
                        isConnecting = viewModel.connectingSourceNodeId == node.id
                    )
                }
            }

            // Connecting status banner
            if (viewModel.connectingSourceNodeId != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(Strings.get("select_connect_target", lang))
                        TextButton(onClick = { viewModel.connectingSourceNodeId = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Add Node Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(Strings.get("add_node", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newNodeTitle,
                        onValueChange = { newNodeTitle = it },
                        label = { Text(Strings.get("node_title", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNodeContent,
                        onValueChange = { newNodeContent = it },
                        label = { Text(Strings.get("node_content", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNodeTitle.isNotBlank()) {
                            viewModel.addNode(
                                title = newNodeTitle,
                                content = newNodeContent,
                                nodeType = newNodeType,
                                colorHex = "#3B82F6",
                                x = 200f - offsetX,
                                y = 200f - offsetY
                            )
                            newNodeTitle = ""
                            newNodeContent = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(Strings.get("create", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    // Node Detail Modal
    viewModel.selectedNodeForDetail?.let { selectedNode ->
        NodeDetailModal(
            viewModel = viewModel,
            node = selectedNode,
            onDismiss = { viewModel.selectedNodeForDetail = null },
            onOpenSubCanvas = { nodeId, title ->
                viewModel.openSubCanvas(nodeId, title)
            }
        )
    }

    // Minimap Dialog
    if (viewModel.isMinimapOpen) {
        MinimapDialog(
            lang = lang,
            nodes = allProjectNodes,
            onDismiss = { viewModel.isMinimapOpen = false }
        )
    }
}

@Composable
fun StoryNodeCard(
    node: CanvasNodeEntity,
    lang: String,
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onConnectClick: () -> Unit,
    isConnecting: Boolean
) {
    val cardColor = Color(android.graphics.Color.parseColor(node.colorHex))

    Box(
        modifier = Modifier
            .offset(x = node.xPos.dp, y = node.yPos.dp)
            .width(220.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnecting) MaterialTheme.colorScheme.tertiaryContainer else cardColor.copy(alpha = 0.85f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = onConnectClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2
                )
            }
        }
    }
}
