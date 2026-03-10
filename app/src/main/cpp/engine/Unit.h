#ifndef JAYGAME_UNIT_H
#define JAYGAME_UNIT_H

#include "MathTypes.h"
#include "Entity.h"
#include "Buff.h"
#include "ObjectPool.h"
#include "SpatialHash.h"

class Enemy;
class Projectile;
class SpriteBatch;
class SpriteAtlas;
class ParticleSystem;

class Unit {
public:
    bool active = false;
    int unitDefId = -1;      // references UnitData table
    int level = 1;           // 1-7 star
    Vec2 position;           // world position (cell center)
    float attackTimer = 0.f; // countdown to next attack
    Entity entity;           // for rendering
    BuffContainer unitBuffs; // buffs received from aura units

    // Last target for ability tracking
    Enemy* lastTarget = nullptr;

    float animTime_ = 0.f;
    bool attacking_ = false;
    float attackAnimTimer_ = 0.f;  // countdown for attack animation display

    int gridRow = -1;            // stored row for management
    int gridCol = -1;            // stored col

    // Free movement
    Vec2 homePosition;           // grid cell center (wander anchor)
    Vec2 wanderTarget;           // current wander destination
    float moveSpeed = 50.f;      // chase speed (pixels/sec)
    float wanderTimer = 0.f;     // time until next wander point

    void init(int defId, Vec2 pos);
    void update(float dt, ObjectPool<Enemy>& enemies, ObjectPool<Projectile>& projectiles,
                SpatialHash<Enemy>& spatialHash);
    void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;
    void spawnAura(ParticleSystem& particles) const;

    float getDamage() const;    // baseATK * level multiplier * buff bonus
    float getRange() const;     // range from UnitDef with row bonus
    float getAtkSpeed() const;  // atkSpeed from UnitDef * buff bonus
    bool isMagicDamage() const; // true if unit family is Frost/Poison/Lightning

private:
    // Level multiplier table: level 1=1.0, 2=1.5, 3=2.2, 4=3.2, 5=4.5, 6=6.0, 7=8.0
    static constexpr float LEVEL_MULTIPLIER[7] = {1.0f, 1.5f, 2.2f, 3.2f, 4.5f, 6.0f, 8.0f};

    Enemy* findTarget(SpatialHash<Enemy>& spatialHash);
};

#endif // JAYGAME_UNIT_H
