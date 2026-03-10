#ifndef JAYGAME_ENTITY_H
#define JAYGAME_ENTITY_H

#include "Components.h"
#include "SpriteBatch.h"

class Entity {
public:
    Transform transform;
    SpriteComponent sprite;
    bool active = true;
    int id = -1;

    Entity() = default;

    void syncPrevious() {
        transform.syncPrevious();
    }

    void render(float alpha, SpriteBatch &batch) const {
        if (!active || !sprite.region.texture) return;

        Vec2 pos = transform.interpolatedPosition(alpha);
        float rot = transform.interpolatedRotation(alpha);

        batch.draw(*sprite.region.texture,
                   pos, {sprite.size.x * transform.scale.x, sprite.size.y * transform.scale.y},
                   sprite.region.uvRect,
                   sprite.color,
                   rot,
                   sprite.origin);
    }

    Rect getBounds() const {
        float w = sprite.size.x * transform.scale.x;
        float h = sprite.size.y * transform.scale.y;
        return {
            transform.position.x - w * sprite.origin.x,
            transform.position.y - h * sprite.origin.y,
            w, h
        };
    }
};

#endif // JAYGAME_ENTITY_H
