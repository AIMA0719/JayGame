#include "TestScene.h"
#include "GameEngine.h"
#include "TextureAsset.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>

#include <cmath>
#include <cstdlib>
#include <algorithm>

TestScene::TestScene(GameEngine &engine)
    : engine_(engine),
      entityPool_(256),
      spatialHash_(64.f) {
}

void TestScene::onEnter() {
    aout << "TestScene::onEnter" << std::endl;

    auto assetManager = engine_.getApp()->activity->assetManager;
    texture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");

    if (!texture_) {
        aout << "ERROR: Failed to load texture!" << std::endl;
        return;
    }

    spawnEntities();
    aout << "TestScene: spawned " << activeEntities_.size() << " entities" << std::endl;
}

void TestScene::onExit() {
    aout << "TestScene::onExit" << std::endl;
    activeEntities_.clear();
    entityPool_.clear();
    texture_.reset();
}

void TestScene::spawnEntities() {
    // Tower defense layout: create grid-like unit placement + wandering mobs
    // Area: 1280x720 logical pixels

    // --- Lane mobs (entities that move along paths) ---
    for (int i = 0; i < 80; i++) {
        Entity *e = entityPool_.acquire();
        e->id = i;
        e->sprite.region = TextureRegion(texture_);
        e->sprite.size = {ENTITY_SIZE, ENTITY_SIZE};
        e->sprite.color = {
            0.5f + 0.5f * (static_cast<float>(i % 5) / 4.f),
            0.5f + 0.5f * (static_cast<float>(i % 3) / 2.f),
            0.5f + 0.5f * (static_cast<float>(i % 7) / 6.f),
            1.f
        };

        // Spread across the screen in rows
        int row = i / 16;
        int col = i % 16;
        float x = 80.f + col * 75.f;
        float y = 100.f + row * 120.f;
        e->transform.position = {x, y};
        e->transform.syncPrevious();

        activeEntities_.push_back(e);
    }

    // --- Tower units (stationary, larger) ---
    for (int i = 80; i < ENTITY_COUNT; i++) {
        Entity *e = entityPool_.acquire();
        e->id = i;
        e->sprite.region = TextureRegion(texture_);
        e->sprite.size = {ENTITY_SIZE * 1.5f, ENTITY_SIZE * 1.5f};
        e->sprite.color = {1.f, 0.8f, 0.2f, 1.f}; // golden towers

        int idx = i - 80;
        float x = 100.f + (idx % 10) * 120.f;
        float y = 600.f + (idx / 10) * 80.f;
        e->transform.position = {x, y};
        e->transform.syncPrevious();
        e->sprite.zOrder = 1;

        activeEntities_.push_back(e);
    }
}

void TestScene::onUpdate(float dt) {
    animTime_ += dt;

    // Sync previous transforms for interpolation
    for (auto *e : activeEntities_) {
        e->syncPrevious();
    }

    // Animate mob entities (first 80) - wandering motion
    for (int i = 0; i < std::min(80, static_cast<int>(activeEntities_.size())); i++) {
        auto *e = activeEntities_[i];
        float phase = animTime_ + static_cast<float>(e->id) * 0.3f;

        // Gentle oscillation + slow drift
        float dx = std::sin(phase * 1.5f) * 30.f * dt;
        float dy = std::cos(phase * 0.8f) * 15.f * dt;
        e->transform.position.x += dx;
        e->transform.position.y += dy;

        // Keep within bounds
        e->transform.position.x = std::max(20.f, std::min(1260.f, e->transform.position.x));
        e->transform.position.y = std::max(20.f, std::min(700.f, e->transform.position.y));

        // Gentle rotation
        e->transform.rotation = std::sin(phase * 2.f) * 0.15f;
    }

    // Update spatial hash
    updateSpatialHash();

    // FPS counter
    frameCount_++;
    fpsTimer_ += dt;
    if (fpsTimer_ >= 1.f) {
        currentFps_ = static_cast<float>(frameCount_) / fpsTimer_;
        frameCount_ = 0;
        fpsTimer_ = 0.f;
    }

    // Pool stress test: periodically acquire/release entities
    poolTestTimer_ += dt;
    if (poolTestTimer_ >= 2.f) {
        poolTestTimer_ = 0.f;
        poolCycleCount_++;

        // Acquire and immediately release 10 entities to verify no leaks
        std::vector<Entity *> temp;
        for (int i = 0; i < 10; i++) {
            temp.push_back(entityPool_.acquire());
        }
        for (auto *e : temp) {
            entityPool_.release(e);
        }
    }

    // Performance logging
    perfLogTimer_ += dt;
    if (perfLogTimer_ >= PERF_LOG_INTERVAL) {
        perfLogTimer_ = 0.f;
        logPerformanceStats();
    }
}

void TestScene::updateSpatialHash() {
    spatialHash_.clear();
    for (auto *e : activeEntities_) {
        if (e->active) {
            spatialHash_.insert(e, e->getBounds());
        }
    }
}

void TestScene::onRender(float alpha, SpriteBatch &batch) {
    // Sort by zOrder for correct layering
    std::sort(activeEntities_.begin(), activeEntities_.end(),
              [](const Entity *a, const Entity *b) {
                  return a->sprite.zOrder < b->sprite.zOrder;
              });

    batch.begin();

    for (auto *e : activeEntities_) {
        e->render(alpha, batch);
    }

    batch.end();
}

void TestScene::onInput(const InputEvent &event) {
    switch (event.action) {
        case InputAction::Down:
        case InputAction::Tap: {
            // Find entity under touch using spatial hash
            auto candidates = spatialHash_.queryPoint(event.worldPos.x, event.worldPos.y);
            for (auto *e : candidates) {
                if (e->getBounds().contains(event.worldPos)) {
                    aout << "Entity " << e->id << " tapped at ("
                         << event.worldPos.x << ", " << event.worldPos.y << ")" << std::endl;
                    break;
                }
            }
            break;
        }

        case InputAction::DragBegin: {
            auto candidates = spatialHash_.queryPoint(event.worldPos.x, event.worldPos.y);
            for (auto *e : candidates) {
                if (e->getBounds().contains(event.worldPos)) {
                    draggedEntity_ = e;
                    dragOffset_ = e->transform.position - event.worldPos;
                    aout << "Dragging entity " << e->id << std::endl;
                    break;
                }
            }
            break;
        }

        case InputAction::DragMove: {
            if (draggedEntity_) {
                Vec2 newPos = event.worldPos + dragOffset_;
                draggedEntity_->transform.position = newPos;
            }
            break;
        }

        case InputAction::DragEnd:
        case InputAction::Up:
        case InputAction::Cancel: {
            if (draggedEntity_) {
                aout << "Released entity " << draggedEntity_->id
                     << " at (" << draggedEntity_->transform.position.x
                     << ", " << draggedEntity_->transform.position.y << ")" << std::endl;
                draggedEntity_ = nullptr;
            }
            break;
        }

        default:
            break;
    }
}

void TestScene::logPerformanceStats() {
    auto &batch = engine_.getSpriteBatch();
    aout << "=== Performance Stats ===" << std::endl;
    aout << "FPS: " << currentFps_ << std::endl;
    aout << "Active entities: " << activeEntities_.size() << std::endl;
    aout << "Draw calls: " << batch.getDrawCallCount() << std::endl;
    aout << "Sprites rendered: " << batch.getSpriteCount() << std::endl;
    aout << "Pool: active=" << entityPool_.activeCount()
         << " capacity=" << entityPool_.capacity()
         << " free=" << entityPool_.freeCount() << std::endl;
    aout << "SpatialHash cells: " << spatialHash_.getCellCount()
         << " queries: " << spatialHash_.getQueryCount() << std::endl;
    aout << "Pool stress cycles: " << poolCycleCount_ << std::endl;
    spatialHash_.resetQueryCount();
    aout << "=========================" << std::endl;
}
