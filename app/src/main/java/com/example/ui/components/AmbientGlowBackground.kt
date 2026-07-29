package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AmbientGlowBackground(
    glowColorHex: String = "#8B5CF6",
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val baseColor = try {
        Color(android.graphics.Color.parseColor(glowColorHex))
    } catch (_: Exception) {
        Color(0xFF8B5CF6)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")

    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_offset_1"
    )

    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_offset_2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val glowAlpha = if (isDarkMode) 0.18f else 0.12f

        // Center orb 1
        val center1 = Offset(
            x = w * (0.2f + 0.6f * floatOffset1),
            y = h * (0.2f + 0.6f * floatOffset2)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = glowAlpha), Color.Transparent),
                center = center1,
                radius = (w.coerceAtLeast(h) * 0.5f)
            ),
            center = center1,
            radius = w.coerceAtLeast(h) * 0.5f
        )

        // Secondary orb 2
        val center2 = Offset(
            x = w * (0.8f - 0.5f * floatOffset2),
            y = h * (0.7f - 0.5f * floatOffset1)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                center = center2,
                radius = (w.coerceAtLeast(h) * 0.45f)
            ),
            center = center2,
            radius = w.coerceAtLeast(h) * 0.45f
        )
    }
}
