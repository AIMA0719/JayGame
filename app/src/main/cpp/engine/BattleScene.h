#ifndef JAYGAME_BATTLESCENE_H
#define JAYGAME_BATTLESCENE_H

#include "Scene.h"
#include "Grid.h"
#include "Wave.h"
#include "Unit.h"
#include "Enemy.h"
#include "Projectile.h"
#include "MergeSystem.h"
#include "Ability.h"
#include "ObjectPool.h"
#include "SpatialHash.h"
#include "SpriteBatch.h"
#include "MathTypes.h"
#include "SpriteAtlas.h"

#include <vector>
#include <memory>
#include <set>
#include <atomic>

class GameEngine;

class BattleScene : public Scene {
public:
    explicit BattleScene(GameEngine& engine);
    ~BattleScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

    // Public methods for JNI bridge
    void summonUnit();
    float getSummonCost() const;

    // Summon request flag — set from JNI (Compose thread), consumed on game thread
    static std::atomic<bool> summonRequested;

    // State getters for JNI bridge
    int getCurrentWave() const { return currentWave_; }
    int getMaxWaves() const { return maxWaves_; }
    int getPlayerHP() const { return playerHP_; }
    float getSP() const { return sp_; }
    float getElapsedTime() const { return waveTimer_; }
    int getStateInt() const { return static_cast<int>(state_); }
    int getSummonCostInt() const { return static_cast<int>(getSummonCost()); }

private:
    GameEngine& engine_;

    // Game state
    enum class State { Playing, WaveDelay, Victory, Defeat };
    State state_ = State::WaveDelay;

    // Core systems
    Grid grid_;
    WaveManager waveManager_;
    MergeSystem mergeSystem_;
    ObjectPool<Unit> unitPool_;
    ObjectPool<Enemy> enemyPool_;
    ObjectPool<Projectile> projectilePool_;
    SpatialHash<Enemy> enemySpatialHash_;

    // Enemy path waypoints
    std::vector<Vec2> pathWaypoints_;

    // Player state
    int playerHP_ = 20;
    float sp_ = 100.f;
    int summonCount_ = 0;
    int currentWave_ = 0;
    int maxWaves_ = 40;
    int difficulty_ = 0;

    // Deck (5 unit IDs)
    int deck_[5] = {0, 1, 2, 3, 4}; // default: first 5 units

    // Wave delay
    float waveDelayTimer_ = 0.f;
    static constexpr float WAVE_DELAY = 3.f;

    // Wave elapsed timer
    float waveTimer_ = 0.f;

    // SP economy
    float spRegenTimer_ = 0.f;
    static constexpr float SP_REGEN_RATE = 2.f; // per second

    // Sprite atlas (single texture for all rendering)
    SpriteAtlas atlas_;

    // Summon button area (bottom-right)
    static constexpr Rect SUMMON_BUTTON = {1080.f, 620.f, 180.f, 80.f};

    // Monster limit — defeat if active enemies reach this count
    static constexpr int MAX_ENEMY_COUNT = 100;

    // Boss round timer
    float bossTimer_ = 0.f;
    bool isBossRound_ = false;
    static constexpr float BOSS_TIME_LIMIT = 60.f;

    // Ability aura timer
    float auraTimer_ = 0.f;
    static constexpr float AURA_INTERVAL = 0.5f;

    // Tracking for results/achievements
    int killCount_ = 0;
    int mergeCount_ = 0;
    int hpLost_ = 0;
    std::set<int> unitTypesUsed_;

    // Methods
    void setupPath();
    void updateEnemies(float dt);
    void updateUnits(float dt);
    void updateProjectiles(float dt);
    void updateAbilities(float dt);
    void rebuildSpatialHash();
    void renderPath(SpriteBatch& batch);
    void renderHUD(SpriteBatch& batch);
    void checkWaveComplete();
    void startNextWave();
    void notifyBattleEnd(bool victory, int waveReached, int goldEarned, int trophyChange);
};

#endif // JAYGAME_BATTLESCENE_H
