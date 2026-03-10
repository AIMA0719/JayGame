#ifndef JAYGAME_ENEMY_H
#define JAYGAME_ENEMY_H

#include <vector>
#include "MathTypes.h"
#include "Buff.h"

class SpriteBatch;
class TextureAsset;
class SpriteAtlas;

class Enemy {
public:
    bool active = false;
    float hp = 0.f;
    float maxHp = 0.f;
    float baseSpeed = 0.f;    // base speed (pixels per second)
    float speed = 0.f;        // effective speed (after debuffs)
    float baseArmor = 0.f;    // base armor
    float armor = 0.f;        // effective armor (after debuffs)
    float magicResist = 0.f;  // magic damage reduction (0.0 = none, 0.5 = 50% reduction)
    Vec2 position;
    Vec2 prevPosition;        // for interpolation
    int pathIndex = 0;        // current waypoint index
    float pathProgress = 0.f; // 0..1 between waypoints
    bool isBoss = false;
    float size = 48.f;        // render size (48 normal, 96 boss)
    int spReward = 1;         // SP granted on kill
    int loopCount = 0;        // number of times monster has looped the path

    int enemyType = 0;        // 0=normal, 1=fast, 2=tank, 3=flying, 4=boss, 5=miniboss
    float animTime_ = 0.f;

    BuffContainer buffs;      // debuffs applied by units

    void init(float hp, float speed, float armor, float magicResist, bool boss, int spReward, int enemyType = 0);
    void update(float dt, const std::vector<Vec2>& waypoints);
    void takeDamage(float damage, bool isMagic = false);
    bool isDead() const;
    void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;
    Rect getBounds() const;
};

#endif // JAYGAME_ENEMY_H
