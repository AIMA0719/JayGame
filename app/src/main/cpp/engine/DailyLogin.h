#ifndef JAYGAME_DAILYLOGIN_H
#define JAYGAME_DAILYLOGIN_H

#include "PlayerData.h"
#include "Currency.h"
#include <string>

namespace DailyLogin {

struct DayReward {
    int gold;
    int diamonds;
    int cards; // random unit cards
};

// 7-day cycle rewards
inline constexpr DayReward DAILY_REWARDS[7] = {
    {  100, 0, 1},  // Day 1
    {  150, 0, 1},  // Day 2
    {  200, 0, 2},  // Day 3
    {  250, 1, 2},  // Day 4
    {  300, 1, 3},  // Day 5
    {  400, 2, 3},  // Day 6
    { 1000, 5, 5},  // Day 7 (big reward)
};

// Check if player can claim today's reward
inline bool canClaim(const std::string& todayDate) {
    auto& pd = PlayerData::get();
    return pd.lastLoginDate != todayDate || pd.lastClaimedDay == 0;
}

// Get today's reward index (0-6)
inline int getRewardDay(const std::string& todayDate) {
    auto& pd = PlayerData::get();
    if (pd.lastLoginDate == todayDate && pd.lastClaimedDay > 0) {
        // Already claimed, show current day
        return (pd.loginStreak - 1) % 7;
    }
    return pd.loginStreak % 7;
}

// Claim today's reward. Returns the day index (0-6) or -1 if already claimed.
inline int claim(const std::string& todayDate) {
    auto& pd = PlayerData::get();

    if (pd.lastLoginDate == todayDate && pd.lastClaimedDay > 0) {
        return -1; // already claimed
    }

    // Check streak continuity (simple: if lastLoginDate was "yesterday" keep streak)
    // For simplicity, if lastLoginDate != todayDate, increment streak
    // If more than 1 day gap, reset streak (we can't easily check without date math in C++)
    if (pd.lastLoginDate.empty() || pd.lastLoginDate == todayDate) {
        // First login or same day
    } else {
        pd.loginStreak++;
    }

    int dayIdx = (pd.loginStreak) % 7;
    const auto& reward = DAILY_REWARDS[dayIdx];

    Currency::addGold(reward.gold);
    Currency::addDiamonds(reward.diamonds);

    // Add random cards to random owned units
    for (int c = 0; c < reward.cards; c++) {
        int unitId = std::rand() % 10; // random base unit
        pd.units[unitId].cards++;
    }

    pd.lastLoginDate = todayDate;
    pd.lastClaimedDay = dayIdx + 1;

    return dayIdx;
}

} // namespace DailyLogin

#endif // JAYGAME_DAILYLOGIN_H
