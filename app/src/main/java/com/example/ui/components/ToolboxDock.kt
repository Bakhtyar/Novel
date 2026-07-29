package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.Strings
import kotlin.math.roundToInt

@Composable
fun ToolboxDock(
    lang: String,
    onToolDropped: (type: String, screenPosition: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedTool by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    
    val tools = listOf(
        "Main Topic" to Icons.Default.Topic,
        "Subtopic" to Icons.Default.AccountTree,
        "Chapter" to Icons.Default.MenuBook,
        "Idea" to Icons.Default.Lightbulb
    )

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tools.forEach { (type, icon) ->
                    var itemRootPos by remember { mutableStateOf(Offset.Zero) }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                itemRootPos = coords.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        draggedTool = type
                                        dragPosition = itemRootPos
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragPosition += dragAmount
                                    },
                                    onDragEnd = {
                                        val droppedTool = draggedTool
                                        val pos = dragPosition
                                        draggedTool = null
                                        if (droppedTool != null) {
                                            onToolDropped(droppedTool, pos)
                                        }
                                    },
                                    onDragCancel = {
                                        draggedTool = null
                                    }
                                )
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = type,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.translateNodeType(type, lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    // Render the dragged item overlay (globally)
    if (draggedTool != null) {
        val icon = tools.find { it.first == draggedTool }?.second ?: Icons.Default.Topic
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .offset { IntOffset(dragPosition.x.roundToInt(), dragPosition.y.roundToInt()) }
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }
        }
    }
}
