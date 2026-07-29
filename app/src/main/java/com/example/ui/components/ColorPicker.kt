package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun HsvColorPicker(
    colorHex: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    // Parse initial color only once
    LaunchedEffect(Unit) {
        try {
            val color = Color(android.graphics.Color.parseColor(colorHex))
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colorHex), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        } catch (e: Exception) {
            // Default to blue
            hue = 210f
            saturation = 1f
            value = 1f
        }
    }

    val currentColor = Color.hsv(hue, saturation, value)
    
    // Update color string function
    val updateColor = { h: Float, s: Float, v: Float ->
        hue = h
        saturation = s
        value = v
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val hex = String.format("#%06X", (0xFFFFFF and argb))
        onColorChanged(hex)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Saturation / Value Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        updateColor(hue, s, v)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        updateColor(hue, s, v)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Hue base
                drawRect(color = Color.hsv(hue, 1f, 1f))
                // Saturation gradient (white to transparent)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
                // Value gradient (transparent to black)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Draw indicator
                val x = saturation * size.width
                val y = (1f - value) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Hue Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        updateColor(h, saturation, value)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                        updateColor(h, saturation, value)
                    }
                }
        ) {
            val hueColors = remember {
                listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(colors = hueColors)
                )
                
                // Draw indicator
                val x = (hue / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(x, size.height / 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            )
            Text(
                text = String.format("#%06X", (0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
