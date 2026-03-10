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
