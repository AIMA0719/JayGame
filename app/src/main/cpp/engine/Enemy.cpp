#include "Enemy.h"
#include "SpriteBatch.h"
#include "TextureAsset.h"
#include "SpriteAtlas.h"

#include <cmath>
#include <algorithm>

void Enemy::init(float hp_, float speed_, float armor_, float magicResist_, bool boss, int spReward_, int enemyType_) {
    active = true;
    hp = hp_;
    maxHp = hp_;
    baseSpeed = speed_;
    speed = speed_;
    baseArmor = armor_;
    armor = armor_;
    magicResist = magicResist_;
    isBoss = boss;
    spReward = spReward_;
    size = boss ? 96.f : 48.f;
    pathIndex = 0;
    pathProgress = 0.f;
    loopCount = 0;
    enemyType = enemyType_;
    animTime_ = 0.f;
    position = {0.f, 0.f};
    prevPosition = {0.f, 0.f};
    buffs.clear();
}

void Enemy::update(float dt, const std::vector<Vec2>& waypoints) {
    if (!active || isDead()) return;
    animTime_ += dt;
    if (waypoints.size() < 2) return;

    // Process buffs/debuffs
    float dotDamage = 0.f;
    buffs.update(dt, dotDamage);
    if (dotDamage > 0.f) {
        hp -= dotDamage;
        if (hp <= 0.f) { hp = 0.f; return; }
    }

    // Apply debuff effects to speed and armor
    float slowFactor = buffs.getSlowFactor();
    speed = baseSpeed * (1.f - slowFactor);
    armor = std::max(0.f, baseArmor - buffs.getArmorReduction());

    prevPosition = position;

    // Move along the path
    int nextIndex = pathIndex + 1;
    if (nextIndex >= static_cast<int>(waypoints.size())) {
        // Loop back to start
        pathIndex = 0;
        pathProgress = 0.f;
        loopCount++;
        position = waypoints[0];
        prevPosition = position;
        return;
    }

    Vec2 from = waypoints[pathIndex];
    Vec2 to = waypoints[nextIndex];
    Vec2 diff = to - from;
    float segmentLength = diff.length();

    if (segmentLength < 1e-6f) {
        pathIndex++;
        return;
    }

    float distToMove = speed * dt;
    float progressDelta = distToMove / segmentLength;
    pathProgress += progressDelta;

    while (pathProgress >= 1.f) {
        pathProgress -= 1.f;
        pathIndex++;
        nextIndex = pathIndex + 1;
        if (nextIndex >= static_cast<int>(waypoints.size())) {
            // Loop back to start
            pathIndex = 0;
            pathProgress = 0.f;
            loopCount++;
            position = waypoints[0];
            prevPosition = position;
            return;
        }
        from = waypoints[pathIndex];
        to = waypoints[nextIndex];
        segmentLength = (to - from).length();
        if (segmentLength < 1e-6f) continue;
    }

    from = waypoints[pathIndex];
    to = waypoints[std::min(pathIndex + 1, static_cast<int>(waypoints.size()) - 1)];
    position = Vec2::lerp(from, to, std::min(pathProgress, 1.f));
}

void Enemy::takeDamage(float damage, bool isMagic) {
    float finalDamage;
    if (isMagic) {
        // Magic ignores armor but affected by magic resist
        finalDamage = damage * (1.f - magicResist);
    } else {
        // Physical: FinalDamage = Damage * (100 / (100 + Defense))
        finalDamage = damage * (100.f / (100.f + armor));
    }
    finalDamage = std::max(1.f, finalDamage);
    hp -= finalDamage;
    if (hp < 0.f) hp = 0.f;
}

bool Enemy::isDead() const {
    return hp <= 0.f;
}

Rect Enemy::getBounds() const {
    float halfSize = size * 0.5f;
    return {position.x - halfSize, position.y - halfSize, size, size};
}

void Enemy::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = Vec2::lerp(prevPosition, position, alpha);

    // Select sprite and animation frame
    const auto& sprite = atlas.getEnemySprite(enemyType);
    const auto& frame = sprite.walk.getFrame(animTime_);

    // Boss tint: slightly reddish; normal: white
    Vec4 tint = isBoss ? Vec4{1.f, 0.85f, 0.85f, 1.f} : Vec4{1.f, 1.f, 1.f, 1.f};

    batch.draw(tex, pos, {size, size}, frame.uvRect, tint, 0.f, {0.5f, 0.5f});

    // Health bar - use white pixel for solid color fills
    const auto& wp = atlas.getWhitePixel();
    float barWidth = size;
    float barHeight = 6.f;
    float barX = pos.x - barWidth * 0.5f;
    float barY = pos.y - size * 0.5f - 10.f;

    // Bar background (dark)
    batch.draw(tex, barX, barY, barWidth, barHeight,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.2f, 0.2f, 0.2f, 0.8f);

    // Bar fill (green->red based on HP%)
    float hpRatio = (maxHp > 0.f) ? std::max(0.f, std::min(1.f, hp / maxHp)) : 0.f;
    float fillR = 1.f - hpRatio;
    float fillG = hpRatio;
    batch.draw(tex, barX, barY, barWidth * hpRatio, barHeight,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               fillR, fillG, 0.1f, 0.9f);

    // Debuff icons using atlas sprites instead of colored dots
    float indicatorY = barY + barHeight + 2.f;
    float indicatorX = barX;
    constexpr float INDICATOR_SIZE = 8.f;
    constexpr float INDICATOR_GAP = 2.f;

    if (buffs.hasBuffType(BuffType::Slow)) {
        const auto& icon = atlas.getHud("debuff_slow");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f, 1.f, 1.f, 0.9f}, 0.f, {0.f, 0.f});
        indicatorX += INDICATOR_SIZE + INDICATOR_GAP;
    }
    if (buffs.hasBuffType(BuffType::DoT)) {
        const auto& icon = atlas.getHud("debuff_dot");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f, 1.f, 1.f, 0.9f}, 0.f, {0.f, 0.f});
        indicatorX += INDICATOR_SIZE + INDICATOR_GAP;
    }
    if (buffs.hasBuffType(BuffType::ArmorBreak)) {
        const auto& icon = atlas.getHud("debuff_armor");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f, 1.f, 1.f, 0.9f}, 0.f, {0.f, 0.f});
    }
}
