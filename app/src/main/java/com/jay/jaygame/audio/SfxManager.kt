package com.jay.jaygame.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SFX playback manager backed by [SoundPool].
 *
 * Call [init] once with an application/activity Context to set up the pool,
 * then [play] with any [SoundEvent].  Sound assets are in assets/sfx/ (CC0, Kenney.nl).
 */
object SfxManager {

    private const val TAG = "SfxManager"
    private const val MAX_STREAMS = 8

    @Volatile
    private var soundPool: SoundPool? = null
    private val loadedIds = mutableMapOf<SoundEvent, Int>()
    private var enabled = true

    /**
     * Future mapping: SoundEvent -> asset path under assets/sfx/.
     * Add entries here when real sound files are available.
     */
    private val assetPaths = mapOf(
        SoundEvent.Summon        to "sfx/summon.ogg",
        SoundEvent.SummonRare    to "sfx/summon_rare.ogg",
        SoundEvent.SummonLegend  to "sfx/summon_legend.ogg",
        SoundEvent.Merge         to "sfx/merge.ogg",
        SoundEvent.MergeLucky    to "sfx/merge_lucky.mp3",
        SoundEvent.Attack        to "sfx/attack.ogg",
        SoundEvent.AttackMelee   to "sfx/attack_melee.mp3",
        SoundEvent.AttackRanged  to "sfx/attack_ranged.mp3",
        SoundEvent.AttackMagic   to "sfx/attack_magic.mp3",
        SoundEvent.CriticalHit   to "sfx/critical.mp3",
        SoundEvent.EnemyDeath    to "sfx/enemy_death.ogg",
        SoundEvent.BossAppear    to "sfx/boss_appear.ogg",
        SoundEvent.BossDefeat    to "sfx/boss_defeat.ogg",
        SoundEvent.WaveStart     to "sfx/wave_start.ogg",
        SoundEvent.WaveClear     to "sfx/wave_clear.ogg",
        SoundEvent.Victory       to "sfx/victory.ogg",
        SoundEvent.Defeat        to "sfx/defeat.ogg",
        SoundEvent.ButtonClick   to "sfx/button_click.ogg",
        SoundEvent.GoldPickup    to "sfx/gold_pickup.ogg",
        SoundEvent.LevelUp       to "sfx/level_up.ogg",
        SoundEvent.SkillActivate to "sfx/skill_activate.ogg",
        SoundEvent.RoguelikeCardReveal to "sfx/summon.ogg",
    )

    /** Initialise the SoundPool and preload all registered assets. */
    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attrs)
            .build()

        val pool = soundPool ?: return
        for ((event, path) in assetPaths) {
            try {
                val afd = context.assets.openFd(path)
                try {
                    val id = pool.load(afd, 1)
                    loadedIds[event] = id
                } finally {
                    afd.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load $path for $event: ${e.message}")
            }
        }
        Log.d(TAG, "SoundPool initialised – ${loadedIds.size} sounds loaded")
    }

    fun initAsync(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { init(context) }
    }

    /** Play a sound effect.  Silently no-ops if the asset is not yet loaded. */
    fun play(event: SoundEvent, volume: Float = 1f) {
        if (!enabled) return
        val id = loadedIds[event]
        if (id != null) {
            soundPool?.play(id, volume, volume, 1, 0, 1f)
        } else {
            Log.d(TAG, "play(${event.name}) – no asset loaded (stub)")
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loadedIds.clear()
        Log.d(TAG, "SoundPool released")
    }
}
