#include "Unit.h"
#include "UnitData.h"
#include "Enemy.h"
#include "Projectile.h"
#include "Grid.h"
#include "SpriteBatch.h"
#include "SpriteAtlas.h"
#include "TextureAsset.h"
#include "AuraEffect.h"
#include "ParticleSystem.h"

#include <cmath>
#include <cstdlib>
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

    // Free movement init
    homePosition = pos;
    wanderTarget = pos;
    wanderTimer = 0.5f + (std::rand() % 200) * 0.01f;

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

    // Move speed by family (pixels/sec)
    switch (def.family) {
        case UnitFamily::Fire:      moveSpeed = 60.f; break;
        case UnitFamily::Frost:     moveSpeed = 30.f; break;
        case UnitFamily::Poison:    moveSpeed = 40.f; break;
        case UnitFamily::Lightning: moveSpeed = 35.f; break;
        case UnitFamily::Support:   moveSpeed = 50.f; break;
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

    // Update unit buffs
    float dummyDot = 0.f;
    unitBuffs.update(dt, dummyDot);

    // ── Free Movement ──
    wanderTimer -= dt;

    // Find enemies in detection range (1.5x attack range) for chasing
    float atkRange = getRange();
    float detRange = atkRange * 1.5f;
    Rect detRect(position.x - detRange, position.y - detRange, detRange * 2.f, detRange * 2.f);
    auto candidates = spatialHash.query(detRect);

    Enemy* chaseTarget = nullptr;
    float bestDistSq = detRange * detRange;
    for (Enemy* e : candidates) {
        if (!e->active || e->isDead()) continue;
        float dSq = (e->position - position).lengthSq();
        if (dSq < bestDistSq) {
            bestDistSq = dSq;
            chaseTarget = e;
        }
    }

    if (chaseTarget) {
        float dist = std::sqrt(bestDistSq);
        // Move toward enemy if outside 70% of attack range
        if (dist > atkRange * 0.7f) {
            Vec2 dir = (chaseTarget->position - position).normalized();
            float step = moveSpeed * dt;
            float targetDist = dist - atkRange * 0.6f;
            if (targetDist > 0.f && step > targetDist) step = targetDist;
            if (step > 0.f && targetDist > 0.f) {
                position = position + dir * step;
            }
        }
    } else {
        // Wander near home position
        if (wanderTimer <= 0.f) {
            wanderTarget.x = homePosition.x + ((std::rand() % 100) - 50) * 0.5f;
            wanderTarget.y = homePosition.y + ((std::rand() % 100) - 50) * 0.3f;
            wanderTarget.x = std::max(Grid::GRID_X + 15.f, std::min(Grid::GRID_X + Grid::GRID_W - 15.f, wanderTarget.x));
            wanderTarget.y = std::max(Grid::GRID_Y + 15.f, std::min(Grid::GRID_Y + Grid::GRID_H - 15.f, wanderTarget.y));
            wanderTimer = 1.5f + (std::rand() % 200) * 0.01f;
        }
        Vec2 diff = wanderTarget - position;
        float dist = diff.length();
        if (dist > 2.f) {
            Vec2 dir = diff.normalized();
            float step = moveSpeed * 0.3f * dt;
            if (step > dist) step = dist;
            position = position + dir * step;
        }
    }

    // Clamp within field bounds
    position.x = std::max(Grid::GRID_X + 10.f, std::min(Grid::GRID_X + Grid::GRID_W - 10.f, position.x));
    position.y = std::max(Grid::GRID_Y + 10.f, std::min(Grid::GRID_Y + Grid::GRID_H - 10.f, position.y));
    entity.transform.position = position;

    // ── Attack ──
    attackTimer -= dt;
    if (attackTimer > 0.f) return;

    // Find target within attack range
    Enemy* target = findTarget(spatialHash);
    if (!target) return;

    lastTarget = target;

    // Fire projectile
    Projectile* proj = projectiles.acquire();
    if (proj) {
        proj->init(position, target, getDamage());
        proj->sourceUnitId = unitDefId;
        proj->isMagic = isMagicDamage();

        // Send unitDefId as projType so Compose can render unique effects per unit
        proj->projType = unitDefId;

        attacking_ = true;
        attackAnimTimer_ = 0.3f;
    }

    // Reset attack timer
    attackTimer = 1.f / getAtkSpeed();
}

void Unit::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    float cx = position.x;
    float cy = position.y;
    float spriteSize = 56.f;
    int grade = unitDefId / 5;

    // 1. Pedestal glow (flat ellipse under unit, grade-colored)
    drawPedestalGlow(batch, atlas, {cx, cy}, grade);

    // 2. Get the actual unit sprite from atlas
    const auto& unitSprite = atlas.getUnitSprite(unitDefId);
    const SpriteFrame& frame = attacking_
        ? unitSprite.attack.getFrame(attackAnimTimer_)
        : unitSprite.idle.getFrame(animTime_);

    // Brightness increases with level; grade adds subtle tint
    float brightness = 1.0f + (level - 1) * 0.08f;
    Vec4 gradeCol = getGradeColor(grade);
    float tintAmount = grade * 0.04f; // subtle grade tint
    float r = std::min(brightness * (1.f - tintAmount) + gradeCol.x * tintAmount, 1.2f);
    float g = std::min(brightness * (1.f - tintAmount) + gradeCol.y * tintAmount, 1.2f);
    float b = std::min(brightness * (1.f - tintAmount) + gradeCol.z * tintAmount, 1.2f);

    // 3. Draw unit sprite
    batch.draw(tex,
               {cx, cy}, {spriteSize, spriteSize},
               frame.uvRect, {r, g, b, 1.f},
               0.f, {0.5f, 0.5f});

    // 4. Attack animation: glow pulse with family color
    if (attacking_) {
        const auto& wp = atlas.getWhitePixel();
        int familyIdx = unitDefId % 5;
        Vec4 famCol = getFamilyColor(familyIdx);
        float pulse = 0.5f + 0.5f * std::sin(attackAnimTimer_ * 20.f);
        float glowSize = spriteSize + 8.f * pulse + grade * 2.f;
        batch.draw(tex,
                   cx - glowSize * 0.5f, cy - glowSize * 0.5f, glowSize, glowSize,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   famCol.x, famCol.y, famCol.z, 0.2f * pulse);
    }

    // 5. Level badge (gold dots above sprite)
    drawLevelBadge(batch, atlas, {cx, cy}, level);
}

void Unit::spawnAura(ParticleSystem& particles) const {
    if (!active) return;
    spawnUnitAura(particles, position, unitDefId, level, animTime_);
}

float Unit::getDamage() const {
    const UnitDef& def = getUnitDef(unitDefId);
    int idx = (level >= 1 && level <= 7) ? (level - 1) : 0;
    float baseDmg = def.baseATK * LEVEL_MULTIPLIER[idx];
    float atkBonus = unitBuffs.getAtkBonus();
    return baseDmg * (1.f + atkBonus);
}

float Unit::getRange() const {
    int grade = unitDefId / 5;
    // Legendary and Transcendent: map-wide attack range
    if (grade >= 3) return 2000.f;
    return getUnitDef(unitDefId).range;
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
