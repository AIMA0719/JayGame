package com.example.jaygame.audio

import android.content.Context
import android.media.MediaPlayer

object BgmManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentAsset: String? = null

    fun play(context: Context, assetPath: String, loop: Boolean = true) {
        if (currentAsset == assetPath && mediaPlayer?.isPlaying == true) return
        stop()
        val afd = context.assets.openFd(assetPath)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = loop
            setVolume(0.5f, 0.5f)
            prepare()
            start()
        }
        currentAsset = assetPath
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentAsset = null
    }

    fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        mediaPlayer?.takeIf { !it.isPlaying }?.start()
    }
}
