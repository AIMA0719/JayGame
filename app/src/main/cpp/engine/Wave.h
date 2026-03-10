#ifndef JAYGAME_WAVE_H
#define JAYGAME_WAVE_H

#include <vector>
#include "MathTypes.h"
#include "ObjectPool.h"

class Enemy;

struct WaveConfig {
    int waveNumber;
    int enemyCount;
    float enemyHP;
    float enemySpeed;
    float enemyArmor;
    bool isBoss;
    int spReward;        // SP reward for clearing wave
    float spawnInterval; // seconds between enemy spawns
};

class WaveManager {
public:
    void init();
    void startWave(int waveNumber);
    void update(float dt, ObjectPool<Enemy>& enemies, const std::vector<Vec2>& path);

    bool isWaveComplete() const;    // all enemies spawned AND dead/exited
    bool isSpawning() const;
    int getCurrentWave() const;
    int getEnemiesRemaining() const;
    WaveConfig getWaveConfig(int wave) const;

    void onEnemyDefeated();
    void onEnemyEscaped();

    void setHPMultiplier(float multiplier) { hpMultiplier_ = multiplier; }

private:
    int currentWave_ = 0;
    int enemiesSpawned_ = 0;
    int enemiesDefeated_ = 0;
    int enemiesEscaped_ = 0;
    int totalEnemies_ = 0;
    float spawnTimer_ = 0.f;
    bool waveActive_ = false;
    float hpMultiplier_ = 1.f;
    WaveConfig currentConfig_{};
};

#endif // JAYGAME_WAVE_H
