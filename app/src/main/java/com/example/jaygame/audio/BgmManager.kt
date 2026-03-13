package com.example.jaygame.audio

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.view.animation.LinearInterpolator

object BgmManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentAsset: String? = null
    private var fadeAnimator: ValueAnimator? = null
    private var targetVolume = 0.5f

    private const val CROSSFADE_MS = 500L

    fun play(context: Context, assetPath: String, loop: Boolean = true) {
        if (currentAsset == assetPath && mediaPlayer?.isPlaying == true) return

        val oldPlayer = mediaPlayer
        if (oldPlayer != null && oldPlayer.isPlaying) {
            // Crossfade: fade out old, then start new
            crossfadeOut(oldPlayer) {
                oldPlayer.release()
            }
        } else {
            oldPlayer?.release()
        }

        mediaPlayer = null
        currentAsset = null

        val afd = context.assets.openFd(assetPath)
        val newPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = loop
            setVolume(0f, 0f)
            prepare()
            start()
        }
        mediaPlayer = newPlayer
        currentAsset = assetPath

        // Fade in the new player
        fadeIn(newPlayer, targetVolume)
    }

    fun stop() {
        cancelFade()
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

    /** Crossfade out [player] over [CROSSFADE_MS], then invoke [onComplete]. */
    private fun crossfadeOut(player: MediaPlayer, onComplete: () -> Unit) {
        cancelFade()
        fadeAnimator = ValueAnimator.ofFloat(targetVolume, 0f).apply {
            duration = CROSSFADE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val vol = anim.animatedValue as Float
                try {
                    player.setVolume(vol, vol)
                } catch (_: IllegalStateException) {
                    // player already released
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    try {
                        if (player.isPlaying) player.stop()
                    } catch (_: IllegalStateException) { }
                    onComplete()
                }
            })
            start()
        }
    }

    /** Fade in [player] from 0 to [target] over [CROSSFADE_MS]. */
    private fun fadeIn(player: MediaPlayer, target: Float) {
        cancelFade()
        fadeAnimator = ValueAnimator.ofFloat(0f, target).apply {
            duration = CROSSFADE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val vol = anim.animatedValue as Float
                try {
                    player.setVolume(vol, vol)
                } catch (_: IllegalStateException) { }
            }
            start()
        }
    }

    private fun cancelFade() {
        fadeAnimator?.cancel()
        fadeAnimator = null
    }
}
