#include "Wave.h"
#include "Enemy.h"

#include <cmath>
#include <algorithm>

void WaveManager::init() {
    currentWave_ = 0;
    enemiesSpawned_ = 0;
    enemiesDefeated_ = 0;
    enemiesEscaped_ = 0;
    totalEnemies_ = 0;
    spawnTimer_ = 0.f;
    waveActive_ = false;
    hpMultiplier_ = 1.f;
    currentConfig_ = {};
}

WaveConfig WaveManager::getWaveConfig(int wave) const {
    WaveConfig config{};
    config.waveNumber = wave;

    bool boss = (wave % 10 == 0) && (wave > 0);
    config.isBoss = boss;

    // HP: 100 * pow(1.12, wave-1) for normal; 5x for boss
    float baseHP = 100.f * std::pow(1.12f, static_cast<float>(wave - 1));
    config.enemyHP = boss ? baseHP * 5.f : baseHP;

    // Speed: 60 + wave*1.5 for normal; 0.6x for boss
    float baseSpeed = 60.f + static_cast<float>(wave) * 1.5f;
    config.enemySpeed = boss ? baseSpeed * 0.6f : baseSpeed;

    // Armor scales gently
    config.enemyArmor = static_cast<float>(wave) * 0.5f;

    // Enemy count: 8 + wave/2 for normal; 1 for boss
    config.enemyCount = boss ? 1 : (8 + wave / 2);

    // SP reward per enemy kill
    config.spReward = boss ? (5 + wave / 2) : (1 + wave / 10);

    // Spawn interval: 1.0s normal, decreasing by 0.01 per wave (min 0.3s)
    float interval = 1.0f - static_cast<float>(wave - 1) * 0.01f;
    config.spawnInterval = std::max(0.3f, interval);

    return config;
}

void WaveManager::startWave(int waveNumber) {
    currentWave_ = waveNumber;
    currentConfig_ = getWaveConfig(waveNumber);
    enemiesSpawned_ = 0;
    enemiesDefeated_ = 0;
    enemiesEscaped_ = 0;
    totalEnemies_ = currentConfig_.enemyCount;
    spawnTimer_ = 0.f; // spawn first enemy immediately
    waveActive_ = true;
}

void WaveManager::update(float dt, ObjectPool<Enemy>& enemies, const std::vector<Vec2>& path) {
    if (!waveActive_) return;
    if (enemiesSpawned_ >= totalEnemies_) return;

    spawnTimer_ -= dt;
    if (spawnTimer_ > 0.f) return;

    // Spawn an enemy
    Enemy* enemy = enemies.acquire();
    if (enemy) {
        enemy->init(currentConfig_.enemyHP * hpMultiplier_,
                    currentConfig_.enemySpeed,
                    currentConfig_.enemyArmor,
                    currentConfig_.isBoss,
                    currentConfig_.spReward);

        // Place at first waypoint
        if (!path.empty()) {
            enemy->position = path[0];
            enemy->prevPosition = path[0];
        }

        enemiesSpawned_++;
    }

    spawnTimer_ = currentConfig_.spawnInterval;
}

bool WaveManager::isWaveComplete() const {
    if (!waveActive_) return false;
    return (enemiesSpawned_ >= totalEnemies_) &&
           ((enemiesDefeated_ + enemiesEscaped_) >= totalEnemies_);
}

bool WaveManager::isSpawning() const {
    return waveActive_ && (enemiesSpawned_ < totalEnemies_);
}

int WaveManager::getCurrentWave() const {
    return currentWave_;
}

int WaveManager::getEnemiesRemaining() const {
    return totalEnemies_ - (enemiesDefeated_ + enemiesEscaped_);
}

void WaveManager::onEnemyDefeated() {
    enemiesDefeated_++;
}

void WaveManager::onEnemyEscaped() {
    enemiesEscaped_++;
}
