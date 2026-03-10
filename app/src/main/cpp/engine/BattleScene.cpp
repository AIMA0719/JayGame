#include "BattleScene.h"
#include "GameEngine.h"
#include "UnitData.h"
#include "ResultScene.h"
#include "PlayerData.h"
#include "Currency.h"
#include "SeasonPass.h"
#include "Achievement.h"
#include "SaveSystem.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <jni.h>

#include <cmath>
#include <cstdlib>
#include <algorithm>

// Static members
std::atomic<bool> BattleScene::summonRequested{false};
std::atomic<int> BattleScene::clickedTileIndex{-1};
std::atomic<int> BattleScene::mergeRequestUnitId{-1};
std::atomic<int> BattleScene::sellRequestTileIndex{-1};
std::atomic<int> BattleScene::upgradeRequestTileIndex{-1};
std::atomic<int> BattleScene::swapRequestFrom{-1};
std::atomic<int> BattleScene::swapRequestTo{-1};
std::atomic<int> BattleScene::relocateRequestTile{-1};
std::atomic<int> BattleScene::relocateRequestX{0};
std::atomic<int> BattleScene::relocateRequestY{0};

// JNI state push — cached references
static jclass g_bridgeClass = nullptr;
static jmethodID g_updateMethod = nullptr;
static jmethodID g_battleEndMethod = nullptr;
static jmethodID g_pushGridMethod = nullptr;
static jmethodID g_onSummonMethod = nullptr;
static jmethodID g_pushEnemiesMethod = nullptr;
static jmethodID g_pushProjectilesMethod = nullptr;
static jmethodID g_pushUnitsMethod = nullptr;
static jmethodID g_onUnitClickedMethod = nullptr;
static jmethodID g_onMergeMethod = nullptr;
static jmethodID g_onDamageMethod = nullptr;
static bool g_jniInitialized = false;
static float g_statePushTimer = 0.f;
static constexpr float STATE_PUSH_INTERVAL = 0.1f; // push 10 times/sec

static jclass findClassViaLoader(JNIEnv* env, jobject activity, const char* className) {
    jclass activityClass = env->GetObjectClass(activity);
    jmethodID getClassLoader = env->GetMethodID(activityClass, "getClassLoader",
        "()Ljava/lang/ClassLoader;");
    jobject classLoader = env->CallObjectMethod(activity, getClassLoader);
    env->DeleteLocalRef(activityClass);

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = env->GetMethodID(classLoaderClass, "loadClass",
        "(Ljava/lang/String;)Ljava/lang/Class;");
    env->DeleteLocalRef(classLoaderClass);

    jstring jClassName = env->NewStringUTF(className);
    auto cls = (jclass)env->CallObjectMethod(classLoader, loadClass, jClassName);
    env->DeleteLocalRef(jClassName);
    env->DeleteLocalRef(classLoader);

    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    return cls;
}

BattleScene::BattleScene(GameEngine& engine)
    : engine_(engine),
      unitPool_(64),
      enemyPool_(256),
      projectilePool_(512),
      enemySpatialHash_(64.f) {
}

void BattleScene::onEnter() {
    aout << "BattleScene::onEnter" << std::endl;

    auto assetManager = engine_.getApp()->activity->assetManager;

    // Load sprite atlas (single texture for all rendering)
    if (!atlas_.load(assetManager)) {
        aout << "ERROR: Failed to load sprite atlas!" << std::endl;
        return;
    }

    setupPath();
    grid_.clear();
    waveManager_.init();

    // Reset player state (no HP system — lose only by 100 monsters)
    sp_ = 100.f;
    summonCount_ = 0;
    currentWave_ = 0;
    killCount_ = 0;
    mergeCount_ = 0;
    waveTimer_ = 0.f;
    bossTimer_ = 0.f;
    isBossRound_ = false;
    gridPushTimer_ = 0.f;
    unitTypesUsed_.clear();

    // Reset atomic flags
    clickedTileIndex.store(-1, std::memory_order_release);
    mergeRequestUnitId.store(-1, std::memory_order_release);
    sellRequestTileIndex.store(-1, std::memory_order_release);
    upgradeRequestTileIndex.store(-1, std::memory_order_release);
    swapRequestFrom.store(-1, std::memory_order_release);
    swapRequestTo.store(-1, std::memory_order_release);
    relocateRequestTile.store(-1, std::memory_order_release);
    relocateRequestX.store(0, std::memory_order_release);
    relocateRequestY.store(0, std::memory_order_release);

    // Copy deck and stage data from PlayerData
    auto& pd = PlayerData::get();
    for (int i = 0; i < 5; i++) {
        deck_[i] = pd.deck[i];
    }
    maxWaves_ = pd.stageMaxWaves;
    difficulty_ = pd.difficulty;

    // Apply difficulty HP multiplier: 0=easy(0.8x), 1=normal(1.0x), 2=hard(1.5x)
    {
        static const float diffMultipliers[] = {0.8f, 1.0f, 1.5f};
        int idx = (difficulty_ >= 0 && difficulty_ <= 2) ? difficulty_ : 1;
        waveManager_.setHPMultiplier(diffMultipliers[idx]);
    }

    // Start with wave delay before wave 1
    state_ = State::WaveDelay;
    waveDelayTimer_ = WAVE_DELAY;

    // Initialize JNI bridge for BattleBridge state push
    if (!g_jniInitialized) {
        JNIEnv* env = nullptr;
        engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
        if (env) {
            jclass localCls = findClassViaLoader(env, engine_.getApp()->activity->javaGameActivity,
                "com.example.jaygame.bridge.BattleBridge");
            if (localCls) {
                g_bridgeClass = (jclass)env->NewGlobalRef(localCls);
                env->DeleteLocalRef(localCls);
                g_updateMethod = env->GetStaticMethodID(g_bridgeClass, "updateState",
                    "(IIIIFFIIIIF)V");
                g_battleEndMethod = env->GetStaticMethodID(g_bridgeClass, "onBattleEnd",
                    "(ZIIIIII)V");
                g_pushGridMethod = env->GetStaticMethodID(g_bridgeClass, "updateGridState",
                    "([I[I[I[Z[I)V");
                g_onSummonMethod = env->GetStaticMethodID(g_bridgeClass, "onSummonResult",
                    "(II)V");
                g_pushEnemiesMethod = env->GetStaticMethodID(g_bridgeClass, "updateEnemyPositions",
                    "([F[F[I[FI)V");
                g_pushProjectilesMethod = env->GetStaticMethodID(g_bridgeClass, "updateProjectiles",
                    "([F[F[F[F[II)V");
                g_onDamageMethod = env->GetStaticMethodID(g_bridgeClass, "onDamageDealt",
                    "(FFIZ)V");
                g_pushUnitsMethod = env->GetStaticMethodID(g_bridgeClass, "updateUnitPositions",
                    "([F[F[I[I[I[Z[II)V");
                g_onUnitClickedMethod = env->GetStaticMethodID(g_bridgeClass, "onUnitClicked",
                    "(IIIIZI)V");
                g_onMergeMethod = env->GetStaticMethodID(g_bridgeClass, "onMergeComplete",
                    "(IZI)V");
                g_jniInitialized = (g_updateMethod != nullptr && g_battleEndMethod != nullptr);
                aout << "BattleBridge JNI initialized: " << g_jniInitialized
                     << " gridMethod: " << (g_pushGridMethod != nullptr)
                     << " unitsMethod: " << (g_pushUnitsMethod != nullptr) << std::endl;
            }
        }
    }
    g_statePushTimer = 0.f;

    aout << "BattleScene initialized. Path waypoints: " << pathWaypoints_.size() << std::endl;
}

void BattleScene::onExit() {
    aout << "BattleScene::onExit" << std::endl;
    unitPool_.clear();
    enemyPool_.clear();
    projectilePool_.clear();
    grid_.clear();
    // SpriteAtlas cleans up via shared_ptr automatically
}

void BattleScene::setupPath() {
    // Donut path: rectangular loop around the unit grid
    // Grid is at (290, 190) size (700, 340)
    // Path runs clockwise with 80px margin outside grid
    pathWaypoints_.clear();
    float margin = 80.f;
    float left   = Grid::GRID_X - margin;          // 210
    float right  = Grid::GRID_X + Grid::GRID_W + margin;  // 1070
    float top    = Grid::GRID_Y - margin;           // 110
    float bottom = Grid::GRID_Y + Grid::GRID_H + margin;  // 610

    // Clockwise rectangular loop
    pathWaypoints_.push_back({left,  top});     // top-left
    pathWaypoints_.push_back({right, top});     // top-right
    pathWaypoints_.push_back({right, bottom});  // bottom-right
    pathWaypoints_.push_back({left,  bottom});  // bottom-left
    pathWaypoints_.push_back({left,  top});     // back to start (close loop)
}

void BattleScene::onUpdate(float dt) {
    // Check summon request from Compose
    if (summonRequested.exchange(false, std::memory_order_acq_rel)) {
        summonUnit();
    }

    // Check click/merge/sell requests from Compose
    {
        int tile = clickedTileIndex.exchange(-1, std::memory_order_acq_rel);
        if (tile >= 0) handleTileClick(tile);
    }
    {
        int tile = mergeRequestUnitId.exchange(-1, std::memory_order_acq_rel);
        if (tile >= 0) handleMergeRequest(tile);
    }
    {
        int tile = sellRequestTileIndex.exchange(-1, std::memory_order_acq_rel);
        if (tile >= 0) handleSellRequest(tile);
    }
    {
        int tile = upgradeRequestTileIndex.exchange(-1, std::memory_order_acq_rel);
        if (tile >= 0) handleUpgradeRequest(tile);
    }
    {
        int from = swapRequestFrom.exchange(-1, std::memory_order_acq_rel);
        int to = swapRequestTo.exchange(-1, std::memory_order_acq_rel);
        if (from >= 0 && to >= 0) handleSwapRequest(from, to);
    }
    {
        int tile = relocateRequestTile.exchange(-1, std::memory_order_acq_rel);
        if (tile >= 0) {
            float nx = relocateRequestX.load(std::memory_order_acquire) / 10000.f;
            float ny = relocateRequestY.load(std::memory_order_acquire) / 10000.f;
            handleRelocateUnit(tile, nx, ny);
        }
    }

    // Push state to Compose HUD periodically
    g_statePushTimer -= dt;
    if (g_statePushTimer <= 0.f && g_jniInitialized) {
        g_statePushTimer = STATE_PUSH_INTERVAL;
        JNIEnv* env = nullptr;
        engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
        if (env && g_bridgeClass && g_updateMethod) {
            // Count active enemies for JNI push
            int activeEnemyCount = 0;
            enemyPool_.forEach([&](Enemy& enemy) {
                if (enemy.active) activeEnemyCount++;
            });

            env->CallStaticVoidMethod(g_bridgeClass, g_updateMethod,
                currentWave_, maxWaves_,
                20, 20,  // HP always 20 (no HP system)
                sp_, waveTimer_,
                static_cast<int>(state_),
                static_cast<int>(getSummonCost()),
                activeEnemyCount,
                isBossRound_ ? 1 : 0,
                bossTimer_);
        }
    }

    // Push grid state periodically
    gridPushTimer_ -= dt;
    if (gridPushTimer_ <= 0.f) {
        gridPushTimer_ = GRID_PUSH_INTERVAL;
        pushGridState();
        pushEnemyPositions();
        pushProjectilePositions();
        pushUnitPositions();
    }

    switch (state_) {
        case State::WaveDelay: {
            waveDelayTimer_ -= dt;

            // SP regen during delay
            sp_ += SP_REGEN_RATE * dt;

            if (waveDelayTimer_ <= 0.f) {
                startNextWave();
            }
            break;
        }

        case State::Playing: {
            // SP regen
            sp_ += SP_REGEN_RATE * dt;
            waveTimer_ += dt;

            // Update wave spawner
            waveManager_.update(dt, enemyPool_, pathWaypoints_);

            // Update enemies
            updateEnemies(dt);

            // Check active enemy count — defeat if >= MAX_ENEMY_COUNT
            {
                int activeEnemyCount = 0;
                enemyPool_.forEach([&](Enemy& enemy) {
                    if (enemy.active) activeEnemyCount++;
                });
                if (activeEnemyCount >= MAX_ENEMY_COUNT) {
                    state_ = State::Defeat;
                    aout << "DEFEAT! Too many enemies (" << activeEnemyCount << ")" << std::endl;
                    notifyBattleEnd(false, currentWave_, currentWave_ * 10, -15);
                    break;
                }
            }

            // Boss round timer — defeat if time runs out
            if (isBossRound_) {
                bossTimer_ -= dt;
                if (bossTimer_ <= 0.f) {
                    bossTimer_ = 0.f;
                    state_ = State::Defeat;
                    aout << "DEFEAT! Boss timer expired on wave " << currentWave_ << std::endl;
                    notifyBattleEnd(false, currentWave_, currentWave_ * 10, -15);
                    break;
                }
            }

            // Rebuild spatial hash for enemy queries
            rebuildSpatialHash();

            // Update units (target acquisition + firing)
            updateUnits(dt);

            // Update projectiles (handles ability on-hit effects)
            updateProjectiles(dt);

            // Update aura abilities (buff/shield)
            updateAbilities(dt);

            // Check wave completion
            checkWaveComplete();
            break;
        }

        case State::Victory:
        case State::Defeat:
            // Game over states - no updates
            break;
    }
}

void BattleScene::updateEnemies(float dt) {
    enemyPool_.forEach([&](Enemy& enemy) {
        if (!enemy.active) return;

        enemy.update(dt, pathWaypoints_);

        if (enemy.isDead()) {
            // Grant SP reward
            sp_ += static_cast<float>(enemy.spReward);
            waveManager_.onEnemyDefeated();
            killCount_++;
            enemy.active = false;
            enemyPool_.release(&enemy);
        }
        // Enemies loop automatically in Enemy::update() — no reachedEnd check needed
        // Defeat condition is checked separately via active enemy count >= 100
    });
}

void BattleScene::updateUnits(float dt) {
    unitPool_.forEach([&](Unit& unit) {
        if (!unit.active) return;
        unit.update(dt, enemyPool_, projectilePool_, enemySpatialHash_);
    });
}

void BattleScene::updateProjectiles(float dt) {
    JNIEnv* env = nullptr;
    if (g_jniInitialized && g_onDamageMethod) {
        engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    }

    projectilePool_.forEach([&](Projectile& proj) {
        if (!proj.active) return;

        bool wasPrevActive = proj.active;
        proj.update(dt);

        if (!proj.active && wasPrevActive) {
            // Projectile just finished
            if (proj.hasHit() && proj.getHitTarget() && proj.sourceUnitId >= 0) {
                // Apply damage with magic flag
                Enemy* hitEnemy = proj.getHitTarget();
                if (hitEnemy->active && !hitEnemy->isDead()) {
                    hitEnemy->takeDamage(proj.damage, proj.isMagic);

                    // Notify Compose of damage hit for visual effect
                    if (env && g_onDamageMethod) {
                        constexpr float SW = 1280.f;
                        constexpr float SH = 720.f;
                        env->CallStaticVoidMethod(g_bridgeClass, g_onDamageMethod,
                            hitEnemy->position.x / SW,
                            hitEnemy->position.y / SH,
                            static_cast<jint>(proj.damage),
                            static_cast<jboolean>(false));
                    }

                    // Find source unit to apply ability effects
                    unitPool_.forEach([&](Unit& unit) {
                        if (unit.active && unit.unitDefId == proj.sourceUnitId) {
                            Ability::onProjectileHit(unit, *hitEnemy, proj.damage,
                                                     enemyPool_, projectilePool_,
                                                     enemySpatialHash_);
                        }
                    });
                }
            }
            projectilePool_.release(&proj);
        }
    });
}

void BattleScene::updateAbilities(float dt) {
    auraTimer_ -= dt;
    if (auraTimer_ > 0.f) return;
    auraTimer_ = AURA_INTERVAL;

    // Apply aura effects from buff/shield units
    unitPool_.forEach([&](Unit& unit) {
        if (!unit.active) return;
        Ability::applyAuraEffects(unit, grid_, unitPool_);
        Ability::applyShieldAura(unit, grid_, unitPool_);
    });
}

void BattleScene::rebuildSpatialHash() {
    enemySpatialHash_.clear();
    enemyPool_.forEach([&](Enemy& enemy) {
        if (enemy.active && !enemy.isDead()) {
            enemySpatialHash_.insert(&enemy, enemy.getBounds());
        }
    });
}

void BattleScene::summonUnit() {
    float cost = getSummonCost();
    if (sp_ < cost) return;
    if (grid_.getEmptyCellCount() == 0) return;

    // Roll a random unit from the deck
    const UnitDef& def = rollRandomUnit(deck_, 5);

    // Find a random empty cell
    int row, col;
    if (!grid_.getRandomEmptyCell(row, col)) return;

    // Deduct SP
    sp_ -= cost;
    summonCount_++;

    // Create unit
    Unit* unit = unitPool_.acquire();
    if (!unit) return;

    Vec2 cellPos = grid_.cellCenter(row, col);
    unit->init(def.id, cellPos);
    unit->gridRow = row;
    unit->gridCol = col;
    unit->homePosition = cellPos;

    grid_.placeUnit(row, col, unit);
    unitTypesUsed_.insert(def.id);

    aout << "Summoned " << def.name << " at (" << row << "," << col
         << ") cost=" << cost << " SP=" << sp_ << std::endl;

    notifySummonResult(def.id, def.id / 5);
}

float BattleScene::getSummonCost() const {
    return 10.f;
}

void BattleScene::checkWaveComplete() {
    if (waveManager_.isWaveComplete()) {
        aout << "Wave " << currentWave_ << " complete!" << std::endl;

        if (currentWave_ >= maxWaves_) {
            state_ = State::Victory;
            aout << "VICTORY! All waves cleared!" << std::endl;
            notifyBattleEnd(true, currentWave_, currentWave_ * 15, 25);
        } else {
            state_ = State::WaveDelay;
            waveDelayTimer_ = WAVE_DELAY;
        }
    }
}

void BattleScene::notifyBattleEnd(bool victory, int waveReached, int goldEarned, int trophyChange) {
    if (!g_jniInitialized || !g_battleEndMethod) {
        aout << "BattleBridge JNI not ready — cannot notify battle end" << std::endl;
        return;
    }

    // Apply rewards to PlayerData + save (same as ResultScene::onEnter did)
    auto& pd = PlayerData::get();
    Currency::addGold(goldEarned);
    Currency::addTrophies(trophyChange);
    int cardReward = waveReached / 5;
    for (int i = 0; i < cardReward; i++) {
        // Only give cards for LOW grade (summonable) units: IDs 0-4
        int unitId = std::rand() % 5;
        pd.units[unitId].cards++;
    }
    if (victory) pd.totalWins++; else pd.totalLosses++;
    if (waveReached > pd.highestWave) pd.highestWave = waveReached;
    pd.totalKills += killCount_;
    pd.totalMerges += mergeCount_;
    if (victory) pd.wonWithoutDamage = true;  // no HP system — all victories are "without damage"
    if (victory && unitTypesUsed_.size() <= 1) pd.wonWithSingleType = true;
    SeasonPass::addBattleXP();
    AchievementSystem::get().checkAndUnlock();
    SaveSystem::get().save();

    // Notify Compose via JNI
    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (env) {
        env->CallStaticVoidMethod(g_bridgeClass, g_battleEndMethod,
            static_cast<jboolean>(victory),
            waveReached, goldEarned, trophyChange,
            killCount_, mergeCount_, cardReward);
    }

    aout << "Battle end notified to Compose: " << (victory ? "VICTORY" : "DEFEAT") << std::endl;
}

void BattleScene::startNextWave() {
    currentWave_++;
    aout << "Starting wave " << currentWave_ << std::endl;
    waveManager_.startWave(currentWave_);
    waveTimer_ = 0.f;
    state_ = State::Playing;

    // Boss round detection
    isBossRound_ = (currentWave_ % 10 == 0) && (currentWave_ > 0);
    if (isBossRound_) {
        // Boss timer: 60 - (currentWave / 10) * 5, minimum 30s
        float bossTime = 60.f - static_cast<float>(currentWave_ / 10) * 5.f;
        bossTimer_ = std::max(30.f, bossTime);
        aout << "BOSS ROUND! Time limit: " << bossTimer_ << "s" << std::endl;
    }
}

// ---- Rendering ----

void BattleScene::onRender(float alpha, SpriteBatch& batch) {
    if (!atlas_.getTexture()) return;

    batch.begin();

    // 0. Render grass background — dark tint to match neon theme
    {
        const auto& grassTile = atlas_.getTile("grass");
        const auto& tex = *atlas_.getTexture();
        for (float y = 0.f; y < 720.f; y += 64.f) {
            for (float x = 0.f; x < 1280.f; x += 64.f) {
                batch.draw(tex, x, y, 64.f, 64.f,
                           grassTile.uvRect.x, grassTile.uvRect.y,
                           grassTile.uvRect.w, grassTile.uvRect.h,
                           0.08f, 0.08f, 0.15f, 0.9f);
            }
        }
    }

    // 1. Render path
    renderPath(batch);

    // 2. Enemies are now rendered by Compose overlay via pushEnemyPositions()

    // 3. Render projectiles
    projectilePool_.forEach([&](Projectile& proj) {
        if (proj.active) {
            proj.render(alpha, batch, atlas_);
        }
    });

    // 4. Wave delay overlay
    if (state_ == State::WaveDelay) {
        renderHUD(batch);
    }

    batch.end();
}

void BattleScene::renderPath(SpriteBatch& batch) {
    const auto& tex = *atlas_.getTexture();
    const auto& pathH = atlas_.getTile("path_h");
    const auto& pathV = atlas_.getTile("path_v");
    constexpr float PATH_WIDTH = 24.f; // wider path for better visibility

    for (size_t i = 0; i + 1 < pathWaypoints_.size(); i++) {
        Vec2 a = pathWaypoints_[i];
        Vec2 b = pathWaypoints_[i + 1];

        if (std::abs(a.y - b.y) < 1.f) {
            // Horizontal segment
            float x0 = std::min(a.x, b.x);
            float w = std::abs(b.x - a.x);
            batch.draw(tex,
                       x0, a.y - PATH_WIDTH * 0.5f, w, PATH_WIDTH,
                       pathH.uvRect.x, pathH.uvRect.y, pathH.uvRect.w, pathH.uvRect.h,
                       0.12f, 0.15f, 0.3f, 0.5f);
        } else {
            // Vertical segment
            float y0 = std::min(a.y, b.y);
            float h = std::abs(b.y - a.y);
            batch.draw(tex,
                       a.x - PATH_WIDTH * 0.5f, y0, PATH_WIDTH, h,
                       pathV.uvRect.x, pathV.uvRect.y, pathV.uvRect.w, pathV.uvRect.h,
                       0.12f, 0.15f, 0.3f, 0.5f);
        }
    }
}

void BattleScene::renderHUD(SpriteBatch& batch) {
    // HUD rendering is now handled entirely by Compose overlay.
    // This method is kept as a stub for wave delay overlay rendered in C++.
    if (state_ == State::WaveDelay) {
        auto& text = engine_.getTextRenderer();
        const auto& tex = *atlas_.getTexture();
        const auto& panel = atlas_.getHud("panel");
        char buf[64];

        float pulse = 0.5f + 0.5f * std::sin(waveDelayTimer_ * 4.f);
        batch.draw(tex, 440.f, 330.f, 400.f, 60.f,
                   panel.uvRect.x, panel.uvRect.y, panel.uvRect.w, panel.uvRect.h,
                   0.6f, 0.6f, 0.8f, 0.9f);
        snprintf(buf, sizeof(buf), "NEXT WAVE IN %.0f", waveDelayTimer_ + 1.f);
        text.drawText(batch, buf, 640.f, 345.f, 3.f,
                      {0.5f, 0.8f, 1.f, pulse}, TextAlign::Center);
    }
}

void BattleScene::onInput(const InputEvent& event) {
    if (state_ == State::Victory || state_ == State::Defeat) return;

    // All interaction is now click-based via Compose overlay (JNI).
    // This method handles raw touch events on the C++ rendering surface.
    if (event.action == InputAction::Tap) {
        Vec2 pos = event.worldPos;
        int row, col;
        if (grid_.getCellAt(pos, row, col)) {
            int tileIndex = row * Grid::COLS + col;
            handleTileClick(tileIndex);
        }
    }
}

void BattleScene::summonUnitAt(int tileIndex) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col) || !grid_.isEmpty(row, col)) return;

    float cost = getSummonCost();
    if (sp_ < cost) return;

    const UnitDef& def = rollRandomUnit(deck_, 5);
    sp_ -= cost;
    summonCount_++;

    Unit* unit = unitPool_.acquire();
    if (!unit) return;

    Vec2 cellPos = grid_.cellCenter(row, col);
    unit->init(def.id, cellPos);
    unit->gridRow = row;
    unit->gridCol = col;
    unit->homePosition = cellPos;
    grid_.placeUnit(row, col, unit);
    unitTypesUsed_.insert(def.id);

    aout << "Summoned " << def.name << " at tile " << tileIndex
         << " cost=" << cost << " SP=" << sp_ << std::endl;

    notifySummonResult(def.id, def.id / 5);
}

void BattleScene::handleTileClick(int tileIndex) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col)) return;

    Unit* unit = grid_.getUnit(row, col);
    if (unit) {
        // Notify Compose to show unit detail popup
        notifyUnitClicked(tileIndex, unit);
        const UnitDef& def = getUnitDef(unit->unitDefId);
        aout << "Clicked unit: " << def.name << " lv" << unit->level
             << " ATK=" << unit->getDamage() << std::endl;
    } else {
        // Empty tile — summon
        summonUnitAt(tileIndex);
    }
}

void BattleScene::handleMergeRequest(int tileIndex) {
    auto result = mergeSystem_.trySmartMerge(tileIndex, grid_, unitPool_);
    if (result.success) {
        mergeCount_++;
        notifyMergeComplete(tileIndex, result.lucky, result.resultUnitId);
        aout << "Smart merge! Result unit: " << result.resultUnitId
             << (result.lucky ? " (LUCKY!)" : "") << std::endl;
    }
}

void BattleScene::handleSellRequest(int tileIndex) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col)) return;

    Unit* unit = grid_.removeUnit(row, col);
    if (unit) {
        // Refund some SP based on unit grade
        int grade = unit->unitDefId / 5;
        float refund = 5.f + grade * 10.f;
        sp_ += refund;
        unit->active = false;
        unitPool_.release(unit);
        aout << "Sold unit at tile " << tileIndex << " refund=" << refund << std::endl;
    }
}

void BattleScene::handleUpgradeRequest(int tileIndex) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col)) return;

    Unit* unit = grid_.getUnit(row, col);
    if (!unit || !unit->active) return;

    // Max level is 7
    if (unit->level >= 7) return;

    // Get upgrade cost
    int costIndex = unit->level - 1; // level 1 -> index 0
    if (costIndex < 0 || costIndex >= 6) return;
    float cost = UPGRADE_COSTS[costIndex];

    if (sp_ < cost) return;

    sp_ -= cost;
    unit->level++;

    aout << "Upgraded unit at tile " << tileIndex << " to level " << unit->level
         << " cost=" << cost << " SP=" << sp_ << std::endl;
}

void BattleScene::handleSwapRequest(int fromTile, int toTile) {
    if (fromTile < 0 || fromTile >= Grid::TOTAL_CELLS || toTile < 0 || toTile >= Grid::TOTAL_CELLS) return;
    if (fromTile == toTile) return;

    int fromRow = fromTile / Grid::COLS, fromCol = fromTile % Grid::COLS;
    int toRow = toTile / Grid::COLS, toCol = toTile % Grid::COLS;

    Unit* unitA = grid_.getUnit(fromRow, fromCol);
    Unit* unitB = grid_.getUnit(toRow, toCol);

    if (!unitA) return;

    grid_.removeUnit(fromRow, fromCol);
    if (unitB) grid_.removeUnit(toRow, toCol);

    // Swap grid slots — units walk to new home positions naturally
    grid_.placeUnit(toRow, toCol, unitA);
    unitA->homePosition = grid_.cellCenter(toRow, toCol);
    unitA->gridRow = toRow;
    unitA->gridCol = toCol;

    if (unitB) {
        grid_.placeUnit(fromRow, fromCol, unitB);
        unitB->homePosition = grid_.cellCenter(fromRow, fromCol);
        unitB->gridRow = fromRow;
        unitB->gridCol = fromCol;
    }

    aout << "Swapped tile " << fromTile << " <-> " << toTile << std::endl;
}

void BattleScene::pushGridState() {
    if (!g_jniInitialized || !g_pushGridMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    static constexpr int N = Grid::TOTAL_CELLS;
    jintArray unitIds = env->NewIntArray(N);
    jintArray grades = env->NewIntArray(N);
    jintArray families = env->NewIntArray(N);
    jbooleanArray canMerge = env->NewBooleanArray(N);
    jintArray levels = env->NewIntArray(N);

    int ids[N], grd[N], fam[N], lvls[N];
    jboolean merge[N];

    for (int i = 0; i < N; i++) {
        int r = i / Grid::COLS, c = i % Grid::COLS;
        Unit* u = grid_.getUnit(r, c);
        if (u && u->active) {
            ids[i] = u->unitDefId;
            grd[i] = u->unitDefId / 5;  // grade
            fam[i] = u->unitDefId % 5;  // family
            lvls[i] = u->level;
            // Can merge if 3+ same family+grade exist
            int count = 0;
            int grade = u->unitDefId / 5;
            int family = u->unitDefId % 5;
            for (int j = 0; j < N; j++) {
                int jr = j / Grid::COLS, jc = j % Grid::COLS;
                Unit* uj = grid_.getUnit(jr, jc);
                if (uj && uj->active && uj->unitDefId / 5 == grade && uj->unitDefId % 5 == family)
                    count++;
            }
            merge[i] = (count >= 3 && grade < 4) ? JNI_TRUE : JNI_FALSE;
        } else {
            ids[i] = -1;
            grd[i] = -1;
            fam[i] = -1;
            lvls[i] = 0;
            merge[i] = JNI_FALSE;
        }
    }

    env->SetIntArrayRegion(unitIds, 0, N, ids);
    env->SetIntArrayRegion(grades, 0, N, grd);
    env->SetIntArrayRegion(families, 0, N, fam);
    env->SetBooleanArrayRegion(canMerge, 0, N, merge);
    env->SetIntArrayRegion(levels, 0, N, lvls);

    env->CallStaticVoidMethod(g_bridgeClass, g_pushGridMethod, unitIds, grades, families, canMerge, levels);

    env->DeleteLocalRef(unitIds);
    env->DeleteLocalRef(grades);
    env->DeleteLocalRef(families);
    env->DeleteLocalRef(canMerge);
    env->DeleteLocalRef(levels);
}

void BattleScene::notifySummonResult(int unitDefId, int grade) {
    if (!g_jniInitialized || !g_onSummonMethod) return;
    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (env && g_bridgeClass) {
        env->CallStaticVoidMethod(g_bridgeClass, g_onSummonMethod, unitDefId, grade);
    }
}

void BattleScene::pushProjectilePositions() {
    if (!g_jniInitialized || !g_pushProjectilesMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    int count = 0;
    projectilePool_.forEach([&](Projectile& proj) {
        if (proj.active) count++;
    });

    if (count == 0) {
        jfloatArray emptyF = env->NewFloatArray(0);
        jintArray emptyI = env->NewIntArray(0);
        env->CallStaticVoidMethod(g_bridgeClass, g_pushProjectilesMethod,
            emptyF, emptyF, emptyF, emptyF, emptyI, 0);
        env->DeleteLocalRef(emptyF);
        env->DeleteLocalRef(emptyI);
        return;
    }

    constexpr float SCREEN_W = 1280.f;
    constexpr float SCREEN_H = 720.f;

    std::vector<float> srcXs(count), srcYs(count), dstXs(count), dstYs(count);
    std::vector<int> types(count);
    int idx = 0;

    projectilePool_.forEach([&](Projectile& proj) {
        if (!proj.active || idx >= count) return;
        srcXs[idx] = proj.position.x / SCREEN_W;
        srcYs[idx] = proj.position.y / SCREEN_H;
        // Target position (where projectile is heading)
        if (proj.target && proj.target->active) {
            dstXs[idx] = proj.target->position.x / SCREEN_W;
            dstYs[idx] = proj.target->position.y / SCREEN_H;
        } else {
            dstXs[idx] = srcXs[idx];
            dstYs[idx] = srcYs[idx];
        }
        types[idx] = proj.projType;
        idx++;
    });

    jfloatArray jsrcX = env->NewFloatArray(count);
    jfloatArray jsrcY = env->NewFloatArray(count);
    jfloatArray jdstX = env->NewFloatArray(count);
    jfloatArray jdstY = env->NewFloatArray(count);
    jintArray jtypes = env->NewIntArray(count);

    env->SetFloatArrayRegion(jsrcX, 0, count, srcXs.data());
    env->SetFloatArrayRegion(jsrcY, 0, count, srcYs.data());
    env->SetFloatArrayRegion(jdstX, 0, count, dstXs.data());
    env->SetFloatArrayRegion(jdstY, 0, count, dstYs.data());
    env->SetIntArrayRegion(jtypes, 0, count, types.data());

    env->CallStaticVoidMethod(g_bridgeClass, g_pushProjectilesMethod,
        jsrcX, jsrcY, jdstX, jdstY, jtypes, count);

    env->DeleteLocalRef(jsrcX);
    env->DeleteLocalRef(jsrcY);
    env->DeleteLocalRef(jdstX);
    env->DeleteLocalRef(jdstY);
    env->DeleteLocalRef(jtypes);
}

void BattleScene::pushEnemyPositions() {
    if (!g_jniInitialized || !g_pushEnemiesMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    // Count active enemies first
    int count = 0;
    enemyPool_.forEach([&](Enemy& enemy) {
        if (enemy.active && !enemy.isDead()) count++;
    });

    if (count == 0) {
        // Push empty
        jfloatArray emptyF = env->NewFloatArray(0);
        jintArray emptyI = env->NewIntArray(0);
        env->CallStaticVoidMethod(g_bridgeClass, g_pushEnemiesMethod,
            emptyF, emptyF, emptyI, emptyF, 0);
        env->DeleteLocalRef(emptyF);
        env->DeleteLocalRef(emptyI);
        return;
    }

    // Allocate arrays
    std::vector<float> xs(count), ys(count), hpRatios(count);
    std::vector<int> types(count);
    int idx = 0;

    // Screen dimensions for normalization
    constexpr float SCREEN_W = 1280.f;
    constexpr float SCREEN_H = 720.f;

    enemyPool_.forEach([&](Enemy& enemy) {
        if (!enemy.active || enemy.isDead() || idx >= count) return;
        xs[idx] = enemy.position.x / SCREEN_W;  // normalize 0-1
        ys[idx] = enemy.position.y / SCREEN_H;  // normalize 0-1
        types[idx] = enemy.isBoss ? 99 : enemy.enemyType;
        hpRatios[idx] = (enemy.maxHp > 0.f) ? (enemy.hp / enemy.maxHp) : 0.f;
        idx++;
    });

    jfloatArray jxs = env->NewFloatArray(count);
    jfloatArray jys = env->NewFloatArray(count);
    jintArray jtypes = env->NewIntArray(count);
    jfloatArray jhp = env->NewFloatArray(count);

    env->SetFloatArrayRegion(jxs, 0, count, xs.data());
    env->SetFloatArrayRegion(jys, 0, count, ys.data());
    env->SetIntArrayRegion(jtypes, 0, count, types.data());
    env->SetFloatArrayRegion(jhp, 0, count, hpRatios.data());

    env->CallStaticVoidMethod(g_bridgeClass, g_pushEnemiesMethod,
        jxs, jys, jtypes, jhp, count);

    env->DeleteLocalRef(jxs);
    env->DeleteLocalRef(jys);
    env->DeleteLocalRef(jtypes);
    env->DeleteLocalRef(jhp);
}

void BattleScene::pushUnitPositions() {
    if (!g_jniInitialized || !g_pushUnitsMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    // Count active units
    int count = 0;
    for (int i = 0; i < Grid::TOTAL_CELLS; i++) {
        int r = i / Grid::COLS, c = i % Grid::COLS;
        Unit* u = grid_.getUnit(r, c);
        if (u && u->active) count++;
    }

    if (count == 0) {
        jfloatArray emptyF = env->NewFloatArray(0);
        jintArray emptyI = env->NewIntArray(0);
        jbooleanArray emptyB = env->NewBooleanArray(0);
        env->CallStaticVoidMethod(g_bridgeClass, g_pushUnitsMethod,
            emptyF, emptyF, emptyI, emptyI, emptyI, emptyB, emptyI, 0);
        env->DeleteLocalRef(emptyF);
        env->DeleteLocalRef(emptyI);
        env->DeleteLocalRef(emptyB);
        return;
    }

    constexpr float SCREEN_W = 1280.f;
    constexpr float SCREEN_H = 720.f;

    std::vector<float> xs(count), ys(count);
    std::vector<int> defIds(count), grades(count), levels(count), tiles(count);
    std::vector<jboolean> attacking(count);
    int idx = 0;

    for (int i = 0; i < Grid::TOTAL_CELLS && idx < count; i++) {
        int r = i / Grid::COLS, c = i % Grid::COLS;
        Unit* u = grid_.getUnit(r, c);
        if (u && u->active) {
            xs[idx] = u->position.x / SCREEN_W;
            ys[idx] = u->position.y / SCREEN_H;
            defIds[idx] = u->unitDefId;
            grades[idx] = u->unitDefId / 5;
            levels[idx] = u->level;
            attacking[idx] = u->attacking_ ? JNI_TRUE : JNI_FALSE;
            tiles[idx] = i;
            idx++;
        }
    }

    jfloatArray jxs = env->NewFloatArray(count);
    jfloatArray jys = env->NewFloatArray(count);
    jintArray jDefIds = env->NewIntArray(count);
    jintArray jGrades = env->NewIntArray(count);
    jintArray jLevels = env->NewIntArray(count);
    jbooleanArray jAttacking = env->NewBooleanArray(count);
    jintArray jTiles = env->NewIntArray(count);

    env->SetFloatArrayRegion(jxs, 0, count, xs.data());
    env->SetFloatArrayRegion(jys, 0, count, ys.data());
    env->SetIntArrayRegion(jDefIds, 0, count, defIds.data());
    env->SetIntArrayRegion(jGrades, 0, count, grades.data());
    env->SetIntArrayRegion(jLevels, 0, count, levels.data());
    env->SetBooleanArrayRegion(jAttacking, 0, count, attacking.data());
    env->SetIntArrayRegion(jTiles, 0, count, tiles.data());

    env->CallStaticVoidMethod(g_bridgeClass, g_pushUnitsMethod,
        jxs, jys, jDefIds, jGrades, jLevels, jAttacking, jTiles, count);

    env->DeleteLocalRef(jxs);
    env->DeleteLocalRef(jys);
    env->DeleteLocalRef(jDefIds);
    env->DeleteLocalRef(jGrades);
    env->DeleteLocalRef(jLevels);
    env->DeleteLocalRef(jAttacking);
    env->DeleteLocalRef(jTiles);
}

void BattleScene::notifyUnitClicked(int tileIndex, Unit* unit) {
    if (!g_jniInitialized || !g_onUnitClickedMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    int grade = unit->unitDefId / 5;
    int family = unit->unitDefId % 5;

    // Check if this unit can merge (3+ same family+grade)
    int mergeCount = 0;
    for (int i = 0; i < Grid::TOTAL_CELLS; i++) {
        int r = i / Grid::COLS, c = i % Grid::COLS;
        Unit* u = grid_.getUnit(r, c);
        if (u && u->active && u->unitDefId / 5 == grade && u->unitDefId % 5 == family)
            mergeCount++;
    }
    bool canMerge = (mergeCount >= 3 && grade < 4);

    env->CallStaticVoidMethod(g_bridgeClass, g_onUnitClickedMethod,
        tileIndex, unit->unitDefId, grade, family,
        static_cast<jboolean>(canMerge), unit->level);
}

void BattleScene::notifyMergeComplete(int tileIndex, bool lucky, int resultUnitId) {
    if (!g_jniInitialized || !g_onMergeMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    env->CallStaticVoidMethod(g_bridgeClass, g_onMergeMethod,
        tileIndex, static_cast<jboolean>(lucky), resultUnitId);
}

void BattleScene::handleRelocateUnit(int tileIndex, float normX, float normY) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col)) return;

    Unit* unit = grid_.getUnit(row, col);
    if (!unit || !unit->active) return;

    // Convert normalized to world coordinates
    float worldX = normX * 1280.f;
    float worldY = normY * 720.f;

    // Clamp within field bounds
    worldX = std::max(Grid::GRID_X + 10.f, std::min(Grid::GRID_X + Grid::GRID_W - 10.f, worldX));
    worldY = std::max(Grid::GRID_Y + 10.f, std::min(Grid::GRID_Y + Grid::GRID_H - 10.f, worldY));

    unit->homePosition = {worldX, worldY};
    unit->position = {worldX, worldY};
    unit->entity.transform.position = unit->position;

    aout << "Relocated unit at tile " << tileIndex << " to (" << worldX << ", " << worldY << ")" << std::endl;
}
