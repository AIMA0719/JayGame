#ifndef JAYGAME_PLAYERDATA_H
#define JAYGAME_PLAYERDATA_H

#include <string>
#include <array>

// Total units in the game
static constexpr int TOTAL_UNITS = 25;
static constexpr int MAX_UNIT_LEVEL = 7;

// Trophy rank thresholds
enum class TrophyRank { Bronze, Silver, Gold, Diamond, Master };

inline TrophyRank getTrophyRank(int trophies) {
    if (trophies >= 4000) return TrophyRank::Master;
    if (trophies >= 3000) return TrophyRank::Diamond;
    if (trophies >= 2000) return TrophyRank::Gold;
    if (trophies >= 1000) return TrophyRank::Silver;
    return TrophyRank::Bronze;
}

inline const char* getRankName(TrophyRank rank) {
    switch (rank) {
        case TrophyRank::Bronze:  return "BRONZE";
        case TrophyRank::Silver:  return "SILVER";
        case TrophyRank::Gold:    return "GOLD";
        case TrophyRank::Diamond: return "DIAMOND";
        case TrophyRank::Master:  return "MASTER";
    }
    return "BRONZE";
}

// Per-unit data
struct UnitProgress {
    bool owned = false;
    int cards = 0;
    int level = 1;
};

// All persistent player state
struct PlayerData {
    // Currencies
    int gold = 500;
    int diamonds = 0;
    int gas = 0;           // 영구 강화 재화

    // Family ATK upgrade levels (계열별 영구 공격력 강화)
    int familyUpgrade[5] = {0, 0, 0, 0, 0}; // 화염, 냉기, 독, 번개, 보조

    // Progression
    int trophies = 0;
    int playerLevel = 1;
    int totalXP = 0;

    // Units
    std::array<UnitProgress, TOTAL_UNITS> units;

    // Deck (5 unit IDs)
    int deck[5] = {0, 1, 2, 3, 4};

    // Stats (for achievements)
    int totalWins = 0;
    int totalLosses = 0;
    int totalKills = 0;
    int totalMerges = 0;
    int totalGoldEarned = 0;
    int highestWave = 0;
    int maxUnitLevel = 1;
    bool wonWithoutDamage = false;  // perfect win flag
    bool wonWithSingleType = false; // mono-unit win flag

    // Settings
    bool soundEnabled = true;
    bool musicEnabled = true;

    // Daily login
    std::string lastLoginDate;
    int loginStreak = 0;
    int lastClaimedDay = 0; // 0 = not claimed today

    // Season pass
    int seasonXP = 0;
    int seasonClaimedTier = 0; // highest tier claimed

    // Stage / difficulty
    int stageId = 0;
    int difficulty = 0;
    int stageMaxWaves = 40;

    // Save version
    int saveVersion = 1;

    // Singleton access
    static PlayerData& get() {
        static PlayerData instance;
        return instance;
    }

    void initDefaults() {
        gold = 500;
        diamonds = 0;
        gas = 0;
        for (int i = 0; i < 5; i++) familyUpgrade[i] = 0;
        trophies = 0;
        playerLevel = 1;
        totalXP = 0;

        for (int i = 0; i < TOTAL_UNITS; i++) {
            units[i].owned = (i < 10); // base 10 owned
            units[i].cards = 0;
            units[i].level = 1;
        }

        deck[0] = 0; deck[1] = 1; deck[2] = 2; deck[3] = 3; deck[4] = 4;

        totalWins = 0;
        totalLosses = 0;
        totalKills = 0;
        totalMerges = 0;
        totalGoldEarned = 0;
        highestWave = 0;
        maxUnitLevel = 1;
        wonWithoutDamage = false;
        wonWithSingleType = false;

        soundEnabled = true;
        musicEnabled = true;

        lastLoginDate = "";
        loginStreak = 0;
        lastClaimedDay = 0;

        seasonXP = 0;
        seasonClaimedTier = 0;

        stageId = 0;
        difficulty = 0;
        stageMaxWaves = 40;

        saveVersion = 1;
    }

private:
    PlayerData() { initDefaults(); }
};

#endif // JAYGAME_PLAYERDATA_H
