#ifndef JAYGAME_COMPONENTS_H
#define JAYGAME_COMPONENTS_H

#include "MathTypes.h"
#include "Sprite.h"

struct Transform {
    Vec2 position;
    Vec2 prevPosition; // for interpolation
    float rotation = 0.f;
    float prevRotation = 0.f;
    Vec2 scale = {1.f, 1.f};

    void syncPrevious() {
        prevPosition = position;
        prevRotation = rotation;
    }

    Vec2 interpolatedPosition(float alpha) const {
        return Vec2::lerp(prevPosition, position, alpha);
    }

    float interpolatedRotation(float alpha) const {
        return prevRotation + (rotation - prevRotation) * alpha;
    }
};

struct SpriteComponent {
    TextureRegion region;
    Vec4 color = {1.f, 1.f, 1.f, 1.f};
    Vec2 size = {64.f, 64.f};   // size in logical pixels
    Vec2 origin = {0.5f, 0.5f}; // normalized origin (0.5 = center)
    int zOrder = 0;
};

#endif // JAYGAME_COMPONENTS_H
