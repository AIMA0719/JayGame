#include "Achievement.h"
#include "AndroidOut.h"

int AchievementSystem::getProgress(int id) const {
    auto& pd = PlayerData::get();
    const auto& def = ACHIEVEMENTS[id];

    switch (def.category) {
        case AchievementCategory::Battle:
            switch (id) {
                case 0: return pd.totalWins;                  // Win 1
                case 1: return pd.totalWins;                  // Win 10
                case 2: return pd.totalWins;                  // Win 50
                case 3: return pd.highestWave;                // Reach wave 40
                case 4: return pd.totalKills;                 // Kill 1000
            }
            break;
        case AchievementCategory::Merge:
            switch (id) {
                case 5: return pd.totalMerges;                // Merge 10
                case 6: return pd.totalMerges;                // Merge 100
                case 7: return pd.maxUnitLevel;               // Level 5 unit
                case 8: return pd.maxUnitLevel;               // Level 7 unit
            }
            break;
        case AchievementCategory::Collection: {
            int owned = 0;
            for (int i = 0; i < TOTAL_UNITS; i++) {
                if (pd.units[i].owned) owned++;
            }
            return owned;
        }
        case AchievementCategory::Economy:
            switch (id) {
                case 12: return pd.totalGoldEarned;           // 1000 gold
                case 13: return pd.totalGoldEarned;           // 10000 gold
                case 14: return pd.totalGoldEarned;           // 100000 gold
                case 15: return pd.maxUnitLevel;              // Unit lv5
            }
            break;
        case AchievementCategory::Special:
            switch (id) {
                case 16: return pd.wonWithoutDamage ? 1 : 0;  // Perfect
                case 17: return pd.wonWithSingleType ? 1 : 0; // Specialist
                case 18: return pd.trophies;                  // Silver rank
                case 19: return pd.trophies;                  // Diamond rank
            }
            break;
    }
    return 0;
}

int AchievementSystem::checkAndUnlock() {
    for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
        if (unlocked_[i]) continue;

        int progress = getProgress(i);
        if (progress >= ACHIEVEMENTS[i].threshold) {
            unlocked_[i] = true;

            // Grant rewards
            Currency::addGold(ACHIEVEMENTS[i].goldReward);
            Currency::addDiamonds(ACHIEVEMENTS[i].diamondReward);

            aout << "Achievement unlocked: " << ACHIEVEMENTS[i].name
                 << " (+" << ACHIEVEMENTS[i].goldReward << " gold, +"
                 << ACHIEVEMENTS[i].diamondReward << " diamonds)" << std::endl;

            triggerPopup(i);
            return i;
        }
    }
    return -1;
}

void AchievementSystem::triggerPopup(int id) {
    popupActive_ = true;
    popupAchievementId_ = id;
    popupTimer_ = POPUP_DURATION;
}

void AchievementSystem::updatePopup(float dt) {
    if (!popupActive_) return;
    popupTimer_ -= dt;
    if (popupTimer_ <= 0.f) {
        popupActive_ = false;
    }
}
