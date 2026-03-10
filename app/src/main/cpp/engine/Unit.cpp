#include "Unit.h"
#include "UnitData.h"
#include "Enemy.h"
#include "Projectile.h"
#include "SpriteBatch.h"
#include "SpriteAtlas.h"
#include "TextureAsset.h"

#include <cmath>
#include <limits>

void Unit::init(int defId, Vec2 pos) {
    active = true;
    unitDefId = defId;
    level = 1;
    position = pos;
    attackTimer = 0.f;
    lastTarget = nullptr;
    animTime_ = 0.f;
    attacking_ = false;
    attackAnimTimer_ = 0.f;
    unitBuffs.clear();

    entity.active = true;
    entity.transform.position = pos;
    entity.transform.syncPrevious();
    entity.sprite.size = {80.f, 80.f};
    entity.sprite.origin = {0.5f, 0.5f};

    // Color-code by element
    const UnitDef& def = getUnitDef(defId);
    switch (def.element) {
        case UnitElement::Physical:
            entity.sprite.color = {1.0f, 0.6f, 0.3f, 1.f}; // orange
            break;
        case UnitElement::Magic:
            entity.sprite.color = {0.4f, 0.6f, 1.0f, 1.f}; // blue
            break;
        case UnitElement::Support:
            entity.sprite.color = {0.4f, 1.0f, 0.5f, 1.f}; // green
            break;
    }
}

void Unit::update(float dt, ObjectPool<Enemy>& enemies, ObjectPool<Projectile>& projectiles,
                  SpatialHash<Enemy>& spatialHash) {
    if (!active) return;

    animTime_ += dt;

    if (attackAnimTimer_ > 0.f) {
        attackAnimTimer_ -= dt;
        if (attackAnimTimer_ <= 0.f) {
            attacking_ = false;
        }
    }

    attackTimer -= dt;
    if (attackTimer > 0.f) return;

    // Update unit buffs
    float dummyDot = 0.f;
    unitBuffs.update(dt, dummyDot);

    // Find target
    Enemy* target = findTarget(spatialHash);
    if (!target) return;

    lastTarget = target;

    // Fire projectile
    Projectile* proj = projectiles.acquire();
    if (proj) {
        proj->init(position, target, getDamage());
        proj->sourceUnitId = unitDefId;

        // Set projectile visual type based on unit ability
        const UnitDef& def = getUnitDef(unitDefId);
        switch (def.ability) {
            case AbilityType::Splash:  proj->projType = 1; break; // fireball
            case AbilityType::Slow:    proj->projType = 2; break; // ice
            case AbilityType::DoT:     proj->projType = 3; break; // poison
            case AbilityType::Chain:   proj->projType = 4; break; // lightning
            default:                   proj->projType = 0; break; // arrow
        }

        attacking_ = true;
        attackAnimTimer_ = 0.3f;  // show attack animation for 0.3s
    }

    // Reset attack timer (period = 1 / atkSpeed)
    attackTimer = 1.f / getAtkSpeed();
}

void Unit::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();
    float cx = position.x;
    float cy = position.y;
    float radius = 24.f;

    // Unit type colors
    static const Vec4 unitColors[] = {
        {0.9f, 0.3f, 0.3f, 1.f},  // 0: red
        {0.3f, 0.5f, 0.9f, 1.f},  // 1: blue
        {0.3f, 0.9f, 0.4f, 1.f},  // 2: green
        {0.9f, 0.9f, 0.3f, 1.f},  // 3: yellow
        {0.8f, 0.3f, 0.9f, 1.f},  // 4: purple
        {0.9f, 0.6f, 0.2f, 1.f},  // 5: orange
        {0.3f, 0.9f, 0.9f, 1.f},  // 6: cyan
        {0.9f, 0.5f, 0.6f, 1.f},  // 7: pink
        {0.6f, 0.6f, 0.6f, 1.f},  // 8: gray
        {1.0f, 0.8f, 0.4f, 1.f},  // 9: gold
        {0.4f, 0.3f, 0.8f, 1.f},  // 10: indigo
        {0.2f, 0.7f, 0.5f, 1.f},  // 11: emerald
        {0.8f, 0.2f, 0.5f, 1.f},  // 12: magenta
        {0.5f, 0.8f, 0.2f, 1.f},  // 13: lime
        {0.7f, 0.4f, 0.2f, 1.f},  // 14: brown
    };

    int colorIdx = unitDefId % 15;
    Vec4 color = unitColors[colorIdx];

    // Brightness increases with level
    float brightness = 1.0f + (level - 1) * 0.05f;
    color.x = std::min(color.x * brightness, 1.f);
    color.y = std::min(color.y * brightness, 1.f);
    color.z = std::min(color.z * brightness, 1.f);

    // Outer ring (darker border)
    float outR = radius + 2.f;
    batch.draw(tex,
               cx - outR, cy - outR, outR * 2.f, outR * 2.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x * 0.5f, color.y * 0.5f, color.z * 0.5f, 0.8f);

    // Inner filled circle (colored square)
    batch.draw(tex,
               cx - radius, cy - radius, radius * 2.f, radius * 2.f,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x, color.y, color.z, color.w);

    // Attack animation pulse
    if (attacking_) {
        float pulse = 0.5f + 0.5f * std::sin(attackAnimTimer_ * 20.f);
        float pR = radius + 4.f * pulse;
        batch.draw(tex,
                   cx - pR, cy - pR, pR * 2.f, pR * 2.f,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   1.f, 1.f, 1.f, 0.2f * pulse);
    }
}

float Unit::getDamage() const {
    const UnitDef& def = getUnitDef(unitDefId);
    int idx = (level >= 1 && level <= 7) ? (level - 1) : 0;
    float baseDmg = def.baseATK * LEVEL_MULTIPLIER[idx];
    float atkBonus = unitBuffs.getAtkBonus();
    return baseDmg * (1.f + atkBonus);
}

float Unit::getRange() const {
    return getUnitDef(unitDefId).range;
}

float Unit::getAtkSpeed() const {
    float baseSpd = getUnitDef(unitDefId).atkSpeed;
    float spdBonus = unitBuffs.getSpdBonus();
    return baseSpd * (1.f + spdBonus);
}

Enemy* Unit::findTarget(SpatialHash<Enemy>& spatialHash) {
    float range = getRange();

    // Query spatial hash with a bounding rect centered on unit position
    Rect queryRect(position.x - range, position.y - range, range * 2.f, range * 2.f);
    auto candidates = spatialHash.query(queryRect);

    Enemy* bestTarget = nullptr;
    float bestProgress = -1.f;

    for (Enemy* enemy : candidates) {
        if (!enemy->active || enemy->isDead()) continue;

        // Check actual distance
        Vec2 diff = enemy->position - position;
        float distSq = diff.lengthSq();
        if (distSq > range * range) continue;

        // Pick enemy closest to path end (highest pathProgress + pathIndex)
        // Use pathIndex * 1000 + pathProgress as a combined metric
        float progress = static_cast<float>(enemy->pathIndex) * 1000.f + enemy->pathProgress;
        if (progress > bestProgress) {
            bestProgress = progress;
            bestTarget = enemy;
        }
    }

    return bestTarget;
}
