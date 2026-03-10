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

// JNI state push — cached references
static jclass g_bridgeClass = nullptr;
static jmethodID g_updateMethod = nullptr;
static jmethodID g_battleEndMethod = nullptr;
static jmethodID g_pushGridMethod = nullptr;
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
                    "([I[I[I[Z)V");
                g_jniInitialized = (g_updateMethod != nullptr && g_battleEndMethod != nullptr);
                aout << "BattleBridge JNI initialized: " << g_jniInitialized
                     << " gridMethod: " << (g_pushGridMethod != nullptr) << std::endl;
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
    // S-shape path in top area (y: 30-280) — monsters traverse above the grid
    pathWaypoints_.clear();
    pathWaypoints_.push_back({40.f,   60.f});   // start left
    pathWaypoints_.push_back({1240.f, 60.f});   // go right
    pathWaypoints_.push_back({1240.f, 160.f});  // down
    pathWaypoints_.push_back({40.f,   160.f});  // go left
    pathWaypoints_.push_back({40.f,   260.f});  // down
    pathWaypoints_.push_back({1240.f, 260.f});  // go right
    // Path ends here, monsters loop back to start
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

    // Place on grid
    grid_.placeUnit(row, col, unit);

    unitTypesUsed_.insert(def.id);

    aout << "Summoned " << def.name << " at (" << row << "," << col
         << ") cost=" << cost << " SP=" << sp_ << std::endl;
}

float BattleScene::getSummonCost() const {
    // Cost increases with each summon: 10 + 2 * summonCount
    return 10.f + 2.f * static_cast<float>(summonCount_);
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

    // 1. Render grid
    grid_.render(batch, atlas_);

    // 2. Render path
    renderPath(batch);

    // 3. Render enemies
    enemyPool_.forEach([&](Enemy& enemy) {
        if (enemy.active) {
            enemy.render(alpha, batch, atlas_);
        }
    });

    // 4. Render units
    unitPool_.forEach([&](Unit& unit) {
        if (unit.active) {
            unit.render(alpha, batch, atlas_);
        }
    });

    // 4.5. Render unit level text
    {
        auto& tr = engine_.getTextRenderer();
        char buf[8];
        unitPool_.forEach([&](Unit& unit) {
            if (!unit.active) return;
            snprintf(buf, sizeof(buf), "%d", unit.level);
            tr.drawText(batch, buf, unit.position.x, unit.position.y + 14.f, 2.f,
                        {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);
        });
    }

    // 5. Render projectiles
    projectilePool_.forEach([&](Projectile& proj) {
        if (proj.active) {
            proj.render(alpha, batch, atlas_);
        }
    });

    // 6. Merge system rendering removed — now click-based via Compose overlay

    // 7. HUD rendering disabled — now handled by Compose overlay
    // renderHUD(batch);

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
    grid_.placeUnit(row, col, unit);
    unitTypesUsed_.insert(def.id);

    aout << "Summoned " << def.name << " at tile " << tileIndex
         << " cost=" << cost << " SP=" << sp_ << std::endl;
}

void BattleScene::handleTileClick(int tileIndex) {
    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;
    if (!grid_.isValid(row, col)) return;

    Unit* unit = grid_.getUnit(row, col);
    if (unit) {
        // Unit info — logged for now, detail popup handled by Compose
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

void BattleScene::pushGridState() {
    if (!g_jniInitialized || !g_pushGridMethod) return;

    JNIEnv* env = nullptr;
    engine_.getApp()->activity->vm->AttachCurrentThread(&env, nullptr);
    if (!env || !g_bridgeClass) return;

    jintArray unitIds = env->NewIntArray(15);
    jintArray grades = env->NewIntArray(15);
    jintArray families = env->NewIntArray(15);
    jbooleanArray canMerge = env->NewBooleanArray(15);

    int ids[15], grd[15], fam[15];
    jboolean merge[15];

    for (int i = 0; i < 15; i++) {
        int r = i / 5, c = i % 5;
        Unit* u = grid_.getUnit(r, c);
        if (u && u->active) {
            ids[i] = u->unitDefId;
            grd[i] = u->unitDefId / 5;  // grade
            fam[i] = u->unitDefId % 5;  // family
            // Can merge if 3+ same family+grade exist
            int count = 0;
            int grade = u->unitDefId / 5;
            int family = u->unitDefId % 5;
            for (int j = 0; j < 15; j++) {
                int jr = j / 5, jc = j % 5;
                Unit* uj = grid_.getUnit(jr, jc);
                if (uj && uj->active && uj->unitDefId / 5 == grade && uj->unitDefId % 5 == family)
                    count++;
            }
            merge[i] = (count >= 3 && grade < 4) ? JNI_TRUE : JNI_FALSE;
        } else {
            ids[i] = -1;
            grd[i] = -1;
            fam[i] = -1;
            merge[i] = JNI_FALSE;
        }
    }

    env->SetIntArrayRegion(unitIds, 0, 15, ids);
    env->SetIntArrayRegion(grades, 0, 15, grd);
    env->SetIntArrayRegion(families, 0, 15, fam);
    env->SetBooleanArrayRegion(canMerge, 0, 15, merge);

    env->CallStaticVoidMethod(g_bridgeClass, g_pushGridMethod, unitIds, grades, families, canMerge);

    env->DeleteLocalRef(unitIds);
    env->DeleteLocalRef(grades);
    env->DeleteLocalRef(families);
    env->DeleteLocalRef(canMerge);
}
