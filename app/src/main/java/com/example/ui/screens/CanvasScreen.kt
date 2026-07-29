package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffectManager
import com.example.data.CanvasNodeEntity
import com.example.ui.components.AmbientGlowBackground
import com.example.ui.components.MusicPlayerSheet
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: StoryViewModel,
    onBack: () -> Unit
) {
    val lang = viewModel.language
    val clipboardManager = LocalClipboardManager.current

    // If full document note editor is active
    val activeDocNode = viewModel.selectedDocumentNode
    if (activeDocNode != null) {
        FullDocumentEditorScreen(
            viewModel = viewModel,
            node = activeDocNode,
            onBackToCanvas = { viewModel.closeDocumentEditor() }
        )
        return
    }

    // If presentation mode is active
    val allProjectNodes by viewModel.allProjectNodes.collectAsState()
    if (viewModel.isPresentationMode) {
        PresentationModeScreen(
            viewModel = viewModel,
            nodes = allProjectNodes,
            onExit = { viewModel.isPresentationMode = false }
        )
        return
    }

    val nodes by viewModel.nodes.collectAsState()
    val connections by viewModel.connections.collectAsState()

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newNodeTitle by remember { mutableStateOf("") }
    var newNodeContent by remember { mutableStateOf("") }
    var newNodeType by remember { mutableStateOf("Event") }
    var newNodeColorHex by remember { mutableStateOf("#3B82F6") }
    var showStructureMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val isSubCanvas = viewModel.currentParentNodeId > 0L
    val tileColors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#EF4444", "#14B8A6")

    // State for smooth dragging
    val draggingOffsets = remember { mutableStateMapOf<Long, Offset>() }

    // Search filter
    val filteredNodes = nodes.filter { node ->
        if (viewModel.searchQuery.isBlank()) true
        else node.title.contains(viewModel.searchQuery, ignoreCase = true) ||
                node.tags.contains(viewModel.searchQuery, ignoreCase = true) ||
                node.nodeType.contains(viewModel.searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSubCanvas) "${viewModel.currentSubNodeTitle} (${Strings.get("open_subcanvas", lang)})" else Strings.get("app_title", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isSubCanvas) Strings.get("subcanvas_workspace", lang) else Strings.get("infinite_canvas", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            SoundEffectManager.playClick()
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
                    // Presentation Mode Button
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        viewModel.isPresentationMode = true
                    }) {
                        Icon(Icons.Default.Slideshow, contentDescription = Strings.get("presentation_mode", lang), tint = MaterialTheme.colorScheme.primary)
                    }

                    // Structure Auto-Layouts
                    Box {
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            showStructureMenu = true
                        }) {
                            Icon(Icons.Default.AccountTree, contentDescription = Strings.get("structures", lang))
                        }

                        DropdownMenu(
                            expanded = showStructureMenu,
                            onDismissRequest = { showStructureMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.get("mindmap_layout", lang)) },
                                onClick = {
                                    showStructureMenu = false
                                    viewModel.arrangeStructure("MINDMAP")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("tree_structure", lang)) },
                                onClick = {
                                    showStructureMenu = false
                                    viewModel.arrangeStructure("TREE")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("timeline_sequence", lang)) },
                                onClick = {
                                    showStructureMenu = false
                                    viewModel.arrangeStructure("TIMELINE")
                                }
                            )
                        }
                    }

                    // Export Markdown Outline
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        showExportDialog = true
                    }) {
                        Icon(Icons.Default.Share, contentDescription = Strings.get("export_markdown", lang))
                    }

                    // Music Player Icon Button
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        viewModel.isMusicPlayerOpen = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = Strings.get("music_player", lang),
                            tint = if (viewModel.audioPlayerManager.isPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    // Minimap
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        viewModel.isMinimapOpen = true
                    }) {
                        Icon(Icons.Default.Map, contentDescription = Strings.get("minimap", lang))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            var expanded by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(visible = expanded, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtendedFloatingActionButton(
                            onClick = { expanded = false; showAddDialog = true; newNodeType = "Main Topic" },
                            text = { Text("Main Topic", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Topic, null) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = { expanded = false; showAddDialog = true; newNodeType = "Subtopic" },
                            text = { Text("Subtopic") },
                            icon = { Icon(Icons.Default.AccountTree, null) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = { expanded = false; showAddDialog = true; newNodeType = "Idea" },
                            text = { Text("Floating Idea") },
                            icon = { Icon(Icons.Default.Lightbulb, null) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = { expanded = false; showAddDialog = true; newNodeType = "Chapter" },
                            text = { Text("Chapter") },
                            icon = { Icon(Icons.Default.MenuBook, null) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
                FloatingActionButton(
                    onClick = {
                        SoundEffectManager.playClick()
                        expanded = !expanded
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_node_fab")
                ) {
                    Icon(if (expanded) Icons.Default.Close else Icons.Default.Add, contentDescription = Strings.get("add_node", lang))
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient Glowing Background
            AmbientGlowBackground(
                glowColorHex = viewModel.ambientGlowColorHex,
                isDarkMode = viewModel.isDarkMode,
                modifier = Modifier.fillMaxSize()
            )

            // Pan & Pinch-to-Zoom Workspace
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(0.3f, 3.0f)
                            zoomScale = newZoom
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                // Tilemap Grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (viewModel.isGridVisible) {
                        val gridStep = viewModel.gridSize * zoomScale
                        if (gridStep > 10f) {
                            val gridColor = if (viewModel.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                            val startX = (offsetX % gridStep + gridStep) % gridStep
                            val startY = (offsetY % gridStep + gridStep) % gridStep

                            var x = startX
                            while (x < size.width) {
                                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.5f)
                                x += gridStep
                            }
                            var y = startY
                            while (y < size.height) {
                                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.5f)
                                y += gridStep
                            }
                        }
                    }
                }

                // Infinite Virtual Canvas Workspace Layer with scale & translation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                            scaleX = zoomScale
                            scaleY = zoomScale
                        }
                ) {
                    // Draw Node Connections
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nodeMap = allProjectNodes.associateBy { it.id }
                        for (conn in connections) {
                            val fromNode = nodeMap[conn.fromNodeId]
                            val toNode = nodeMap[conn.toNodeId]
                            if (fromNode != null && toNode != null) {
                                val fromDrag = draggingOffsets[fromNode.id] ?: Offset.Zero
                                val toDrag = draggingOffsets[toNode.id] ?: Offset.Zero
                                
                                val fX = fromNode.xPos + fromDrag.x
                                val fY = fromNode.yPos + fromDrag.y
                                val tX = toNode.xPos + toDrag.x
                                val tY = toNode.yPos + toDrag.y

                                val fXPx = fX.dp.toPx()
                                val fYPx = fY.dp.toPx()
                                val tXPx = tX.dp.toPx()
                                val tYPx = tY.dp.toPx()
                                val widthPx = 230.dp.toPx()
                                val heightPx = 60.dp.toPx()

                                val startX = if (tX > fX) fXPx + widthPx else fXPx
                                val startY = fYPx + heightPx
                                val endX = if (tX > fX) tXPx else tXPx + widthPx
                                val endY = tYPx + heightPx

                                val controlX1 = startX + (endX - startX) / 2
                                val controlX2 = startX + (endX - startX) / 2

                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(startX, startY)
                                    cubicTo(controlX1, startY, controlX2, endY, endX, endY)
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.7f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 4f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    // Render Canvas Smart Node Cards
                    for (node in filteredNodes) {
                        if (!node.isCollapsed) {
                            val isSelected = viewModel.selectedNodeForDetail?.id == node.id
                            SmartStoryNodeCard(
                                node = node,
                                lang = lang,
                                zoomScale = zoomScale,
                                dragOffset = draggingOffsets[node.id] ?: Offset.Zero,
                                onDragUpdate = { offset ->
                                    draggingOffsets[node.id] = offset
                                },
                                onDragEnd = { dx, dy ->
                                    viewModel.updateNodePosition(node.id, node.xPos + dx, node.yPos + dy)
                                    draggingOffsets.remove(node.id)
                                },
                                onClick = {
                                    SoundEffectManager.playClick()
                                    viewModel.selectedNodeForDetail = node
                                },
                                onOpenDocument = {
                                    viewModel.openDocumentForNode(node)
                                },
                                onToggleCollapse = {
                                    viewModel.toggleCollapseNode(node)
                                },
                                onConnectClick = {
                                    SoundEffectManager.playClick()
                                    if (viewModel.connectingSourceNodeId == null) {
                                        viewModel.connectingSourceNodeId = node.id
                                    } else {
                                        viewModel.addConnection(viewModel.connectingSourceNodeId!!, node.id)
                                        viewModel.connectingSourceNodeId = null
                                    }
                                },
                                isConnecting = viewModel.connectingSourceNodeId == node.id,
                                isSelected = isSelected,
                                onQuickAdd = { direction ->
                                    viewModel.quickAddNode(node, direction)
                                }
                            )
                        }
                    }
                }
            }

            // Tilemap Floating Control Dock
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        viewModel.isSnapToGrid = !viewModel.isSnapToGrid
                    }) {
                        Icon(
                            imageVector = Icons.Default.Grid4x4,
                            contentDescription = Strings.get("grid_snap", lang),
                            tint = if (viewModel.isSnapToGrid) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        viewModel.isGridVisible = !viewModel.isGridVisible
                    }) {
                        Icon(
                            imageVector = if (viewModel.isGridVisible) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = Strings.get("grid_visible", lang),
                            tint = if (viewModel.isGridVisible) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.5f)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp))

                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.3f)
                    }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }

                    TextButton(onClick = {
                        SoundEffectManager.playClick()
                        zoomScale = 1.0f
                        offsetX = 0f
                        offsetY = 0f
                    }) {
                        Text("${(zoomScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        zoomScale = (zoomScale + 0.2f).coerceAtMost(3.0f)
                    }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                }
            }

            // Connection Target Mode Banner
            if (viewModel.connectingSourceNodeId != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(Strings.get("select_connect_target", lang))
                        TextButton(onClick = {
                            SoundEffectManager.playClick()
                            viewModel.connectingSourceNodeId = null
                        }) {
                            Text(Strings.get("cancel", lang))
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

                    Text(
                        text = Strings.get("color", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        tileColors.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Blue }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (newNodeColorHex == hex) 2.dp else 0.dp,
                                        color = if (newNodeColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newNodeColorHex = hex }
                            )
                        }
                    }
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
                                colorHex = newNodeColorHex,
                                x = (200f - offsetX) / zoomScale,
                                y = (200f - offsetY) / zoomScale
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

    // Export Dialog
    if (showExportDialog) {
        val markdownText = remember { viewModel.generateMarkdownExport() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings.get("export_dialog_title", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Strings.get("export_dialog_desc", lang))
                    OutlinedTextField(
                        value = markdownText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    SoundEffectManager.playClick()
                    clipboardManager.setText(AnnotatedString(markdownText))
                    showExportDialog = false
                }) {
                    Text(Strings.get("copy_to_clipboard", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings.get("close", lang))
                }
            }
        )
    }

    // Music Player Bottom Sheet
    if (viewModel.isMusicPlayerOpen) {
        MusicPlayerSheet(
            audioManager = viewModel.audioPlayerManager,
            lang = lang,
            onDismiss = { viewModel.isMusicPlayerOpen = false }
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
            },
            onOpenDocument = { node ->
                viewModel.openDocumentForNode(node)
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
fun SmartStoryNodeCard(
    node: CanvasNodeEntity,
    lang: String,
    zoomScale: Float,
    dragOffset: Offset,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onOpenDocument: () -> Unit,
    onToggleCollapse: () -> Unit,
    onConnectClick: () -> Unit,
    isConnecting: Boolean,
    isSelected: Boolean,
    onQuickAdd: (String) -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(node.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .offset(x = (node.xPos + dragOffset.x).dp, y = (node.yPos + dragOffset.y).dp)
    ) {
        // Quick add handles (only visible when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .offset(x = 240.dp, y = 40.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onQuickAdd("RIGHT") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Right", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = 40.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onQuickAdd("LEFT") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Left", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier
                    .offset(x = 95.dp, y = 130.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onQuickAdd("BOTTOM") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bottom", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        Card(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnecting) MaterialTheme.colorScheme.tertiaryContainer else cardColor.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 16.dp else 8.dp),
            modifier = Modifier
                .width(230.dp)
                .pointerInput(node.id, zoomScale) {
                    val density = density
                    var currentDragOffset = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            currentDragOffset = dragOffset
                        },
                        onDragEnd = {
                            onDragEnd(currentDragOffset.x, currentDragOffset.y)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dx = (dragAmount.x / zoomScale) / density
                            val dy = (dragAmount.y / zoomScale) / density
                            currentDragOffset = Offset(currentDragOffset.x + dx, currentDragOffset.y + dy)
                            onDragUpdate(currentDragOffset)
                        }
                    )
                }
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header: Marker Icon + Node Type Badge + Connect Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = Strings.translateNodeType(node.nodeType, lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Open Document Note Button
                        IconButton(
                            onClick = onOpenDocument,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = Strings.get("open_doc_note", lang),
                                tint = if (node.documentNote.isNotBlank()) Color.Yellow else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Connect button
                        IconButton(
                            onClick = onConnectClick,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = node.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )

                if (node.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = node.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2
                    )
                }

                if (node.documentNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenDocument() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(Strings.get("full_doc_attached", lang), fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                if (node.progress > 0 || node.dateLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (node.dateLabel.isNotBlank()) {
                            Text(node.dateLabel, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        if (node.progress > 0) {
                            Text("${node.progress}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
