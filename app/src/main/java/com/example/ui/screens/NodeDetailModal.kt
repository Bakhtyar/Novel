package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEffectManager
import com.example.data.CanvasNodeEntity
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailModal(
    viewModel: StoryViewModel,
    node: CanvasNodeEntity,
    onDismiss: () -> Unit,
    onOpenSubCanvas: (Long, String) -> Unit,
    onOpenDocument: (CanvasNodeEntity) -> Unit
) {
    val lang = viewModel.language
    var title by remember { mutableStateOf(node.title) }
    var content by remember { mutableStateOf(node.content) }
    var nodeType by remember { mutableStateOf(node.nodeType) }
    var colorHex by remember { mutableStateOf(node.colorHex) }
    var tags by remember { mutableStateOf(node.tags) }
    var markerIcon by remember { mutableStateOf(node.markerIcon) }
    var progress by remember { mutableFloatStateOf(node.progress.toFloat()) }
    var isCompleted by remember { mutableStateOf(node.isCompleted) }
    var dateLabel by remember { mutableStateOf(node.dateLabel) }
    var boundaryGroup by remember { mutableStateOf(node.boundaryGroup) }
    var showSketch by remember { mutableStateOf(false) }

    val colorsList = listOf("#3B82F6", "#10B981", "#8B5CF6", "#EF4444", "#F59E0B", "#EC4899", "#14B8A6", "#6366F1")
    val nodeTypes = listOf("Main Topic", "Subtopic", "Character", "Chapter", "Event", "Kingdom", "Ability", "Secret", "Timeline", "Idea")
    val iconsList = listOf("star", "person", "kingdom", "book", "flag", "secret", "lightning", "check")

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.updateNodeDetails(
                node.copy(
                    title = title,
                    content = content,
                    nodeType = nodeType,
                    colorHex = colorHex,
                    tags = tags,
                    markerIcon = markerIcon,
                    progress = progress.toInt(),
                    isCompleted = isCompleted,
                    dateLabel = dateLabel,
                    boundaryGroup = boundaryGroup
                )
            )
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.get("smart_node_settings", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.duplicateNode(node); onDismiss() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = Strings.get("duplicate", lang))
                    }
                    IconButton(onClick = { viewModel.deleteNode(node); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = Strings.get("delete", lang), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // PRIMARY HERO ACTION: Open Full Document Note Editor
            Card(
                onClick = {
                    viewModel.updateNodeDetails(
                        node.copy(
                            title = title,
                            content = content,
                            nodeType = nodeType,
                            colorHex = colorHex,
                            tags = tags,
                            markerIcon = markerIcon,
                            progress = progress.toInt(),
                            isCompleted = isCompleted,
                            dateLabel = dateLabel,
                            boundaryGroup = boundaryGroup
                        )
                    )
                    onDismiss()
                    onOpenDocument(node.copy(title = title, content = content))
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.get("open_full_doc", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (node.documentNote.isNotBlank()) "${node.documentNote.length} ${Strings.get("chars_written", lang)}" else Strings.get("dedicated_writing_studio", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(Strings.get("node_title", lang)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(Strings.get("short_summary", lang)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

            // Node Category Selector
            Text(Strings.get("node_category", lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            ScrollableTabRow(
                selectedTabIndex = nodeTypes.indexOf(nodeType).coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                nodeTypes.forEach { type ->
                    Tab(
                        selected = nodeType == type,
                        onClick = { nodeType = type; SoundEffectManager.playClick() },
                        text = { Text(Strings.translateNodeType(type, lang)) }
                    )
                }
            }

            // Marker Icon Selector
            Text(Strings.get("marker_badge", lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                iconsList.forEach { iconKey ->
                    FilterChip(
                        selected = markerIcon == iconKey,
                        onClick = { markerIcon = iconKey; SoundEffectManager.playClick() },
                        label = { Text(iconKey) }
                    )
                }
            }

            // Color Picker
            Text(text = Strings.get("color", lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            com.example.ui.components.HsvColorPicker(
                colorHex = colorHex,
                onColorChanged = { colorHex = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${Strings.get("completion_progress", lang)} (${progress.toInt()}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
            }
            Slider(
                value = progress,
                onValueChange = { progress = it },
                valueRange = 0f..100f,
                steps = 10
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text(Strings.get("date_chapter_label", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = boundaryGroup,
                    onValueChange = { boundaryGroup = it },
                    label = { Text(Strings.get("boundary_group", lang)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(Strings.get("tags", lang)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showSketch = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("sketch", lang))
                }

                Button(
                    onClick = {
                        viewModel.updateNodeDetails(node.copy(title = title, content = content, nodeType = nodeType, colorHex = colorHex, tags = tags))
                        onDismiss()
                        onOpenSubCanvas(node.id, title)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("open_subcanvas", lang))
                }
            }

            Button(
                onClick = {
                    viewModel.updateNodeDetails(
                        node.copy(
                            title = title,
                            content = content,
                            nodeType = nodeType,
                            colorHex = colorHex,
                            tags = tags,
                            markerIcon = markerIcon,
                            progress = progress.toInt(),
                            isCompleted = isCompleted,
                            dateLabel = dateLabel,
                            boundaryGroup = boundaryGroup
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.get("save", lang))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSketch) {
        SketchCanvasDialog(
            lang = lang,
            initialSketch = node.sketchData,
            onDismiss = { showSketch = false },
            onSave = { sketchStr ->
                viewModel.updateNodeDetails(node.copy(sketchData = sketchStr))
                showSketch = false
            }
        )
    }
}
