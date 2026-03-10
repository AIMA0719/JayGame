#include "BattleScene.h"
#include "GameEngine.h"
#include "UnitData.h"
#include "CombinationTable.h"
#include "ResultScene.h"
#include "PlayerData.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>

#include <cmath>
#include <cstdlib>
#include <algorithm>

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

    // Reset player state
    playerHP_ = 20;
    sp_ = 100.f;
    summonCount_ = 0;
    currentWave_ = 0;
    killCount_ = 0;
    mergeCount_ = 0;
    hpLost_ = 0;
    waveTimer_ = 0.f;
    unitTypesUsed_.clear();

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
    // S-shape path along the right/bottom of the screen
    pathWaypoints_.clear();
    pathWaypoints_.push_back({760.f,  60.f});
    pathWaypoints_.push_back({1220.f, 60.f});
    pathWaypoints_.push_back({1220.f, 240.f});
    pathWaypoints_.push_back({760.f,  240.f});
    pathWaypoints_.push_back({760.f,  420.f});
    pathWaypoints_.push_back({1220.f, 420.f});
    pathWaypoints_.push_back({1220.f, 660.f});
    pathWaypoints_.push_back({760.f,  660.f});
    pathWaypoints_.push_back({40.f,   660.f});
}

void BattleScene::onUpdate(float dt) {
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
        } else if (enemy.reachedEnd()) {
            // Enemy reached the end - player takes damage
            playerHP_--;
            hpLost_++;
            waveManager_.onEnemyEscaped();
            enemy.active = false;
            enemyPool_.release(&enemy);

            if (playerHP_ <= 0) {
                state_ = State::Defeat;
                aout << "DEFEAT! Wave " << currentWave_ << std::endl;
                ResultScene::BattleResult result;
                result.victory = false;
                result.waveReached = currentWave_;
                result.goldEarned = currentWave_ * 10;
                result.trophyChange = -15;
                result.killCount = killCount_;
                result.mergeCount = mergeCount_;
                engine_.getSceneManager().push(
                    std::make_unique<ResultScene>(engine_, result));
            }
        }
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
                // Find the source unit to apply ability effects
                Enemy* hitEnemy = proj.getHitTarget();
                if (hitEnemy->active && !hitEnemy->isDead()) {
                    // Find source unit by scanning pool (lightweight for pool sizes < 64)
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
            ResultScene::BattleResult result;
            result.victory = true;
            result.waveReached = currentWave_;
            result.goldEarned = currentWave_ * 15;
            result.trophyChange = 25;
            result.killCount = killCount_;
            result.mergeCount = mergeCount_;
            result.perfectWin = (hpLost_ == 0);
            result.monoTypeWin = (unitTypesUsed_.size() <= 1);
            engine_.getSceneManager().push(
                std::make_unique<ResultScene>(engine_, result));
        } else {
            state_ = State::WaveDelay;
            waveDelayTimer_ = WAVE_DELAY;
        }
    }
}

void BattleScene::startNextWave() {
    currentWave_++;
    aout << "Starting wave " << currentWave_ << std::endl;
    waveManager_.startWave(currentWave_);
    waveTimer_ = 0.f;
    state_ = State::Playing;
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

    // 6. Render merge system (ghost sprite, highlights)
    mergeSystem_.render(batch, atlas_, grid_);

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
    auto& text = engine_.getTextRenderer();
    const auto& tex = *atlas_.getTexture();
    const auto& wp = atlas_.getWhitePixel();
    const auto& panel = atlas_.getHud("panel");
    char buf[64];

    // === Top HUD bar (horizontal) ===
    batch.draw(tex, 0.f, 0.f, 1280.f, 50.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.1f, 0.08f, 0.06f, 0.85f);

    // HP (left)
    const auto& hpIcon = atlas_.getHud("icon_hp");
    batch.draw(tex, {10.f, 8.f}, {28.f, 28.f},
               hpIcon.uvRect, {1.f,1.f,1.f,1.f}, 0.f, {0.f,0.f});
    snprintf(buf, sizeof(buf), "%d", playerHP_);
    float hpRatio = static_cast<float>(playerHP_) / 20.f;
    Vec4 hpColor = hpRatio > 0.5f ? Vec4{0.3f, 1.f, 0.4f, 1.f}
                                    : Vec4{1.f, 0.3f, 0.3f, 1.f};
    text.drawText(batch, buf, 42.f, 15.f, 3.f, hpColor);

    // Round (center-left)
    snprintf(buf, sizeof(buf), "Round %d/%d", currentWave_, maxWaves_);
    text.drawText(batch, buf, 400.f, 15.f, 2.8f, {0.5f, 0.8f, 1.f, 1.f}, TextAlign::Center);

    // Timer (center)
    int minutes = static_cast<int>(waveTimer_) / 60;
    int seconds = static_cast<int>(waveTimer_) % 60;
    snprintf(buf, sizeof(buf), "%02d:%02d", minutes, seconds);
    text.drawText(batch, buf, 640.f, 15.f, 2.8f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);

    // SP bar (right)
    const auto& spIcon = atlas_.getHud("icon_sp");
    batch.draw(tex, {880.f, 8.f}, {28.f, 28.f},
               spIcon.uvRect, {1.f,1.f,1.f,1.f}, 0.f, {0.f,0.f});
    batch.draw(tex, 916.f, 14.f, 280.f, 22.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.15f, 0.15f, 0.15f, 0.7f);
    float spRatio = std::min(sp_ / 200.f, 1.f);
    batch.draw(tex, 916.f, 14.f, 280.f * spRatio, 22.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.9f, 0.8f, 0.2f, 0.85f);
    snprintf(buf, sizeof(buf), "SP %.0f/200", sp_);
    text.drawText(batch, buf, 1056.f, 15.f, 2.f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);

    // Wave delay overlay
    if (state_ == State::WaveDelay) {
        float pulse = 0.5f + 0.5f * std::sin(waveDelayTimer_ * 4.f);
        batch.draw(tex, 440.f, 330.f, 400.f, 60.f,
                   panel.uvRect.x, panel.uvRect.y, panel.uvRect.w, panel.uvRect.h,
                   0.6f, 0.6f, 0.8f, 0.9f);
        snprintf(buf, sizeof(buf), "NEXT WAVE IN %.0f", waveDelayTimer_ + 1.f);
        text.drawText(batch, buf, 640.f, 345.f, 3.f,
                      {0.5f, 0.8f, 1.f, pulse}, TextAlign::Center);
    }

    // Summon button (keep for now, will be moved to Compose overlay later)
    float cost = getSummonCost();
    bool canSummon = (sp_ >= cost) && (grid_.getEmptyCellCount() > 0);
    const auto& btnSprite = canSummon
        ? atlas_.getHud("btn_normal")
        : atlas_.getHud("btn_disabled");
    Vec4 btnTint = canSummon
        ? Vec4{0.7f, 1.f, 0.8f, 0.95f}
        : Vec4{0.7f, 0.5f, 0.5f, 0.7f};
    batch.draw(tex,
               SUMMON_BUTTON.x, SUMMON_BUTTON.y, SUMMON_BUTTON.w, SUMMON_BUTTON.h,
               btnSprite.uvRect.x, btnSprite.uvRect.y,
               btnSprite.uvRect.w, btnSprite.uvRect.h,
               btnTint.x, btnTint.y, btnTint.z, btnTint.w);
    const auto& summonIcon = atlas_.getHud("icon_summon");
    batch.draw(tex,
               {SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f - 10.f, SUMMON_BUTTON.y + 5.f},
               {20.f, 20.f},
               summonIcon.uvRect, {1.f,1.f,1.f,0.9f}, 0.f, {0.f,0.f});
    snprintf(buf, sizeof(buf), "SUMMON");
    text.drawText(batch, buf,
                  SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f,
                  SUMMON_BUTTON.y + 28.f,
                  2.f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);
    snprintf(buf, sizeof(buf), "%.0f SP", cost);
    text.drawText(batch, buf,
                  SUMMON_BUTTON.x + SUMMON_BUTTON.w * 0.5f,
                  SUMMON_BUTTON.y + 50.f,
                  1.8f, {0.9f, 0.8f, 0.3f, 0.8f}, TextAlign::Center);
}

void BattleScene::onInput(const InputEvent& event) {
    if (state_ == State::Victory || state_ == State::Defeat) return;

    Vec2 pos = event.worldPos;

    // Handle merge system drag events
    switch (event.action) {
        case InputAction::DragBegin:
            // Only start drag if on a unit in the grid
            if (!SUMMON_BUTTON.contains(pos)) {
                mergeSystem_.onDragBegin(pos, grid_);
            }
            break;

        case InputAction::DragMove:
            if (mergeSystem_.isDragging()) {
                mergeSystem_.onDragMove(pos);
            }
            break;

        case InputAction::DragEnd: {
            if (mergeSystem_.isDragging()) {
                auto result = mergeSystem_.onDragEnd(pos, grid_, unitPool_);
                switch (result) {
                    case MergeSystem::MergeResult::Merged:
                        mergeCount_++;
                        aout << "Merge successful!" << std::endl;
                        break;
                    case MergeSystem::MergeResult::Combined:
                        mergeCount_++;
                        aout << "Hidden combination!" << std::endl;
                        break;
                    default:
                        break;
                }
                return; // consume event
            }
            break;
        }

        case InputAction::Cancel:
            if (mergeSystem_.isDragging()) {
                mergeSystem_.onDragCancel(grid_);
                return;
            }
            break;

        case InputAction::Tap:
            // Summon button
            if (SUMMON_BUTTON.contains(pos)) {
                summonUnit();
                return;
            }
            // Info tap on unit
            {
                int row, col;
                if (grid_.getCellAt(pos, row, col)) {
                    Unit* unit = grid_.getUnit(row, col);
                    if (unit) {
                        const UnitDef& def = getUnitDef(unit->unitDefId);
                        aout << "Unit: " << def.name << " lv" << unit->level
                             << " ATK=" << unit->getDamage()
                             << " SPD=" << unit->getAtkSpeed() << std::endl;
                    }
                }
            }
            break;

        default:
            break;
    }
}
