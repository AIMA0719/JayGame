package com.jay.jaygame.audio

/**
 * All discrete sound effects in the game.
 * Each maps to a future .ogg / .mp3 asset in assets/sfx/.
 */
enum class SoundEvent {
    Summon,
    SummonRare,
    SummonLegend,
    Merge,
    MergeLucky,
    Attack,
    CriticalHit,
    EnemyDeath,
    BossAppear,
    BossDefeat,
    WaveStart,
    WaveClear,
    Victory,
    Defeat,
    ButtonClick,
    GoldPickup,
    LevelUp,
    SkillActivate,
}
