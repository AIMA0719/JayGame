#ifndef JAYGAME_SCENE_H
#define JAYGAME_SCENE_H

#include "InputEvent.h"

class SpriteBatch;

class Scene {
public:
    virtual ~Scene() = default;

    virtual void onEnter() {}
    virtual void onExit() {}
    virtual void onUpdate(float dt) = 0;
    virtual void onRender(float alpha, SpriteBatch &batch) = 0;
    virtual void onInput(const InputEvent &event) {}
    virtual void onResize(int logicalWidth, int logicalHeight) {}
};

#endif // JAYGAME_SCENE_H
