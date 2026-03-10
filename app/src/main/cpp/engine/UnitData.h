#ifndef JAYGAME_UNITDATA_H
#define JAYGAME_UNITDATA_H

#include <cstdlib>
#include <cstddef>
#include <cassert>

// --------------- Enums ---------------

enum class UnitGrade { Low, Medium, High, Supreme, Transcendent };
enum class UnitFamily { Fire, Frost, Poison, Lightning, Support };
enum class AbilityType { None, Splash, Slow, DoT, Chain, Buff,
    Debuff, Summon, Shield, Execute, SPBonus }; // extended for legacy compat

// Backward compatibility — old enum names mapped to new
enum class UnitRarity { Normal = 0, Rare = 1, Epic = 2, Legendary = 3 };
enum class UnitElement { Physical = 0, Magic = 1, Support = 4 };

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
    int mergeResultId;    // -1 = no further merge (Transcendent)
};

// --------------- Unit table (25 units: 5 grades x 5 families) ---------------
//
// ID layout: grade * 5 + familyIndex
//   Fire(0): 0, 5, 10, 15, 20
//   Frost(1): 1, 6, 11, 16, 21
//   Poison(2): 2, 7, 12, 17, 22
//   Lightning(3): 3, 8, 13, 18, 23
//   Support(4): 4, 9, 14, 19, 24

inline constexpr UnitDef UNIT_TABLE[] = {
    // ===== LOW grade (summonable) =====
    // id  name          grade               family                ATK    spd    range   ability             value  mergeResultId
    {  0, "화염병",   UnitGrade::Low,      UnitFamily::Fire,      25.f,  1.0f,  150.f, AbilityType::Splash, 80.f,   5 },
    {  1, "냉기병",   UnitGrade::Low,      UnitFamily::Frost,     20.f,  0.8f,  160.f, AbilityType::Slow,    0.3f,  6 },
    {  2, "독침병",   UnitGrade::Low,      UnitFamily::Poison,    15.f,  0.9f,  140.f, AbilityType::DoT,    10.f,   7 },
    {  3, "전격병",   UnitGrade::Low,      UnitFamily::Lightning, 22.f,  1.2f,  170.f, AbilityType::Chain,   3.f,   8 },
    {  4, "격려병",   UnitGrade::Low,      UnitFamily::Support,   10.f,  0.5f,  100.f, AbilityType::Buff,    0.2f,  9 },

    // ===== MEDIUM grade =====
    {  5, "화염술사",  UnitGrade::Medium,   UnitFamily::Fire,      50.f,  1.0f,  160.f, AbilityType::Splash, 90.f,  10 },
    {  6, "냉기술사",  UnitGrade::Medium,   UnitFamily::Frost,     40.f,  0.9f,  170.f, AbilityType::Slow,    0.4f, 11 },
    {  7, "독술사",    UnitGrade::Medium,   UnitFamily::Poison,    30.f,  1.0f,  150.f, AbilityType::DoT,    15.f,  12 },
    {  8, "전격술사",  UnitGrade::Medium,   UnitFamily::Lightning, 45.f,  1.3f,  180.f, AbilityType::Chain,   4.f,  13 },
    {  9, "지휘관",    UnitGrade::Medium,   UnitFamily::Support,   20.f,  0.5f,  120.f, AbilityType::Buff,    0.3f, 14 },

    // ===== HIGH grade =====
    { 10, "화염마도사", UnitGrade::High,    UnitFamily::Fire,     100.f,  1.1f,  170.f, AbilityType::Splash,100.f,  15 },
    { 11, "냉기마도사", UnitGrade::High,    UnitFamily::Frost,     80.f,  1.0f,  180.f, AbilityType::Slow,    0.5f, 16 },
    { 12, "독마도사",   UnitGrade::High,    UnitFamily::Poison,    60.f,  1.1f,  160.f, AbilityType::DoT,    20.f,  17 },
    { 13, "전격마도사", UnitGrade::High,    UnitFamily::Lightning, 90.f,  1.4f,  190.f, AbilityType::Chain,   5.f,  18 },
    { 14, "대사제",     UnitGrade::High,    UnitFamily::Support,   40.f,  0.5f,  140.f, AbilityType::Buff,    0.4f, 19 },

    // ===== SUPREME grade =====
    { 15, "불의군주",   UnitGrade::Supreme, UnitFamily::Fire,     200.f,  1.2f,  180.f, AbilityType::Splash,120.f,  20 },
    { 16, "얼음의군주", UnitGrade::Supreme, UnitFamily::Frost,    160.f,  1.0f,  190.f, AbilityType::Slow,    0.6f, 21 },
    { 17, "역병의군주", UnitGrade::Supreme, UnitFamily::Poison,   120.f,  1.2f,  170.f, AbilityType::DoT,    30.f,  22 },
    { 18, "뇌전의군주", UnitGrade::Supreme, UnitFamily::Lightning,180.f,  1.5f,  200.f, AbilityType::Chain,   6.f,  23 },
    { 19, "전쟁의군주", UnitGrade::Supreme, UnitFamily::Support,   80.f,  0.6f,  160.f, AbilityType::Buff,    0.5f, 24 },

    // ===== TRANSCENDENT grade (merge-only, no further merge) =====
    { 20, "불사조",     UnitGrade::Transcendent, UnitFamily::Fire,     400.f,  1.3f,  200.f, AbilityType::Splash,160.f, -1 },
    { 21, "빙하제왕",   UnitGrade::Transcendent, UnitFamily::Frost,    320.f,  1.1f,  200.f, AbilityType::Slow,    0.7f,-1 },
    { 22, "독룡",       UnitGrade::Transcendent, UnitFamily::Poison,   240.f,  1.2f,  180.f, AbilityType::DoT,    40.f, -1 },
    { 23, "폭풍신",     UnitGrade::Transcendent, UnitFamily::Lightning,350.f,  1.5f,  220.f, AbilityType::Chain,   7.f, -1 },
    { 24, "수호신",     UnitGrade::Transcendent, UnitFamily::Support,  150.f,  0.6f,  180.f, AbilityType::Buff,    0.6f,-1 },
};

inline constexpr size_t UNIT_TABLE_SIZE = sizeof(UNIT_TABLE) / sizeof(UNIT_TABLE[0]); // 25

// Only LOW grade units can be summoned from gacha (IDs 0-4)
inline constexpr int SUMMONABLE_UNIT_COUNT = 5;

// --------------- Summon grade probabilities ---------------
// Low=80%, Medium=15%, High=5%, Supreme/Transcendent=merge only

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
//   Low 80%, Medium 15%, High 5%
//   Supreme and Transcendent are merge-only (never rolled).
inline UnitGrade rollGrade() {
    float roll = static_cast<float>(std::rand()) / static_cast<float>(RAND_MAX) * 100.f;
    if (roll < 80.f) return UnitGrade::Low;
    if (roll < 95.f) return UnitGrade::Medium;
    return UnitGrade::High;
}

// Given a deck of up to 5 unit IDs (LOW grade units), roll a grade then pick
// a unit from the deck's families matching that grade.
// deckIds: array of LOW-grade unit IDs in the player's deck (IDs 0-4)
// deckSize: number of entries (1..5)
// Returns a reference to the chosen UnitDef.
inline const UnitDef& rollUnitFromDeck(const int* deckIds, int deckSize) {
    assert(deckSize > 0 && deckSize <= 5);

    UnitGrade grade = rollGrade();

    if (grade == UnitGrade::Low) {
        // Equal weight among all deck entries
        int idx = std::rand() % deckSize;
        return getUnitDef(deckIds[idx]);
    }

    // For Medium/High: find deck units whose family has a unit at the rolled grade.
    // The deck contains LOW ids (0-4). Each LOW id's family index == id itself.
    // Medium id = 5 + familyIndex, High id = 10 + familyIndex.
    int gradeOffset = 0;
    switch (grade) {
        case UnitGrade::Medium: gradeOffset = 5;  break;
        case UnitGrade::High:   gradeOffset = 10; break;
        default: break;
    }

    // Collect candidate unit IDs at the rolled grade from deck families
    int candidates[5];
    int candidateCount = 0;
    for (int i = 0; i < deckSize; i++) {
        int familyIndex = deckIds[i]; // LOW ids 0-4 == family index
        int candidateId = gradeOffset + familyIndex;
        if (candidateId >= 0 && candidateId < static_cast<int>(UNIT_TABLE_SIZE)) {
            candidates[candidateCount++] = candidateId;
        }
    }

    if (candidateCount > 0) {
        int idx = std::rand() % candidateCount;
        return getUnitDef(candidates[idx]);
    }

    // Fallback: random LOW unit from deck
    int idx = std::rand() % deckSize;
    return getUnitDef(deckIds[idx]);
}

// Legacy compatibility: rollRandomUnit delegates to rollUnitFromDeck
inline const UnitDef& rollRandomUnit(const int* deckIds, int deckSize) {
    return rollUnitFromDeck(deckIds, deckSize);
}

#endif // JAYGAME_UNITDATA_H
