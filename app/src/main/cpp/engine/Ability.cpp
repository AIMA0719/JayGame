#include "Ability.h"
#include "Unit.h"
#include "UnitData.h"
#include "Enemy.h"
#include "Projectile.h"
#include "Grid.h"

#include <cmath>
#include <algorithm>

namespace Ability {

// Helper: find enemies in radius around a point
static void findEnemiesInRadius(Vec2 center, float radius,
                                 ObjectPool<Enemy>& enemyPool,
                                 SpatialHash<Enemy>& spatialHash,
                                 Enemy* exclude,
                                 std::vector<Enemy*>& outEnemies) {
    outEnemies.clear();
    Rect queryRect(center.x - radius, center.y - radius,
                   radius * 2.f, radius * 2.f);
    auto candidates = spatialHash.query(queryRect);

    float radiusSq = radius * radius;
    for (Enemy* e : candidates) {
        if (!e->active || e->isDead() || e == exclude) continue;
        Vec2 diff = e->position - center;
        if (diff.lengthSq() <= radiusSq) {
            outEnemies.push_back(e);
        }
    }
}

void onProjectileHit(const Unit& sourceUnit, Enemy& hitEnemy,
                     float damage,
                     ObjectPool<Enemy>& enemyPool,
                     ObjectPool<Projectile>& projectilePool,
                     SpatialHash<Enemy>& spatialHash) {

    const UnitDef& def = getUnitDef(sourceUnit.unitDefId);

    switch (def.ability) {
        case AbilityType::None:
            // Pure single target - damage already applied
            break;

        case AbilityType::Splash: {
            // Deal splash damage to enemies in radius
            float radius = def.abilityValue;
            std::vector<Enemy*> nearby;
            findEnemiesInRadius(hitEnemy.position, radius,
                                enemyPool, spatialHash, &hitEnemy, nearby);
            float splashDmg = damage * 0.5f; // 50% splash damage
            for (Enemy* e : nearby) {
                e->takeDamage(splashDmg);
            }
            break;
        }

        case AbilityType::Slow: {
            // Apply slow debuff
            Buff slowBuff;
            slowBuff.type = BuffType::Slow;
            slowBuff.magnitude = def.abilityValue; // e.g., 0.3 = 30% slow
            slowBuff.duration = 2.5f;
            slowBuff.tickTimer = 0.f;
            slowBuff.tickInterval = 0.f;
            slowBuff.sourceUnitId = sourceUnit.unitDefId;
            hitEnemy.buffs.addBuff(slowBuff);
            break;
        }

        case AbilityType::DoT: {
            // Apply damage-over-time
            Buff dotBuff;
            dotBuff.type = BuffType::DoT;
            dotBuff.magnitude = def.abilityValue; // damage per tick
            dotBuff.duration = 3.f;
            dotBuff.tickTimer = 0.5f;
            dotBuff.tickInterval = 0.5f;
            dotBuff.sourceUnitId = sourceUnit.unitDefId;
            hitEnemy.buffs.addBuff(dotBuff);
            break;
        }

        case AbilityType::Chain: {
            // Bounce projectile to nearby enemies
            int bounceCount = static_cast<int>(def.abilityValue);
            float bounceRadius = 150.f;
            float bounceDamage = damage * 0.7f; // 70% per bounce

            Enemy* current = &hitEnemy;
            std::vector<Enemy*> hit;
            hit.push_back(current);

            for (int i = 0; i < bounceCount; i++) {
                std::vector<Enemy*> nearby;
                findEnemiesInRadius(current->position, bounceRadius,
                                    enemyPool, spatialHash, nullptr, nearby);

                Enemy* nextTarget = nullptr;
                float bestDist = bounceRadius * bounceRadius + 1.f;

                for (Enemy* e : nearby) {
                    // Skip already-hit enemies
                    bool alreadyHit = false;
                    for (Enemy* h : hit) {
                        if (h == e) { alreadyHit = true; break; }
                    }
                    if (alreadyHit) continue;

                    float distSq = (e->position - current->position).lengthSq();
                    if (distSq < bestDist) {
                        bestDist = distSq;
                        nextTarget = e;
                    }
                }

                if (!nextTarget) break;

                // Create a chain projectile (visual only, damage applied immediately)
                nextTarget->takeDamage(bounceDamage);
                hit.push_back(nextTarget);
                current = nextTarget;
                bounceDamage *= 0.7f; // diminishing per bounce
            }
            break;
        }

        case AbilityType::Debuff: {
            // Armor break debuff
            Buff armorBreak;
            armorBreak.type = BuffType::ArmorBreak;
            armorBreak.magnitude = 10.f; // flat armor reduction
            armorBreak.duration = 4.f;
            armorBreak.tickTimer = 0.f;
            armorBreak.tickInterval = 0.f;
            armorBreak.sourceUnitId = sourceUnit.unitDefId;
            hitEnemy.buffs.addBuff(armorBreak);
            break;
        }

        case AbilityType::Execute: {
            // Bonus damage when enemy HP is low
            float threshold = def.abilityValue; // e.g., 0.15 = below 15%
            float hpRatio = hitEnemy.hp / hitEnemy.maxHp;
            if (hpRatio < threshold) {
                // Deal 3x bonus damage
                hitEnemy.takeDamage(damage * 2.f); // already took 1x from projectile
            }
            break;
        }

        case AbilityType::SPBonus:
            // SP bonus is handled on kill in BattleScene, not on hit
            break;

        case AbilityType::Buff:
        case AbilityType::Shield:
        case AbilityType::Summon:
            // These are aura/periodic effects, not on-hit
            break;
    }
}

void applyAuraEffects(const Unit& sourceUnit, const Grid& /*grid*/,
                      ObjectPool<Unit>& unitPool) {
    const UnitDef& def = getUnitDef(sourceUnit.unitDefId);
    if (def.ability != AbilityType::Buff) return;

    // Distance-based aura (works with free-moving units)
    constexpr float AURA_RANGE = 150.f;
    constexpr float AURA_RANGE_SQ = AURA_RANGE * AURA_RANGE;

    unitPool.forEach([&](Unit& neighbor) {
        if (!neighbor.active || &neighbor == &sourceUnit) return;
        Vec2 diff = neighbor.position - sourceUnit.position;
        if (diff.lengthSq() <= AURA_RANGE_SQ) {
            Buff atkBuff;
            atkBuff.type = BuffType::AtkUp;
            atkBuff.magnitude = def.abilityValue;
            atkBuff.duration = 1.1f;
            atkBuff.tickTimer = 0.f;
            atkBuff.tickInterval = 0.f;
            atkBuff.sourceUnitId = sourceUnit.unitDefId;
            neighbor.unitBuffs.addBuff(atkBuff);
        }
    });
}

void applyShieldAura(const Unit& sourceUnit, const Grid& /*grid*/,
                     ObjectPool<Unit>& unitPool) {
    const UnitDef& def = getUnitDef(sourceUnit.unitDefId);
    if (def.ability != AbilityType::Shield) return;

    // Distance-based shield aura (works with free-moving units)
    constexpr float SHIELD_RANGE = 150.f;
    constexpr float SHIELD_RANGE_SQ = SHIELD_RANGE * SHIELD_RANGE;

    unitPool.forEach([&](Unit& neighbor) {
        if (!neighbor.active || &neighbor == &sourceUnit) return;
        Vec2 diff = neighbor.position - sourceUnit.position;
        if (diff.lengthSq() <= SHIELD_RANGE_SQ) {
            if (!neighbor.unitBuffs.hasBuffType(BuffType::Shield)) {
                Buff shield;
                shield.type = BuffType::Shield;
                shield.magnitude = def.abilityValue;
                shield.duration = 5.f;
                shield.tickTimer = 0.f;
                shield.tickInterval = 0.f;
                shield.sourceUnitId = sourceUnit.unitDefId;
                neighbor.unitBuffs.addBuff(shield);
            }
        }
    });
}

} // namespace Ability
