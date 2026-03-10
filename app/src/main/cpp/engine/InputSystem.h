#ifndef JAYGAME_INPUTSYSTEM_H
#define JAYGAME_INPUTSYSTEM_H

#include <vector>
#include <functional>
#include "InputEvent.h"
#include "MathTypes.h"

struct android_app;

class InputSystem {
public:
    static constexpr float LOGICAL_WIDTH = 1280.f;
    static constexpr float LOGICAL_HEIGHT = 720.f;
    static constexpr float TAP_THRESHOLD = 15.f;     // pixels in screen space
    static constexpr float TAP_TIME_MS = 300.f;

    using EventCallback = std::function<void(const InputEvent &)>;

    void setScreenSize(int width, int height);
    void setEventCallback(EventCallback callback);

    // Process raw input from GameActivity, emits InputEvents via callback
    void processInput(android_app *app);

    // Convert screen coordinates to logical world coordinates
    Vec2 screenToWorld(float screenX, float screenY) const;

private:
    EventCallback callback_;
    int screenWidth_ = 1;
    int screenHeight_ = 1;

    // Per-pointer tracking for gesture detection
    struct PointerState {
        bool active = false;
        bool dragging = false;
        Vec2 downPos;
        double downTime = 0.0;
    };
    static constexpr int MAX_POINTERS = 10;
    PointerState pointers_[MAX_POINTERS] = {};

    double getCurrentTimeMs() const;
};

#endif // JAYGAME_INPUTSYSTEM_H
