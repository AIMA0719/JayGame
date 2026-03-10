#ifndef JAYGAME_PROJECTILE_H
#define JAYGAME_PROJECTILE_H

#include "MathTypes.h"

class SpriteBatch;
class SpriteAtlas;
class TextureAsset;
class Enemy;

class Projectile {
public:
    bool active = false;
    Vec2 position;
    Vec2 velocity;
    float damage = 0.f;
    Enemy* target = nullptr;
    float speed = 400.f;
    float lifetime = 3.f;
    float size = 16.f;
    int sourceUnitId = -1;   // unit def ID that fired this projectile
    int projType = 0;        // 0=arrow, 1=fireball, 2=ice, 3=poison, 4=lightning, 5=generic

    void init(Vec2 startPos, Enemy* target, float damage);
    void update(float dt);
    bool hasHit() const;
    Enemy* getHitTarget() const;
    void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;

private:
    bool hit_ = false;
    Enemy* hitTarget_ = nullptr;
    Vec2 prevPosition_;
};

#endif // JAYGAME_PROJECTILE_H
