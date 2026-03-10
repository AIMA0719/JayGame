#ifndef JAYGAME_INPUTEVENT_H
#define JAYGAME_INPUTEVENT_H

#include "MathTypes.h"

enum class InputAction {
    Down,
    Up,
    Move,
    Cancel,
    Tap,
    DragBegin,
    DragMove,
    DragEnd
};

struct InputEvent {
    InputAction action;
    Vec2 screenPos;   // raw screen coordinates
    Vec2 worldPos;    // transformed to logical coordinates (1280x720)
    int pointerId = 0;
    float deltaX = 0.f;
    float deltaY = 0.f;
};

#endif // JAYGAME_INPUTEVENT_H
