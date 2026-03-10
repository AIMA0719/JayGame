#ifndef JAYGAME_UNITUPGRADE_H
#define JAYGAME_UNITUPGRADE_H

#include "PlayerData.h"
#include "Currency.h"

namespace UnitUpgrade {

struct UpgradeCost {
    int cards;
    int gold;
};

// Level 1->2 through 6->7 costs
inline constexpr UpgradeCost UPGRADE_COSTS[6] = {
    {  2,   100},  // 1->2
    {  4,   200},  // 2->3
    { 10,   500},  // 3->4
    { 20,  1000},  // 4->5
    { 50,  2000},  // 5->6
    {100,  5000},  // 6->7
};

inline const UpgradeCost* getCost(int currentLevel) {
    if (currentLevel < 1 || currentLevel >= MAX_UNIT_LEVEL) return nullptr;
    return &UPGRADE_COSTS[currentLevel - 1];
}

// Check if unit can be upgraded
inline bool canUpgrade(int unitId) {
    auto& pd = PlayerData::get();
    if (unitId < 0 || unitId >= TOTAL_UNITS) return false;
    auto& unit = pd.units[unitId];
    if (!unit.owned) return false;
    if (unit.level >= MAX_UNIT_LEVEL) return false;

    const auto* cost = getCost(unit.level);
    if (!cost) return false;

    return unit.cards >= cost->cards && Currency::canSpendGold(cost->gold);
}

// Returns true on success
inline bool upgrade(int unitId) {
    if (!canUpgrade(unitId)) return false;

    auto& pd = PlayerData::get();
    auto& unit = pd.units[unitId];
    const auto* cost = getCost(unit.level);

    unit.cards -= cost->cards;
    Currency::spendGold(cost->gold);
    unit.level++;

    if (unit.level > pd.maxUnitLevel) {
        pd.maxUnitLevel = unit.level;
    }

    return true;
}

// Stat boost multiplier for a given level
// +10% ATK, +5% attack speed per level above 1
inline float getATKMultiplier(int level) {
    return 1.f + 0.1f * static_cast<float>(level - 1);
}

inline float getSpdMultiplier(int level) {
    return 1.f + 0.05f * static_cast<float>(level - 1);
}

} // namespace UnitUpgrade

#endif // JAYGAME_UNITUPGRADE_H
