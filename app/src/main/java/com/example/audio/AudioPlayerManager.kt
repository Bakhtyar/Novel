package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String = "Local Audio",
    val uri: Uri?,
    val isFavorite: Boolean = false,
    val isPreset: Boolean = false
)

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    var currentTrack by mutableStateOf<AudioTrackItem?>(null)
    var isPlaying by mutableStateOf(false)
    var isLoopingFavorites by mutableStateOf(false)
    val tracks = mutableStateListOf<AudioTrackItem>()

    init {
        // Seed default ambient relaxation presets
        tracks.addAll(
            listOf(
                AudioTrackItem(
                    id = "preset_ambient_1",
                    title = "Deep Focus Cosmos",
                    artist = "Ambient Synth",
                    uri = null,
                    isFavorite = true,
                    isPreset = true
                ),
                AudioTrackItem(
                    id = "preset_ambient_2",
                    title = "Mindful Waves & Rain",
                    artist = "Story Canvas Ambient",
                    uri = null,
                    isFavorite = true,
                    isPreset = true
                ),
                AudioTrackItem(
                    id = "preset_ambient_3",
                    title = "Night Worldbuilding Flow",
                    artist = "Chilled Lofi",
                    uri = null,
                    isFavorite = false,
                    isPreset = true
                )
            )
        )
        currentTrack = tracks.firstOrNull()
    }

    fun addLocalTrack(uri: Uri, title: String) {
        val newTrack = AudioTrackItem(
            id = "local_${System.currentTimeMillis()}",
            title = title,
            artist = "Device Storage",
            uri = uri,
            isFavorite = true
        )
        tracks.add(0, newTrack)
        playTrack(newTrack)
    }

    fun toggleFavorite(trackId: String) {
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index != -1) {
            val updated = tracks[index].copy(isFavorite = !tracks[index].isFavorite)
            tracks[index] = updated
            if (currentTrack?.id == trackId) {
                currentTrack = updated
            }
        }
    }

    fun playTrack(track: AudioTrackItem) {
        currentTrack = track
        stopPlayer()

        if (track.uri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, track.uri)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        this@AudioPlayerManager.isPlaying = true
                    }
                    setOnCompletionListener {
                        this@AudioPlayerManager.isPlaying = false
                        onTrackCompleted()
                    }
                    setOnErrorListener { _, _, _ ->
                        this@AudioPlayerManager.isPlaying = false
                        true
                    }
                }
            } catch (_: Exception) {
                isPlaying = false
            }
        } else {
            // Preset simulated ambient audio mode
            isPlaying = true
        }
    }

    fun togglePlayPause() {
        val track = currentTrack ?: return
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
                isPlaying = true
            } else {
                playTrack(track)
            }
        }
    }

    fun nextTrack() {
        val activeList = getActivePlaylist()
        if (activeList.isEmpty()) return
        val currentIndex = activeList.indexOfFirst { it.id == currentTrack?.id }
        val nextIndex = if (currentIndex == -1 || currentIndex >= activeList.size - 1) 0 else currentIndex + 1
        playTrack(activeList[nextIndex])
    }

    fun previousTrack() {
        val activeList = getActivePlaylist()
        if (activeList.isEmpty()) return
        val currentIndex = activeList.indexOfFirst { it.id == currentTrack?.id }
        val prevIndex = if (currentIndex <= 0) activeList.size - 1 else currentIndex - 1
        playTrack(activeList[prevIndex])
    }

    private fun onTrackCompleted() {
        if (isLoopingFavorites) {
            nextTrack()
        }
    }

    private fun getActivePlaylist(): List<AudioTrackItem> {
        val favorites = tracks.filter { it.isFavorite }
        return if (isLoopingFavorites && favorites.isNotEmpty()) favorites else tracks
    }

    private fun stopPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
    }

    fun release() {
        stopPlayer()
    }
}
