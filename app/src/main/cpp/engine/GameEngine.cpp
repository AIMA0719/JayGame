#include "GameEngine.h"

#include <chrono>
#include <cassert>
#include <GLES3/gl3.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>

#include "AndroidOut.h"
#include "Scene.h"

GameEngine::GameEngine(android_app *app) : app_(app) {
    aout << "GameEngine created" << std::endl;

    // Initialize save system and load player data
    SaveSystem::get().init(app);
    SaveSystem::get().load();
}

GameEngine::~GameEngine() {
    // Shutdown in reverse order
    if (initialized_) {
        textRenderer_.shutdown();
        spriteBatch_.shutdown();
        graphics_.shutdown();
        initialized_ = false;
    }
    aout << "GameEngine destroyed" << std::endl;
}

void GameEngine::onWindowInit() {
    windowReady_ = true;
    aout << "onWindowInit: initialized_=" << initialized_ << " hasContext=" << graphics_.hasContext() << std::endl;
    if (!initialized_ && app_->window) {
        if (graphics_.hasContext()) {
            // GL context exists but surface was destroyed — just recreate surface
            if (graphics_.recreateSurface(app_)) {
                initialized_ = true;
                updateProjection();
                aout << "GL re-initialized (surface recreated)" << std::endl;
            }
        } else {
            initGL();
        }
    }
}

void GameEngine::onWindowTerm() {
    windowReady_ = false;
    if (initialized_) {
        // Only destroy the surface, keep GL context and GPU resources alive
        graphics_.destroySurface();
        initialized_ = false;
        aout << "GL paused (surface destroyed, context kept)" << std::endl;
    }
}

void GameEngine::initGL() {
    if (initialized_) return;

    graphics_.init(app_);

    // Global GL state
    // DeepDark #0f0f23 — matches the dark neon Compose theme
    glClearColor(0x0f / 255.f, 0x0f / 255.f, 0x23 / 255.f, 1.f);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    spriteBatch_.init();
    textRenderer_.init();
    updateProjection();

    initialized_ = true;
    aout << "GameEngine GL initialized" << std::endl;
}

void GameEngine::updateProjection() {
    projection_ = Mat4::ortho(0.f, LOGICAL_WIDTH, LOGICAL_HEIGHT, 0.f, -1.f, 1.f);
    spriteBatch_.setProjectionMatrix(projection_);
}

void GameEngine::run() {
    using Clock = std::chrono::steady_clock;
    using Duration = std::chrono::duration<double>;

    auto previousTime = Clock::now();
    double accumulator = 0.0;

    // Input callback forwards to current scene
    input_.setEventCallback([this](const InputEvent &event) {
        Scene *scene = sceneManager_.current();
        if (scene) {
            scene->onInput(event);
        }
    });

    while (!app_->destroyRequested) {
        // Process Android events
        bool done = false;
        while (!done) {
            int events;
            android_poll_source *pSource;
            int result = ALooper_pollOnce(0, nullptr, &events,
                                          reinterpret_cast<void **>(&pSource));
            switch (result) {
                case ALOOPER_POLL_TIMEOUT:
                    [[clang::fallthrough]];
                case ALOOPER_POLL_WAKE:
                    done = true;
                    break;
                case ALOOPER_EVENT_ERROR:
                    aout << "ALooper_pollOnce returned an error" << std::endl;
                    break;
                case ALOOPER_POLL_CALLBACK:
                    break;
                default:
                    if (pSource) {
                        pSource->process(app_, pSource);
                    }
            }
        }

        // Wait for window to be ready
        if (!windowReady_ || !initialized_) {
            // Try to initialize if window just became ready
            if (windowReady_ && !initialized_ && app_->window) {
                initGL();
                // Re-enter current scene to reload textures
                Scene *s = sceneManager_.current();
                if (s) {
                    s->onEnter();
                }
            } else {
                previousTime = Clock::now();
                continue;
            }
        }

        // Check for render area changes
        if (graphics_.updateRenderArea()) {
            input_.setScreenSize(graphics_.getWidth(), graphics_.getHeight());
            updateProjection();
            Scene *scene = sceneManager_.current();
            if (scene) {
                scene->onResize(static_cast<int>(LOGICAL_WIDTH),
                               static_cast<int>(LOGICAL_HEIGHT));
            }
        }

        // Process input
        input_.processInput(app_);

        // Process pending scene transitions
        sceneManager_.processPending();

        // Fixed timestep update + variable rendering
        auto currentTime = Clock::now();
        double delta = Duration(currentTime - previousTime).count();
        previousTime = currentTime;

        // Clamp delta to prevent spiral of death
        if (delta > 0.25) delta = 0.25;

        accumulator += delta;

        Scene *scene = sceneManager_.current();

        // Fixed timestep updates
        while (accumulator >= FIXED_DT) {
            if (scene) {
                scene->onUpdate(static_cast<float>(FIXED_DT));
            }
            accumulator -= FIXED_DT;
        }

        // Render phase: C++ rendering disabled — Compose handles all visuals.
        // Keep glClear + swap to maintain EGL surface alive for GameActivity.
        glClear(GL_COLOR_BUFFER_BIT);
        // scene->onRender() intentionally skipped: all rendering done by Compose overlay.
        graphics_.swapBuffers();
    }
}
