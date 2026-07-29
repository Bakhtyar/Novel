package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffectManager
import com.example.data.CanvasNodeEntity
import com.example.ui.components.AmbientGlowBackground
import com.example.ui.localization.Strings
import com.example.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.delay

@Composable
fun PresentationModeScreen(
    viewModel: StoryViewModel,
    nodes: List<CanvasNodeEntity>,
    onExit: () -> Unit
) {
    val lang = viewModel.language
    var currentIndex by remember { mutableIntStateOf(0) }
    var isAutoPlaying by remember { mutableStateOf(false) }

    val currentNode = nodes.getOrNull(currentIndex)

    // Auto-play timer
    LaunchedEffect(isAutoPlaying, currentIndex) {
        if (isAutoPlaying && nodes.isNotEmpty()) {
            delay(4000) // 4s per slide
            if (currentIndex < nodes.size - 1) {
                currentIndex++
            } else {
                isAutoPlaying = false
            }
        }
    }

    val nodeColor = try {
        Color(android.graphics.Color.parseColor(currentNode?.colorHex ?: "#3B82F6"))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        AmbientGlowBackground(
            glowColorHex = currentNode?.colorHex ?: "#8B5CF6",
            isDarkMode = true,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Slideshow, contentDescription = null, tint = Color.White)
                    Text(
                        text = Strings.get("story_presentation", lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    SoundEffectManager.playClick()
                    onExit()
                }) {
                    Icon(Icons.Default.Close, contentDescription = Strings.get("exit_presentation", lang), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Presentation Card
            if (currentNode != null) {
                AnimatedContent(
                    targetState = currentNode,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally { width -> width } togetherWith fadeOut() + slideOutHorizontally { width -> -width }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "presentation_slide"
                ) { node ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(nodeColor)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = nodeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = Strings.translateNodeType(node.nodeType, lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = nodeColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (node.content.isNotBlank()) {
                                Text(
                                    text = node.content,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (node.documentNote.isNotBlank()) {
                                Text(
                                    text = Strings.get("detailed_note", lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = node.documentNote,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(Strings.get("no_nodes_present", lang), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Controls Bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Slide
                    IconButton(
                        onClick = {
                            SoundEffectManager.playClick()
                            if (currentIndex > 0) currentIndex--
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = Strings.get("previous", lang))
                    }

                    // Slide Counter
                    Text(
                        text = "${Strings.get("slide", lang)} ${currentIndex + 1} ${Strings.get("of", lang)} ${nodes.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Auto Play Toggle
                    IconButton(onClick = {
                        SoundEffectManager.playClick()
                        isAutoPlaying = !isAutoPlaying
                    }) {
                        Icon(
                            imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = Strings.get("auto_play", lang),
                            tint = if (isAutoPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    // Next Slide
                    IconButton(
                        onClick = {
                            SoundEffectManager.playClick()
                            if (currentIndex < nodes.size - 1) currentIndex++
                        },
                        enabled = currentIndex < nodes.size - 1
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = Strings.get("next", lang))
                    }
                }
            }
        }
    }
}
