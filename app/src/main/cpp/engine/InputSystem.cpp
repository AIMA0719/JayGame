#include "InputSystem.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <game-activity/GameActivity.h>
#include <chrono>

void InputSystem::setScreenSize(int width, int height) {
    screenWidth_ = width > 0 ? width : 1;
    screenHeight_ = height > 0 ? height : 1;
}

void InputSystem::setEventCallback(EventCallback callback) {
    callback_ = std::move(callback);
}

Vec2 InputSystem::screenToWorld(float screenX, float screenY) const {
    return {
        (screenX / static_cast<float>(screenWidth_)) * LOGICAL_WIDTH,
        (screenY / static_cast<float>(screenHeight_)) * LOGICAL_HEIGHT
    };
}

double InputSystem::getCurrentTimeMs() const {
    auto now = std::chrono::steady_clock::now();
    return std::chrono::duration<double, std::milli>(now.time_since_epoch()).count();
}

void InputSystem::processInput(android_app *app) {
    auto *inputBuffer = android_app_swap_input_buffers(app);
    if (!inputBuffer) return;

    for (auto i = 0; i < inputBuffer->motionEventsCount; i++) {
        auto &motionEvent = inputBuffer->motionEvents[i];
        auto action = motionEvent.action;
        auto maskedAction = action & AMOTION_EVENT_ACTION_MASK;

        auto pointerIndex = (action & AMOTION_EVENT_ACTION_POINTER_INDEX_MASK)
            >> AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT;

        switch (maskedAction) {
            case AMOTION_EVENT_ACTION_DOWN:
            case AMOTION_EVENT_ACTION_POINTER_DOWN: {
                auto &pointer = motionEvent.pointers[pointerIndex];
                float sx = GameActivityPointerAxes_getX(&pointer);
                float sy = GameActivityPointerAxes_getY(&pointer);
                int pid = pointer.id;

                if (pid < MAX_POINTERS) {
                    pointers_[pid].active = true;
                    pointers_[pid].dragging = false;
                    pointers_[pid].downPos = {sx, sy};
                    pointers_[pid].downTime = getCurrentTimeMs();
                }

                if (callback_) {
                    InputEvent ev;
                    ev.action = InputAction::Down;
                    ev.screenPos = {sx, sy};
                    ev.worldPos = screenToWorld(sx, sy);
                    ev.pointerId = pid;
                    callback_(ev);
                }
                break;
            }

            case AMOTION_EVENT_ACTION_UP:
            case AMOTION_EVENT_ACTION_POINTER_UP:
            case AMOTION_EVENT_ACTION_CANCEL: {
                auto &pointer = motionEvent.pointers[pointerIndex];
                float sx = GameActivityPointerAxes_getX(&pointer);
                float sy = GameActivityPointerAxes_getY(&pointer);
                int pid = pointer.id;

                if (callback_) {
                    InputEvent ev;
                    ev.screenPos = {sx, sy};
                    ev.worldPos = screenToWorld(sx, sy);
                    ev.pointerId = pid;

                    if (maskedAction == AMOTION_EVENT_ACTION_CANCEL) {
                        ev.action = InputAction::Cancel;
                        callback_(ev);
                    } else {
                        if (pid < MAX_POINTERS && pointers_[pid].dragging) {
                            ev.action = InputAction::DragEnd;
                            callback_(ev);
                        } else if (pid < MAX_POINTERS && pointers_[pid].active) {
                            // Check for tap
                            float dx = sx - pointers_[pid].downPos.x;
                            float dy = sy - pointers_[pid].downPos.y;
                            double elapsed = getCurrentTimeMs() - pointers_[pid].downTime;
                            if (std::sqrt(dx * dx + dy * dy) < TAP_THRESHOLD && elapsed < TAP_TIME_MS) {
                                ev.action = InputAction::Tap;
                                callback_(ev);
                            }
                        }

                        ev.action = InputAction::Up;
                        callback_(ev);
                    }
                }

                if (pid < MAX_POINTERS) {
                    pointers_[pid].active = false;
                    pointers_[pid].dragging = false;
                }
                break;
            }

            case AMOTION_EVENT_ACTION_MOVE: {
                for (auto idx = 0; idx < motionEvent.pointerCount; idx++) {
                    auto &pointer = motionEvent.pointers[idx];
                    float sx = GameActivityPointerAxes_getX(&pointer);
                    float sy = GameActivityPointerAxes_getY(&pointer);
                    int pid = pointer.id;

                    if (callback_) {
                        InputEvent ev;
                        ev.screenPos = {sx, sy};
                        ev.worldPos = screenToWorld(sx, sy);
                        ev.pointerId = pid;

                        // Detect drag start
                        if (pid < MAX_POINTERS && pointers_[pid].active && !pointers_[pid].dragging) {
                            float dx = sx - pointers_[pid].downPos.x;
                            float dy = sy - pointers_[pid].downPos.y;
                            if (std::sqrt(dx * dx + dy * dy) >= TAP_THRESHOLD) {
                                pointers_[pid].dragging = true;
                                ev.action = InputAction::DragBegin;
                                ev.deltaX = dx;
                                ev.deltaY = dy;
                                callback_(ev);
                            }
                        }

                        if (pid < MAX_POINTERS && pointers_[pid].dragging) {
                            ev.action = InputAction::DragMove;
                            ev.deltaX = sx - pointers_[pid].downPos.x;
                            ev.deltaY = sy - pointers_[pid].downPos.y;
                            callback_(ev);
                        } else {
                            ev.action = InputAction::Move;
                            callback_(ev);
                        }
                    }
                }
                break;
            }

            default:
                break;
        }
    }
    android_app_clear_motion_events(inputBuffer);

    // Handle key events
    for (auto i = 0; i < inputBuffer->keyEventsCount; i++) {
        // Key events are ignored for now (no game use yet)
    }
    android_app_clear_key_events(inputBuffer);
}
