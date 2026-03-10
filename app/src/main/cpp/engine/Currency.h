#ifndef JAYGAME_CURRENCY_H
#define JAYGAME_CURRENCY_H

#include "PlayerData.h"

namespace Currency {

static constexpr int GOLD_CAP = 999999;
static constexpr int DIAMOND_CAP = 99999;

// Gold operations
inline bool canSpendGold(int amount) {
    return PlayerData::get().gold >= amount;
}

inline bool spendGold(int amount) {
    auto& pd = PlayerData::get();
    if (pd.gold < amount) return false;
    pd.gold -= amount;
    return true;
}

inline void addGold(int amount) {
    auto& pd = PlayerData::get();
    pd.gold += amount;
    if (pd.gold > GOLD_CAP) pd.gold = GOLD_CAP;
    pd.totalGoldEarned += amount;
}

inline int getGold() {
    return PlayerData::get().gold;
}

// Diamond operations
inline bool canSpendDiamonds(int amount) {
    return PlayerData::get().diamonds >= amount;
}

inline bool spendDiamonds(int amount) {
    auto& pd = PlayerData::get();
    if (pd.diamonds < amount) return false;
    pd.diamonds -= amount;
    return true;
}

inline void addDiamonds(int amount) {
    auto& pd = PlayerData::get();
    pd.diamonds += amount;
    if (pd.diamonds > DIAMOND_CAP) pd.diamonds = DIAMOND_CAP;
}

inline int getDiamonds() {
    return PlayerData::get().diamonds;
}

// Gas operations (permanent upgrade currency)
static constexpr int GAS_CAP = 99999;

inline bool canSpendGas(int amount) {
    return PlayerData::get().gas >= amount;
}

inline bool spendGas(int amount) {
    auto& pd = PlayerData::get();
    if (pd.gas < amount) return false;
    pd.gas -= amount;
    return true;
}

inline void addGas(int amount) {
    auto& pd = PlayerData::get();
    pd.gas += amount;
    if (pd.gas > GAS_CAP) pd.gas = GAS_CAP;
}

inline int getGas() {
    return PlayerData::get().gas;
}

// Family upgrade operations (영구 공격력 강화)
inline int getFamilyUpgradeLevel(int familyIdx) {
    if (familyIdx < 0 || familyIdx >= 5) return 0;
    return PlayerData::get().familyUpgrade[familyIdx];
}

inline bool upgradeFamilyATK(int familyIdx, int gasCost) {
    if (familyIdx < 0 || familyIdx >= 5) return false;
    auto& pd = PlayerData::get();
    if (pd.gas < gasCost) return false;
    pd.gas -= gasCost;
    pd.familyUpgrade[familyIdx]++;
    return true;
}

// Family ATK bonus multiplier: each level = +5% ATK
inline float getFamilyATKMultiplier(int familyIdx) {
    int level = getFamilyUpgradeLevel(familyIdx);
    return 1.0f + level * 0.05f;
}

// Trophy operations
inline void addTrophies(int amount) {
    auto& pd = PlayerData::get();
    pd.trophies += amount;
    if (pd.trophies < 0) pd.trophies = 0;
}

inline int getTrophies() {
    return PlayerData::get().trophies;
}

} // namespace Currency

#endif // JAYGAME_CURRENCY_H
