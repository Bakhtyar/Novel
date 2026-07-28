package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CanvasNodeEntity
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailModal(
    viewModel: StoryViewModel,
    node: CanvasNodeEntity,
    onDismiss: () -> Unit,
    onOpenSubCanvas: (Long, String) -> Unit
) {
    val lang = viewModel.language
    var title by remember { mutableStateOf(node.title) }
    var content by remember { mutableStateOf(node.content) }
    var nodeType by remember { mutableStateOf(node.nodeType) }
    var colorHex by remember { mutableStateOf(node.colorHex) }
    var tags by remember { mutableStateOf(node.tags) }
    var showSketch by remember { mutableStateOf(false) }

    val colorsList = listOf("#3B82F6", "#10B981", "#8B5CF6", "#EF4444", "#F59E0B", "#EC4899")

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.updateNodeDetails(node.copy(title = title, content = content, nodeType = nodeType, colorHex = colorHex, tags = tags))
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
                    text = if (lang == "ar") "تفاصيل الصندوق" else "Box Details",
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
                label = { Text(Strings.get("node_content", lang)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Color Picker
            Text(text = Strings.get("color", lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (hex in colorsList) {
                    val c = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorHex == hex) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
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
                    viewModel.updateNodeDetails(node.copy(title = title, content = content, nodeType = nodeType, colorHex = colorHex, tags = tags))
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
