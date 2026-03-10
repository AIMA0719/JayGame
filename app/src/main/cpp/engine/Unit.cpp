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
    gridRow = -1;
    gridCol = -1;
    unitBuffs.clear();

    entity.active = true;
    entity.transform.position = pos;
    entity.transform.syncPrevious();
    entity.sprite.size = {80.f, 80.f};
    entity.sprite.origin = {0.5f, 0.5f};

    // Color-code by element
    const UnitDef& def = getUnitDef(defId);
    switch (def.family) {
        case UnitFamily::Fire:
            entity.sprite.color = {1.0f, 0.6f, 0.3f, 1.f}; // orange
            break;
        case UnitFamily::Frost:
            entity.sprite.color = {0.4f, 0.6f, 1.0f, 1.f}; // blue
            break;
        case UnitFamily::Poison:
            entity.sprite.color = {0.5f, 1.0f, 0.3f, 1.f}; // green-yellow
            break;
        case UnitFamily::Lightning:
            entity.sprite.color = {0.8f, 0.8f, 1.0f, 1.f}; // light blue-white
            break;
        case UnitFamily::Support:
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
        proj->isMagic = isMagicDamage();

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
    float cx = position.x;
    float cy = position.y;
    float spriteSize = 56.f; // unit sprite display size

    // Get the actual unit sprite from atlas
    const auto& unitSprite = atlas.getUnitSprite(unitDefId);

    // Select animation: attack if attacking, otherwise idle
    const SpriteFrame& frame = attacking_
        ? unitSprite.attack.getFrame(attackAnimTimer_)
        : unitSprite.idle.getFrame(animTime_);

    // Brightness increases with level
    float brightness = 1.0f + (level - 1) * 0.08f;
    float r = std::min(brightness, 1.2f);
    float g = std::min(brightness, 1.2f);
    float b = std::min(brightness, 1.2f);

    // Draw unit sprite from atlas
    batch.draw(tex,
               {cx, cy}, {spriteSize, spriteSize},
               frame.uvRect, {r, g, b, 1.f},
               0.f, {0.5f, 0.5f});

    // Attack animation: glow pulse behind sprite
    if (attacking_) {
        const auto& wp = atlas.getWhitePixel();
        float pulse = 0.5f + 0.5f * std::sin(attackAnimTimer_ * 20.f);
        float glowSize = spriteSize + 8.f * pulse;
        batch.draw(tex,
                   cx - glowSize * 0.5f, cy - glowSize * 0.5f, glowSize, glowSize,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   1.f, 1.f, 1.f, 0.15f * pulse);
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
    float base = getUnitDef(unitDefId).range;
    // Back row (row 0) gets +20% range, front row (row 2) gets melee priority
    if (gridRow == 0) return base * 1.2f;
    if (gridRow == 2) return base * 0.85f;
    return base;
}

float Unit::getAtkSpeed() const {
    float baseSpd = getUnitDef(unitDefId).atkSpeed;
    float spdBonus = unitBuffs.getSpdBonus();
    return baseSpd * (1.f + spdBonus);
}

bool Unit::isMagicDamage() const {
    const UnitDef& def = getUnitDef(unitDefId);
    // Fire=physical splash, Frost/Poison/Lightning=magic, Support=physical
    return def.family == UnitFamily::Frost ||
           def.family == UnitFamily::Poison ||
           def.family == UnitFamily::Lightning;
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
        // Include loopCount to prioritize enemies that have looped more
        float progress = static_cast<float>(enemy->loopCount) * 1000000.f +
                         static_cast<float>(enemy->pathIndex) * 1000.f + enemy->pathProgress;
        if (progress > bestProgress) {
            bestProgress = progress;
            bestTarget = enemy;
        }
    }

    return bestTarget;
}
