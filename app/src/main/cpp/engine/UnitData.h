#ifndef JAYGAME_UNITDATA_H
#define JAYGAME_UNITDATA_H

#include <cstdlib>
#include <cstddef>
#include <cassert>

// --------------- Enums ---------------

enum class UnitGrade { Common, Rare, Hero, Legend, Ancient, Mythic, Immortal };
enum class UnitFamily { Fire, Frost, Poison, Lightning, Support };
enum class AbilityType { None, Splash, Slow, DoT, Chain, Buff,
    Debuff, Summon, Shield, Execute, SPBonus }; // extended for legacy compat

// Backward compatibility — old enum names mapped to new
enum class UnitRarity { Normal = 0, Rare = 1, Epic = 2, Legendary = 3 };
enum class UnitElement { Physical = 0, Magic = 1, Support = 4 };

// Legacy aliases
using UnitGradeLow = UnitGrade;
constexpr UnitGrade UnitGrade_Low = UnitGrade::Common;
constexpr UnitGrade UnitGrade_Medium = UnitGrade::Rare;
constexpr UnitGrade UnitGrade_High = UnitGrade::Hero;
constexpr UnitGrade UnitGrade_Supreme = UnitGrade::Legend;
constexpr UnitGrade UnitGrade_Transcendent = UnitGrade::Ancient;

// --------------- Static unit definition ---------------

struct UnitDef {
    int id;
    const char* name;
    UnitGrade grade;
    UnitFamily family;
    float baseATK;        // damage per hit
    float atkSpeed;       // attacks per second
    float range;          // in logical pixels
    AbilityType ability;
    float abilityValue;   // ability-specific parameter
    int mergeResultId;    // -1 = no further merge (Immortal)
};

// --------------- Unit table (35 units: 7 grades x 5 families) ---------------
//
// ID layout: grade * 5 + familyIndex
//   Fire(0): 0, 5, 10, 15, 20, 25, 30
//   Frost(1): 1, 6, 11, 16, 21, 26, 31
//   Poison(2): 2, 7, 12, 17, 22, 27, 32
//   Lightning(3): 3, 8, 13, 18, 23, 28, 33
//   Support(4): 4, 9, 14, 19, 24, 29, 34

inline constexpr UnitDef UNIT_TABLE[] = {
    // ===== COMMON grade (summonable) =====
    {  0, "루비",      UnitGrade::Common,  UnitFamily::Fire,      25.f,  1.0f,  150.f, AbilityType::Splash, 80.f,   5 },
    {  1, "미스트",    UnitGrade::Common,  UnitFamily::Frost,     20.f,  0.8f,  160.f, AbilityType::Slow,    0.3f,  6 },
    {  2, "베놈",      UnitGrade::Common,  UnitFamily::Poison,    15.f,  0.9f,  140.f, AbilityType::DoT,    10.f,   7 },
    {  3, "스파크",    UnitGrade::Common,  UnitFamily::Lightning, 22.f,  1.2f,  170.f, AbilityType::Chain,   3.f,   8 },
    {  4, "뮤즈",      UnitGrade::Common,  UnitFamily::Support,   10.f,  0.5f,  100.f, AbilityType::Buff,    0.2f,  9 },

    // ===== RARE grade =====
    {  5, "카르마",    UnitGrade::Rare,    UnitFamily::Fire,      50.f,  1.0f,  160.f, AbilityType::Splash, 90.f,  10 },
    {  6, "프로스트",  UnitGrade::Rare,    UnitFamily::Frost,     40.f,  0.9f,  170.f, AbilityType::Slow,    0.4f, 11 },
    {  7, "바이퍼",    UnitGrade::Rare,    UnitFamily::Poison,    30.f,  1.0f,  150.f, AbilityType::DoT,    15.f,  12 },
    {  8, "볼트",      UnitGrade::Rare,    UnitFamily::Lightning, 45.f,  1.3f,  180.f, AbilityType::Chain,   4.f,  13 },
    {  9, "가디언",    UnitGrade::Rare,    UnitFamily::Support,   20.f,  0.5f,  120.f, AbilityType::Buff,    0.3f, 14 },

    // ===== HERO grade =====
    { 10, "이그니스",  UnitGrade::Hero,    UnitFamily::Fire,     100.f,  1.1f,  170.f, AbilityType::Splash,100.f,  15 },
    { 11, "블리자드",  UnitGrade::Hero,    UnitFamily::Frost,     80.f,  1.0f,  180.f, AbilityType::Slow,    0.5f, 16 },
    { 12, "플레이그",  UnitGrade::Hero,    UnitFamily::Poison,    60.f,  1.1f,  160.f, AbilityType::DoT,    20.f,  17 },
    { 13, "썬더",      UnitGrade::Hero,    UnitFamily::Lightning, 90.f,  1.4f,  190.f, AbilityType::Chain,   5.f,  18 },
    { 14, "오라클",    UnitGrade::Hero,    UnitFamily::Support,   40.f,  0.5f,  140.f, AbilityType::Buff,    0.4f, 19 },

    // ===== LEGEND grade =====
    { 15, "인페르노",  UnitGrade::Legend,  UnitFamily::Fire,     200.f,  1.2f,  180.f, AbilityType::Splash,120.f,  20 },
    { 16, "아이스본",  UnitGrade::Legend,  UnitFamily::Frost,    160.f,  1.0f,  190.f, AbilityType::Slow,    0.6f, 21 },
    { 17, "코로시브",  UnitGrade::Legend,  UnitFamily::Poison,   120.f,  1.2f,  170.f, AbilityType::DoT,    30.f,  22 },
    { 18, "스톰",      UnitGrade::Legend,  UnitFamily::Lightning,180.f,  1.5f,  200.f, AbilityType::Chain,   6.f,  23 },
    { 19, "발키리",    UnitGrade::Legend,  UnitFamily::Support,   80.f,  0.6f,  160.f, AbilityType::Buff,    0.5f, 24 },

    // ===== ANCIENT grade =====
    { 20, "화산왕",    UnitGrade::Ancient, UnitFamily::Fire,     350.f,  1.3f,  200.f, AbilityType::Splash,160.f,  25 },
    { 21, "빙하제왕",  UnitGrade::Ancient, UnitFamily::Frost,    280.f,  1.1f,  200.f, AbilityType::Slow,    0.7f, 26 },
    { 22, "헤카테",    UnitGrade::Ancient, UnitFamily::Poison,   220.f,  1.2f,  180.f, AbilityType::DoT,    40.f,  27 },
    { 23, "뇌왕",      UnitGrade::Ancient, UnitFamily::Lightning,320.f,  1.5f,  220.f, AbilityType::Chain,   7.f,  28 },
    { 24, "아르카나",  UnitGrade::Ancient, UnitFamily::Support,  150.f,  0.6f,  180.f, AbilityType::Buff,    0.6f, 29 },

    // ===== MYTHIC grade =====
    { 25, "피닉스",    UnitGrade::Mythic,  UnitFamily::Fire,     550.f,  1.4f,  220.f, AbilityType::Splash,200.f,  30 },
    { 26, "유키",      UnitGrade::Mythic,  UnitFamily::Frost,    450.f,  1.1f,  220.f, AbilityType::Slow,    0.8f, 31 },
    { 27, "니드호그",  UnitGrade::Mythic,  UnitFamily::Poison,   400.f,  1.3f,  200.f, AbilityType::DoT,    50.f,  32 },
    { 28, "토르",      UnitGrade::Mythic,  UnitFamily::Lightning,500.f,  1.6f,  240.f, AbilityType::Chain,   8.f,  33 },
    { 29, "세라핌",    UnitGrade::Mythic,  UnitFamily::Support,  250.f,  0.7f,  200.f, AbilityType::Buff,    0.7f, 34 },

    // ===== IMMORTAL grade (no further merge) =====
    { 30, "태양신 라",  UnitGrade::Immortal, UnitFamily::Fire,     800.f,  1.5f,  250.f, AbilityType::Splash,250.f, -1 },
    { 31, "크로노스",   UnitGrade::Immortal, UnitFamily::Frost,    700.f,  1.2f,  250.f, AbilityType::Slow,    0.9f,-1 },
    { 32, "아포칼립스", UnitGrade::Immortal, UnitFamily::Poison,   650.f,  1.3f,  250.f, AbilityType::DoT,    60.f, -1 },
    { 33, "제우스",     UnitGrade::Immortal, UnitFamily::Lightning,750.f,  1.7f,  250.f, AbilityType::Chain,   9.f, -1 },
    { 34, "가이아",     UnitGrade::Immortal, UnitFamily::Support,  350.f,  0.8f,  250.f, AbilityType::Buff,    0.8f,-1 },
};

inline constexpr size_t UNIT_TABLE_SIZE = sizeof(UNIT_TABLE) / sizeof(UNIT_TABLE[0]); // 35

// Only COMMON grade units can be summoned from gacha (IDs 0-4)
inline constexpr int SUMMONABLE_UNIT_COUNT = 5;

// --------------- Summon grade probabilities ---------------
// Common=60%, Rare=25%, Hero=12%, Legend=3%, Ancient/Mythic/Immortal=merge only

// --------------- Helpers ---------------

inline const UnitDef& getUnitDef(int id) {
    assert(id >= 0 && id < static_cast<int>(UNIT_TABLE_SIZE));
    return UNIT_TABLE[id];
}

inline UnitGrade getGradeForId(int id) {
    assert(id >= 0 && id < static_cast<int>(UNIT_TABLE_SIZE));
    return UNIT_TABLE[id].grade;
}

inline UnitFamily getFamilyForId(int id) {
    assert(id >= 0 && id < static_cast<int>(UNIT_TABLE_SIZE));
    return UNIT_TABLE[id].family;
}

inline int getMergeResult(int unitId) {
    assert(unitId >= 0 && unitId < static_cast<int>(UNIT_TABLE_SIZE));
    return UNIT_TABLE[unitId].mergeResultId;
}

// Roll a grade based on summon probabilities:
//   Common 60%, Rare 25%, Hero 12%, Legend 3%
//   Ancient, Mythic and Immortal are merge-only (never rolled).
inline UnitGrade rollGrade() {
    float roll = static_cast<float>(std::rand()) / static_cast<float>(RAND_MAX) * 100.f;
    if (roll < 60.f) return UnitGrade::Common;
    if (roll < 85.f) return UnitGrade::Rare;
    if (roll < 97.f) return UnitGrade::Hero;
    return UnitGrade::Legend;
}

// Given a deck of up to 5 unit IDs (COMMON grade units), roll a grade then pick
// a unit from the deck's families matching that grade.
inline const UnitDef& rollUnitFromDeck(const int* deckIds, int deckSize) {
    assert(deckSize > 0 && deckSize <= 5);

    UnitGrade grade = rollGrade();

    if (grade == UnitGrade::Common) {
        int idx = std::rand() % deckSize;
        return getUnitDef(deckIds[idx]);
    }

    // For Rare/Hero/Legend: find deck units whose family has a unit at the rolled grade.
    int gradeOffset = 0;
    switch (grade) {
        case UnitGrade::Rare:   gradeOffset = 5;  break;
        case UnitGrade::Hero:   gradeOffset = 10; break;
        case UnitGrade::Legend:  gradeOffset = 15; break;
        default: break;
    }

    int candidates[5];
    int candidateCount = 0;
    for (int i = 0; i < deckSize; i++) {
        int familyIndex = deckIds[i]; // COMMON ids 0-4 == family index
        int candidateId = gradeOffset + familyIndex;
        if (candidateId >= 0 && candidateId < static_cast<int>(UNIT_TABLE_SIZE)) {
            candidates[candidateCount++] = candidateId;
        }
    }

    if (candidateCount > 0) {
        int idx = std::rand() % candidateCount;
        return getUnitDef(candidates[idx]);
    }

    // Fallback: random COMMON unit from deck
    int idx = std::rand() % deckSize;
    return getUnitDef(deckIds[idx]);
}

// Legacy compatibility
inline const UnitDef& rollRandomUnit(const int* deckIds, int deckSize) {
    return rollUnitFromDeck(deckIds, deckSize);
}

#endif // JAYGAME_UNITDATA_H
