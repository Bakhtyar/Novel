package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator

object SoundEffectManager {
    private var toneGenerator: ToneGenerator? = null
    var isSfxEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playClick() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Exception) {}
    }

    fun playSuccess() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        } catch (_: Exception) {}
    }

    fun playSnap() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 20)
        } catch (_: Exception) {}
    }

    fun playDrag() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 15)
        } catch (_: Exception) {}
    }

    fun playDrop() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 35)
        } catch (_: Exception) {}
    }

    fun playAddNode() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
        } catch (_: Exception) {}
    }

    fun playDelete() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 45)
        } catch (_: Exception) {}
    }

    fun playConnect() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 50)
        } catch (_: Exception) {}
    }

    fun playOpenNote() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 35)
        } catch (_: Exception) {}
    }

    fun playCloseNote() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_B, 30)
        } catch (_: Exception) {}
    }

    fun playSave() {
        if (!isSfxEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 50)
        } catch (_: Exception) {}
    }
}
