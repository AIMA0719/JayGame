#include "Projectile.h"
#include "Enemy.h"
#include "SpriteBatch.h"
#include "SpriteAtlas.h"
#include "TextureAsset.h"

#include <cmath>

void Projectile::init(Vec2 startPos, Enemy* tgt, float dmg) {
    active = true;
    position = startPos;
    prevPosition_ = startPos;
    target = tgt;
    damage = dmg;
    speed = 400.f;
    lifetime = 3.f;
    size = 16.f;
    hit_ = false;
    hitTarget_ = nullptr;
    sourceUnitId = -1;
    projType = 0;
    isMagic = false;
    velocity = {0.f, 0.f};
}

void Projectile::update(float dt) {
    if (!active) return;

    prevPosition_ = position;
    lifetime -= dt;
    if (lifetime <= 0.f) {
        active = false;
        return;
    }

    // If target is still alive, home in on it
    if (target && target->active && !target->isDead()) {
        Vec2 diff = target->position - position;
        float dist = diff.length();

        if (dist < size) {
            // Hit! Damage is applied by BattleScene::updateProjectiles with isMagic flag
            hit_ = true;
            hitTarget_ = target;
            active = false;
            return;
        }

        velocity = diff.normalized() * speed;
    }

    position += velocity * dt;
}

bool Projectile::hasHit() const {
    return hit_;
}

Enemy* Projectile::getHitTarget() const {
    return hitTarget_;
}

void Projectile::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = Vec2::lerp(prevPosition_, position, alpha);

    const auto& sprite = atlas.getProjectileSprite(projType);
    float elapsed = 3.f - lifetime; // time since creation
    const auto& frame = sprite.fly.getFrame(elapsed);

    // Calculate rotation from velocity direction
    float rotation = 0.f;
    if (velocity.lengthSq() > 1.f) {
        rotation = std::atan2(velocity.y, velocity.x);
    }

    batch.draw(tex, pos, {size, size}, frame.uvRect,
               {1.f, 1.f, 1.f, 1.f}, rotation, {0.5f, 0.5f});
}
