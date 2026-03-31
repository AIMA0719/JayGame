package com.jay.jaygame.audio

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.view.animation.LinearInterpolator

object BgmManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentAsset: String? = null
    private var fadeOutAnimator: ValueAnimator? = null
    private var fadeInAnimator: ValueAnimator? = null
    private var targetVolume = 0.5f

    private const val CROSSFADE_MS = 500L

    fun play(context: Context, assetPath: String, loop: Boolean = true) {
        if (currentAsset == assetPath && mediaPlayer?.isPlaying == true) return

        val oldPlayer = mediaPlayer
        if (oldPlayer != null && oldPlayer.isPlaying) {
            crossfadeOut(oldPlayer) {
                try { oldPlayer.release() } catch (_: IllegalStateException) { }
            }
        } else {
            try { oldPlayer?.release() } catch (_: IllegalStateException) { }
        }

        mediaPlayer = null
        currentAsset = null

        val newPlayer = MediaPlayer()
        try {
            val afd = context.assets.openFd(assetPath)
            newPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            newPlayer.isLooping = loop
            newPlayer.setVolume(0f, 0f)
            newPlayer.prepare()
            newPlayer.start()
        } catch (_: Exception) {
            newPlayer.release()
            return
        }
        mediaPlayer = newPlayer
        currentAsset = assetPath

        fadeIn(newPlayer, targetVolume)
    }

    fun stop() {
        cancelAllFades()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: IllegalStateException) { }
        mediaPlayer = null
        currentAsset = null
    }

    fun pause() {
        try { mediaPlayer?.takeIf { it.isPlaying }?.pause() } catch (_: IllegalStateException) { }
    }

    fun resume() {
        try { mediaPlayer?.takeIf { !it.isPlaying }?.start() } catch (_: IllegalStateException) { }
    }

    private fun crossfadeOut(player: MediaPlayer, onComplete: () -> Unit) {
        fadeOutAnimator?.cancel()
        fadeOutAnimator = ValueAnimator.ofFloat(targetVolume, 0f).apply {
            duration = CROSSFADE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val vol = anim.animatedValue as Float
                try { player.setVolume(vol, vol) } catch (_: IllegalStateException) { }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    try { if (player.isPlaying) player.stop() } catch (_: IllegalStateException) { }
                    onComplete()
                }
            })
            start()
        }
    }

    private fun fadeIn(player: MediaPlayer, target: Float) {
        fadeInAnimator?.cancel()
        fadeInAnimator = ValueAnimator.ofFloat(0f, target).apply {
            duration = CROSSFADE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val vol = anim.animatedValue as Float
                try { player.setVolume(vol, vol) } catch (_: IllegalStateException) { }
            }
            start()
        }
    }

    private fun cancelAllFades() {
        fadeOutAnimator?.cancel()
        fadeOutAnimator = null
        fadeInAnimator?.cancel()
        fadeInAnimator = null
    }
}
