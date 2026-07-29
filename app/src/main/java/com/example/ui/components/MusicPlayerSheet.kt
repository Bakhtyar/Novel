package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioTrackItem
import com.example.audio.SoundEffectManager
import com.example.ui.localization.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerSheet(
    audioManager: AudioPlayerManager,
    lang: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isListExpanded by remember { mutableStateOf(false) }

    // Storage Picker Launcher for local audio
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            SoundEffectManager.playSuccess()
            for (uri in uris) {
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Local Audio Track"
                audioManager.addLocalTrack(uri, fileName)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("music_player", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = Strings.get("cancel", lang))
                }
            }

            // Current Track Card
            val current = audioManager.currentTrack
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = current?.title ?: Strings.get("no_track", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = current?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite toggle
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            current?.let { audioManager.toggleFavorite(it.id) }
                        }) {
                            Icon(
                                imageVector = if (current?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (current?.isFavorite == true) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Previous
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            audioManager.previousTrack()
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                        }

                        // Play/Pause
                        FloatingActionButton(
                            onClick = {
                                SoundEffectManager.playClick()
                                audioManager.togglePlayPause()
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (audioManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }

                        // Next
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            audioManager.nextTrack()
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                        }

                        // Playlist Favorites Loop Toggle
                        IconButton(onClick = {
                            SoundEffectManager.playClick()
                            audioManager.isLoopingFavorites = !audioManager.isLoopingFavorites
                        }) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Loop Favorites",
                                tint = if (audioManager.isLoopingFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Loop Favorites Banner Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (audioManager.isLoopingFavorites) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.get("loop_favorites", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = audioManager.isLoopingFavorites,
                    onCheckedChange = {
                        SoundEffectManager.playClick()
                        audioManager.isLoopingFavorites = it
                    }
                )
            }

            // Import Local Music Button
            Button(
                onClick = {
                    SoundEffectManager.playClick()
                    audioPicker.launch("audio/*")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("import_local_audio", lang))
            }

            // Playlist Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SoundEffectManager.playClick()
                        isListExpanded = !isListExpanded
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.get("playlist", lang) + " (${audioManager.tracks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // Playlist Track Items
            AnimatedVisibility(visible = isListExpanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioManager.tracks, key = { it.id }) { track ->
                        Card(
                            onClick = {
                                SoundEffectManager.playClick()
                                audioManager.playTrack(track)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (audioManager.currentTrack?.id == track.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    SoundEffectManager.playClick()
                                    audioManager.toggleFavorite(track.id)
                                }) {
                                    Icon(
                                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
