#ifndef JAYGAME_TESTSCENE_H
#define JAYGAME_TESTSCENE_H

#include "Scene.h"
#include "Entity.h"
#include "ObjectPool.h"
#include "SpatialHash.h"
#include "SpriteBatch.h"
#include "MathTypes.h"

#include <vector>
#include <memory>
#include <chrono>

class GameEngine;
class TextureAsset;

class TestScene : public Scene {
public:
    explicit TestScene(GameEngine &engine);
    ~TestScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch &batch) override;
    void onInput(const InputEvent &event) override;

private:
    void spawnEntities();
    void updateSpatialHash();
    void logPerformanceStats();

    GameEngine &engine_;
    std::shared_ptr<TextureAsset> texture_;

    // Entity management
    ObjectPool<Entity> entityPool_;
    std::vector<Entity *> activeEntities_;
    SpatialHash<Entity> spatialHash_;

    // Drag interaction
    Entity *draggedEntity_ = nullptr;
    Vec2 dragOffset_;

    // FPS tracking
    int frameCount_ = 0;
    float fpsTimer_ = 0.f;
    float currentFps_ = 0.f;

    // Performance logging
    float perfLogTimer_ = 0.f;
    static constexpr float PERF_LOG_INTERVAL = 5.f;

    // Pool stress test
    float poolTestTimer_ = 0.f;
    int poolCycleCount_ = 0;

    // Animation
    float animTime_ = 0.f;

    static constexpr int ENTITY_COUNT = 120;
    static constexpr float ENTITY_SIZE = 48.f;
};

#endif // JAYGAME_TESTSCENE_H
