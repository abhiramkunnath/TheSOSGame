package com.example.thesosgame.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var chimeSound: Int? = null
    private var winnerSound: Int? = null
    private var isInitialized = false
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                soundPool = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build()
                
                // Load sounds - For now, we'll use system sounds
                // In a real implementation, you'd add custom sound files to res/raw/
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun playChime() {
        if (!isInitialized) return
        
        // For now, we'll use a simple vibration feedback
        // In a real app, you'd load and play an actual chime sound
        try {
            // This creates a subtle audio feedback using the system
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                // Play a subtle system sound for match
                playSystemSound(AudioManager.FX_KEY_CLICK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playWinnerSound() {
        if (!isInitialized) return
        
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                // Play a celebration sound for winner
                playSystemSound(AudioManager.FX_KEYPRESS_STANDARD)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playSystemSound(soundType: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(soundType, 0.5f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}

// Extension function to create a SoundManager
fun Context.createSoundManager(): SoundManager {
    return SoundManager(this)
}
