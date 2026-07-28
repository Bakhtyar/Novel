package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.CanvasNodeEntity
import com.example.ui.localization.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimapDialog(
    lang: String,
    nodes: List<CanvasNodeEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("minimap", lang)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Miniature representation of nodes on canvas (5000x5000 scaled down)
                    val scaleX = 280f / 5000f
                    val scaleY = 280f / 5000f

                    for (node in nodes) {
                        val nx = (node.xPos * scaleX).coerceIn(0f, 260f)
                        val ny = (node.yPos * scaleY).coerceIn(0f, 260f)
                        Box(
                            modifier = Modifier
                                .offset(x = nx.dp, y = ny.dp)
                                .size(width = 40.dp, height = 24.dp)
                                .background(Color(android.graphics.Color.parseColor(node.colorHex)), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Mini label
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(Strings.get("save", lang))
            }
        }
    )
}
