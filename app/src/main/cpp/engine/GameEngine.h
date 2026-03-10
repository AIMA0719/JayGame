#ifndef JAYGAME_GAMEENGINE_H
#define JAYGAME_GAMEENGINE_H

#include <memory>
#include "GraphicsContext.h"
#include "InputSystem.h"
#include "SceneManager.h"
#include "SpriteBatch.h"
#include "TextRenderer.h"
#include "SaveSystem.h"
#include "MathTypes.h"

struct android_app;

class GameEngine {
public:
    static constexpr float LOGICAL_WIDTH = 1280.f;
    static constexpr float LOGICAL_HEIGHT = 720.f;
    static constexpr double FIXED_DT = 1.0 / 60.0; // 60Hz fixed timestep

    explicit GameEngine(android_app *app);
    ~GameEngine();

    // Non-copyable
    GameEngine(const GameEngine &) = delete;
    GameEngine &operator=(const GameEngine &) = delete;

    void run();

    // Window lifecycle (called from handle_cmd)
    void onWindowInit();
    void onWindowTerm();

    GraphicsContext &getGraphics() { return graphics_; }
    InputSystem &getInput() { return input_; }
    SceneManager &getSceneManager() { return sceneManager_; }
    SpriteBatch &getSpriteBatch() { return spriteBatch_; }
    TextRenderer &getTextRenderer() { return textRenderer_; }
    android_app *getApp() const { return app_; }

private:
    void initGL();
    void updateProjection();

    android_app *app_;
    GraphicsContext graphics_;
    InputSystem input_;
    SceneManager sceneManager_;
    SpriteBatch spriteBatch_;
    TextRenderer textRenderer_;
    Mat4 projection_;

    bool initialized_ = false;
    bool windowReady_ = false;
};

#endif // JAYGAME_GAMEENGINE_H
