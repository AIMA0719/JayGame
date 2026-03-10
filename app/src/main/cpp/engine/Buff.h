#ifndef JAYGAME_BUFF_H
#define JAYGAME_BUFF_H

#include <vector>
#include <algorithm>

enum class BuffType {
    Slow,       // reduce speed by magnitude (0.0-1.0 = percentage)
    DoT,        // damage per tick
    ArmorBreak, // reduce armor by magnitude (flat)
    AtkUp,      // increase ATK of unit by magnitude (0.0-1.0 = percentage)
    SpdUp,      // increase attack speed of unit by magnitude (0.0-1.0 = percentage)
    Shield      // absorb damage (magnitude = remaining shield HP)
};

struct Buff {
    BuffType type;
    float magnitude;     // effect strength
    float duration;      // seconds remaining
    float tickTimer;     // for DoT: time until next tick
    float tickInterval;  // for DoT: seconds between ticks (0.5 typical)
    int sourceUnitId;    // unit def ID that applied this buff (for stacking rules)
    bool active = true;

    // Total damage dealt by this DoT instance (for tracking)
    float totalDotDamage = 0.f;
};

// Container for buffs/debuffs on a single entity
class BuffContainer {
public:
    static constexpr int MAX_STACKS = 3; // max stacks from different sources

    void addBuff(const Buff& buff);
    void update(float dt, float& outDotDamage);
    void clear();

    // Query effective values
    float getSlowFactor() const;      // 0.0 = no slow, 1.0 = fully stopped
    float getArmorReduction() const;
    float getAtkBonus() const;        // 0.0 = no bonus
    float getSpdBonus() const;
    float getShieldHP() const;
    void absorbDamage(float& damage); // reduces damage using shield, updates shield HP

    bool hasBuffType(BuffType type) const;
    int buffCount() const { return static_cast<int>(buffs_.size()); }

    const std::vector<Buff>& getBuffs() const { return buffs_; }

private:
    std::vector<Buff> buffs_;

    int countByTypeExcludingSource(BuffType type, int sourceId) const;
};

#endif // JAYGAME_BUFF_H
