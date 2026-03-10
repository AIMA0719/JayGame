#ifndef JAYGAME_ABILITY_H
#define JAYGAME_ABILITY_H

#include "MathTypes.h"
#include "Buff.h"
#include "ObjectPool.h"
#include "SpatialHash.h"

class Unit;
class Enemy;
class Projectile;
class Grid;

// Ability system: processes unit abilities when projectiles hit or on periodic ticks
namespace Ability {

    // Called when a projectile from a unit hits an enemy.
    // Applies the unit's ability effect (splash, slow, DoT, chain, execute, etc.)
    void onProjectileHit(const Unit& sourceUnit, Enemy& hitEnemy,
                         float damage,
                         ObjectPool<Enemy>& enemyPool,
                         ObjectPool<Projectile>& projectilePool,
                         SpatialHash<Enemy>& spatialHash);

    // Called once per frame per unit to apply aura/buff effects to allies.
    // Only relevant for Buff-type units.
    void applyAuraEffects(const Unit& sourceUnit, const Grid& grid,
                          ObjectPool<Unit>& unitPool);

    // Called once per frame per unit to apply shield to player units in range.
    // Only relevant for Shield-type units.
    void applyShieldAura(const Unit& sourceUnit, const Grid& grid,
                         ObjectPool<Unit>& unitPool);

} // namespace Ability

#endif // JAYGAME_ABILITY_H
