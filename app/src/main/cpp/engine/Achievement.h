#ifndef JAYGAME_ACHIEVEMENT_H
#define JAYGAME_ACHIEVEMENT_H

#include "PlayerData.h"
#include "Currency.h"
#include <functional>
#include <string>

enum class AchievementCategory { Battle, Merge, Collection, Economy, Special };

struct AchievementDef {
    int id;
    const char* name;
    const char* description;
    AchievementCategory category;
    int threshold;
    int goldReward;
    int diamondReward;
};

// 20 achievements
inline constexpr AchievementDef ACHIEVEMENTS[] = {
    // Battle (0-4)
    { 0, "FIRST WIN",     "Win 1 battle",           AchievementCategory::Battle,     1,   100,  0},
    { 1, "VETERAN",       "Win 10 battles",          AchievementCategory::Battle,    10,   500,  5},
    { 2, "COMMANDER",     "Win 50 battles",          AchievementCategory::Battle,    50,  2000, 10},
    { 3, "WAVE MASTER",   "Reach wave 40",           AchievementCategory::Battle,    40,  1000, 10},
    { 4, "SLAYER",        "Kill 1000 enemies",       AchievementCategory::Battle,  1000,  1500, 10},

    // Merge (5-8)
    { 5, "MERGER",        "Merge 10 times",          AchievementCategory::Merge,     10,   200,  0},
    { 6, "FUSION MASTER", "Merge 100 times",         AchievementCategory::Merge,    100,  1000,  5},
    { 7, "ELITE UNIT",    "Create a level 5 unit",   AchievementCategory::Merge,      5,   500,  5},
    { 8, "MAX POWER",     "Create a level 7 unit",   AchievementCategory::Merge,      7,  2000, 20},

    // Collection (9-11)
    { 9, "COLLECTOR",     "Own 5 unique units",      AchievementCategory::Collection,  5,   300,  0},
    {10, "EXPLORER",      "Own 10 unique units",     AchievementCategory::Collection, 10,   800,  5},
    {11, "COMPLETIONIST", "Own all 15 units",        AchievementCategory::Collection, 15,  5000, 50},

    // Economy (12-15)
    {12, "EARNER",        "Earn 1000 gold total",    AchievementCategory::Economy,  1000,   200,  0},
    {13, "WEALTHY",       "Earn 10000 gold total",   AchievementCategory::Economy, 10000,   500,  5},
    {14, "TYCOON",        "Earn 100000 gold total",  AchievementCategory::Economy,100000,  2000, 20},
    {15, "UPGRADER",      "Upgrade a unit to lv5",   AchievementCategory::Economy,     5,   500,  5},

    // Special (16-19)
    {16, "PERFECT",       "Win without losing HP",   AchievementCategory::Special,    1,  1000, 10},
    {17, "SPECIALIST",    "Win with 1 unit type",    AchievementCategory::Special,    1,  1000, 10},
    {18, "BRONZE RANK",   "Reach Silver rank",       AchievementCategory::Special, 1000,   500,  5},
    {19, "DIAMOND RANK",  "Reach Diamond rank",      AchievementCategory::Special, 3000,  3000, 30},
};

inline constexpr int ACHIEVEMENT_COUNT = sizeof(ACHIEVEMENTS) / sizeof(ACHIEVEMENTS[0]);

// Achievement state
class AchievementSystem {
public:
    static AchievementSystem& get() {
        static AchievementSystem instance;
        return instance;
    }

    bool isUnlocked(int id) const {
        if (id < 0 || id >= ACHIEVEMENT_COUNT) return false;
        return unlocked_[id];
    }

    void setUnlocked(int id, bool val) {
        if (id >= 0 && id < ACHIEVEMENT_COUNT) unlocked_[id] = val;
    }

    // Check all achievements after a battle event. Returns ID of newly unlocked, or -1.
    // Call multiple times to get all newly unlocked.
    int checkAndUnlock();

    // Popup state
    bool hasPopup() const { return popupActive_; }
    int getPopupId() const { return popupAchievementId_; }
    float getPopupTimer() const { return popupTimer_; }
    void updatePopup(float dt);
    void dismissPopup() { popupActive_ = false; }

    void reset() {
        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) unlocked_[i] = false;
        popupActive_ = false;
    }

private:
    AchievementSystem() { reset(); }

    bool unlocked_[ACHIEVEMENT_COUNT] = {};
    bool popupActive_ = false;
    int popupAchievementId_ = -1;
    float popupTimer_ = 0.f;
    static constexpr float POPUP_DURATION = 3.f;

    void triggerPopup(int id);
    int getProgress(int id) const;
};

#endif // JAYGAME_ACHIEVEMENT_H
