#ifndef JAYGAME_COMBINATIONTABLE_H
#define JAYGAME_COMBINATIONTABLE_H

#include <cstddef>

// Hidden combination recipe: two specific units at required level produce a new unit
struct CombinationRecipe {
    int unitIdA;         // first unit type ID
    int unitIdB;         // second unit type ID
    int requiredLevel;   // minimum level for both units
    int resultUnitId;    // resulting unit type ID
    int resultLevel;     // level of the resulting unit
};

// Hidden combinations produce special units.
// The result units (IDs 10-14) are defined below in HIDDEN_UNIT_TABLE.
// These units are NOT available from normal summoning.

inline constexpr CombinationRecipe COMBINATION_TABLE[] = {
    // 화염(0) + 냉기(1) = 폭풍(7) at level 3 → Epic Storm at lv1
    { 0,  1, 3, 7, 1 },

    // 번개(4) + 독(2) = 전기독(10) at level 3 → ElectroPoison lv1
    { 4,  2, 3, 10, 1 },

    // 저격(5) + 암살(8) = 처형자(11) at level 4 → Executioner lv1
    { 5,  8, 4, 11, 1 },

    // 강화(6) + 철벽(3) = 성채(12) at level 3 → Citadel lv1
    { 6,  3, 3, 12, 1 },

    // 화염(0) + 용(9) = 불사조(13) at level 5 → Phoenix lv1
    { 0,  9, 5, 13, 1 },
};

inline constexpr size_t COMBINATION_TABLE_SIZE =
    sizeof(COMBINATION_TABLE) / sizeof(COMBINATION_TABLE[0]);

// Check if two units can form a hidden combination.
// Returns pointer to recipe if found, nullptr otherwise.
// Order of unitIdA/B doesn't matter (symmetric).
inline const CombinationRecipe* findCombination(int unitIdA, int unitIdB,
                                                  int levelA, int levelB) {
    for (size_t i = 0; i < COMBINATION_TABLE_SIZE; i++) {
        const auto& r = COMBINATION_TABLE[i];
        bool matchForward = (unitIdA == r.unitIdA && unitIdB == r.unitIdB);
        bool matchReverse = (unitIdA == r.unitIdB && unitIdB == r.unitIdA);

        if ((matchForward || matchReverse) &&
            levelA >= r.requiredLevel && levelB >= r.requiredLevel) {
            return &COMBINATION_TABLE[i];
        }
    }
    return nullptr;
}

#endif // JAYGAME_COMBINATIONTABLE_H
