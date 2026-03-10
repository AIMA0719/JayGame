#ifndef JAYGAME_SEASONPASS_H
#define JAYGAME_SEASONPASS_H

#include "PlayerData.h"
#include "Currency.h"

namespace SeasonPass {

static constexpr int MAX_TIERS = 30;
static constexpr int XP_PER_TIER = 100;
static constexpr int XP_PER_BATTLE = 50;

struct TierReward {
    int gold;
    int diamonds;
    int cards;
};

// 30-tier rewards (free track)
inline constexpr TierReward TIER_REWARDS[MAX_TIERS] = {
    {  100, 0, 1}, {  100, 0, 1}, {  150, 0, 2}, {  150, 1, 2}, {  200, 1, 2},  // 1-5
    {  200, 1, 3}, {  250, 1, 3}, {  250, 2, 3}, {  300, 2, 4}, {  500, 3, 5},  // 6-10
    {  300, 1, 3}, {  300, 2, 3}, {  350, 2, 4}, {  350, 2, 4}, {  400, 3, 4},  // 11-15
    {  400, 3, 5}, {  450, 3, 5}, {  450, 3, 5}, {  500, 4, 5}, { 1000, 5, 8},  // 16-20
    {  500, 3, 5}, {  500, 3, 5}, {  600, 4, 6}, {  600, 4, 6}, {  700, 4, 6},  // 21-25
    {  700, 5, 7}, {  800, 5, 7}, {  900, 5, 8}, { 1000, 5, 8}, { 2000,10,15},  // 26-30
};

inline int getCurrentTier() {
    return PlayerData::get().seasonXP / XP_PER_TIER;
}

inline float getTierProgress() {
    int xp = PlayerData::get().seasonXP;
    int tierXP = xp % XP_PER_TIER;
    return static_cast<float>(tierXP) / static_cast<float>(XP_PER_TIER);
}

inline void addBattleXP() {
    PlayerData::get().seasonXP += XP_PER_BATTLE;
}

inline bool canClaimTier(int tier) {
    if (tier < 0 || tier >= MAX_TIERS) return false;
    auto& pd = PlayerData::get();
    return tier < getCurrentTier() && tier >= pd.seasonClaimedTier;
}

// Claim all unclaimed tiers up to current. Returns number of tiers claimed.
inline int claimAvailableTiers() {
    auto& pd = PlayerData::get();
    int current = getCurrentTier();
    if (current > MAX_TIERS) current = MAX_TIERS;

    int claimed = 0;
    for (int t = pd.seasonClaimedTier; t < current; t++) {
        const auto& reward = TIER_REWARDS[t];
        Currency::addGold(reward.gold);
        Currency::addDiamonds(reward.diamonds);

        for (int c = 0; c < reward.cards; c++) {
            int unitId = std::rand() % 10;
            pd.units[unitId].cards++;
        }
        claimed++;
    }
    pd.seasonClaimedTier = current;
    return claimed;
}

} // namespace SeasonPass

#endif // JAYGAME_SEASONPASS_H
