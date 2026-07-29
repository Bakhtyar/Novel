package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.localization.Strings

data class PathPoint(val offset: Offset, val isNew: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchCanvasDialog(
    lang: String,
    initialSketch: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val points = remember { mutableStateListOf<PathPoint>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("sketch", lang)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.DarkGray, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    points.add(PathPoint(offset, isNew = true))
                                },
                                onDrag = { change, _ ->
                                    points.add(PathPoint(change.position, isNew = false))
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var lastPoint: Offset? = null
                        for (pt in points) {
                            if (pt.isNew || lastPoint == null) {
                                lastPoint = pt.offset
                            } else {
                                drawLine(
                                    color = Color.Cyan,
                                    start = lastPoint,
                                    end = pt.offset,
                                    strokeWidth = 4f,
                                    cap = StrokeCap.Round
                                )
                                lastPoint = pt.offset
                            }
                        }
                    }
                    if (points.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(Strings.get("draw_sketch_here", lang), color = Color.LightGray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val serialized = points.joinToString(";") { "${it.offset.x},${it.offset.y},${it.isNew}" }
                onSave(serialized)
            }) {
                Text(Strings.get("save", lang))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { points.clear() }) {
                    Text(Strings.get("clear", lang))
                }
                TextButton(onClick = onDismiss) {
                    Text(Strings.get("cancel", lang))
                }
            }
        }
    )
}
