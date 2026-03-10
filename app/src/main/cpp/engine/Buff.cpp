#include "Buff.h"
#include <algorithm>
#include <cmath>

void BuffContainer::addBuff(const Buff& buff) {
    // Stacking rules:
    // - Same source (same sourceUnitId + same type): refresh duration
    // - Different source: stack up to MAX_STACKS

    for (auto& existing : buffs_) {
        if (existing.active && existing.type == buff.type &&
            existing.sourceUnitId == buff.sourceUnitId) {
            // Same source: refresh duration and update magnitude to max
            existing.duration = buff.duration;
            existing.magnitude = std::max(existing.magnitude, buff.magnitude);
            existing.tickTimer = buff.tickTimer;
            return;
        }
    }

    // Check stack limit for this type from different sources
    int count = countByTypeExcludingSource(buff.type, buff.sourceUnitId);
    if (count >= MAX_STACKS) {
        // Replace the oldest (first found) buff of this type
        for (auto& existing : buffs_) {
            if (existing.active && existing.type == buff.type) {
                existing = buff;
                return;
            }
        }
    }

    buffs_.push_back(buff);
}

void BuffContainer::update(float dt, float& outDotDamage) {
    outDotDamage = 0.f;

    for (auto& buff : buffs_) {
        if (!buff.active) continue;

        buff.duration -= dt;
        if (buff.duration <= 0.f) {
            buff.active = false;
            continue;
        }

        // DoT tick processing
        if (buff.type == BuffType::DoT) {
            buff.tickTimer -= dt;
            while (buff.tickTimer <= 0.f && buff.active) {
                outDotDamage += buff.magnitude;
                buff.totalDotDamage += buff.magnitude;
                buff.tickTimer += buff.tickInterval;
                if (buff.tickInterval <= 0.f) break; // safety
            }
        }
    }

    // Remove inactive buffs
    buffs_.erase(
        std::remove_if(buffs_.begin(), buffs_.end(),
                        [](const Buff& b) { return !b.active; }),
        buffs_.end());
}

void BuffContainer::clear() {
    buffs_.clear();
}

float BuffContainer::getSlowFactor() const {
    float maxSlow = 0.f;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == BuffType::Slow) {
            maxSlow = std::max(maxSlow, buff.magnitude);
        }
    }
    return std::min(maxSlow, 0.9f); // cap at 90% slow
}

float BuffContainer::getArmorReduction() const {
    float total = 0.f;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == BuffType::ArmorBreak) {
            total += buff.magnitude;
        }
    }
    return total;
}

float BuffContainer::getAtkBonus() const {
    float total = 0.f;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == BuffType::AtkUp) {
            total += buff.magnitude;
        }
    }
    return std::min(total, 1.0f); // cap at 100% bonus
}

float BuffContainer::getSpdBonus() const {
    float total = 0.f;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == BuffType::SpdUp) {
            total += buff.magnitude;
        }
    }
    return std::min(total, 1.0f);
}

float BuffContainer::getShieldHP() const {
    float total = 0.f;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == BuffType::Shield) {
            total += buff.magnitude;
        }
    }
    return total;
}

void BuffContainer::absorbDamage(float& damage) {
    for (auto& buff : buffs_) {
        if (!buff.active || buff.type != BuffType::Shield) continue;
        if (damage <= 0.f) return;

        if (buff.magnitude >= damage) {
            buff.magnitude -= damage;
            damage = 0.f;
        } else {
            damage -= buff.magnitude;
            buff.magnitude = 0.f;
            buff.active = false;
        }
    }
}

bool BuffContainer::hasBuffType(BuffType type) const {
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == type) return true;
    }
    return false;
}

int BuffContainer::countByTypeExcludingSource(BuffType type, int sourceId) const {
    int count = 0;
    for (const auto& buff : buffs_) {
        if (buff.active && buff.type == type && buff.sourceUnitId != sourceId) {
            count++;
        }
    }
    return count;
}
