package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CinematicLightningEffect(
    isEnabled: Boolean,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val baseColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color.White
    }

    var lightningPaths by remember { mutableStateOf<List<Path>>(emptyList()) }
    var lightningAlpha by remember { mutableFloatStateOf(0f) }

    val alphaAnim = animateFloatAsState(
        targetValue = lightningAlpha,
        animationSpec = tween(
            durationMillis = if (lightningAlpha > 0f) 50 else 800, // fast in, slow out
            easing = FastOutSlowInEasing
        ),
        label = "lightning_alpha"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 10000)) // wait 3-10 seconds
            
            // Generate lightning
            val newPaths = mutableListOf<Path>()
            val mainPath = Path()
            
            // Random start on screen bounds
            val startX = Random.nextFloat() * 1000f + 100f
            val startY = 0f
            
            mainPath.moveTo(startX, startY)
            var currentX = startX
            var currentY = startY
            
            while (currentY < 2000f) {
                currentX += Random.nextFloat() * 100f - 50f
                currentY += Random.nextFloat() * 100f + 50f
                mainPath.lineTo(currentX, currentY)
                
                // Branching
                if (Random.nextFloat() > 0.8f) {
                    val branchPath = Path()
                    branchPath.moveTo(currentX, currentY)
                    var branchX = currentX
                    var branchY = currentY
                    for (i in 0..3) {
                        branchX += Random.nextFloat() * 80f - 40f
                        branchY += Random.nextFloat() * 80f + 40f
                        branchPath.lineTo(branchX, branchY)
                    }
                    newPaths.add(branchPath)
                }
            }
            newPaths.add(mainPath)
            
            lightningPaths = newPaths
            lightningAlpha = 0.6f // Flash!
            
            delay(100) // Hold
            lightningAlpha = 0f // Fade
        }
    }

    if (alphaAnim.value > 0.01f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            lightningPaths.forEach { path ->
                // Draw glow
                drawPath(
                    path = path,
                    color = baseColor.copy(alpha = alphaAnim.value * 0.5f),
                    style = Stroke(
                        width = 12f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                // Draw core
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = alphaAnim.value),
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
