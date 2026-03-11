#ifndef JAYGAME_GRADECOLORS_H
#define JAYGAME_GRADECOLORS_H

#include "MathTypes.h"

// ─── Grade colors (7 grades: Common → Immortal) ───
// Matches Kotlin UnitGrade enum colors in UnitDefs.kt

inline constexpr Vec4 GRADE_COLORS[] = {
    {0.62f, 0.62f, 0.62f, 1.f}, // 0 Common  #9E9E9E
    {0.26f, 0.65f, 0.96f, 1.f}, // 1 Rare    #42A5F5
    {0.67f, 0.28f, 0.74f, 1.f}, // 2 Hero    #AB47BC
    {1.00f, 0.56f, 0.00f, 1.f}, // 3 Legend   #FF8F00
    {0.94f, 0.27f, 0.27f, 1.f}, // 4 Ancient  #EF4444
    {0.98f, 0.75f, 0.14f, 1.f}, // 5 Mythic   #FBBF24
    {0.94f, 0.67f, 0.99f, 1.f}, // 6 Immortal #F0ABFC
};

inline constexpr int GRADE_COUNT = 7;

// ─── Family colors (5 families) ───
// Matches Kotlin UnitFamily enum colors in UnitDefs.kt

inline constexpr Vec4 FAMILY_COLORS[] = {
    {1.00f, 0.42f, 0.21f, 1.f}, // 0 Fire      #FF6B35
    {0.39f, 0.71f, 0.96f, 1.f}, // 1 Frost     #64B5F6
    {0.51f, 0.78f, 0.52f, 1.f}, // 2 Poison    #81C784
    {1.00f, 0.84f, 0.31f, 1.f}, // 3 Lightning #FFD54F
    {0.81f, 0.58f, 0.85f, 1.f}, // 4 Support   #CE93D8
};

inline constexpr int FAMILY_COUNT = 5;

// ─── Helpers ───

inline Vec4 getGradeColor(int grade) {
    if (grade < 0 || grade >= GRADE_COUNT) return GRADE_COLORS[0];
    return GRADE_COLORS[grade];
}

inline Vec4 getFamilyColor(int family) {
    if (family < 0 || family >= FAMILY_COUNT) return FAMILY_COLORS[0];
    return FAMILY_COLORS[family];
}

// Blend two colors: result = a * ratioA + b * (1 - ratioA)
inline Vec4 blendColors(Vec4 a, Vec4 b, float ratioA) {
    float rb = 1.f - ratioA;
    return {
        a.x * ratioA + b.x * rb,
        a.y * ratioA + b.y * rb,
        a.z * ratioA + b.z * rb,
        1.f
    };
}

// Make a brighter version of a color (toward white)
inline Vec4 brighten(Vec4 c, float amount = 0.5f) {
    return {
        std::min(c.x * (1.f - amount) + amount, 1.f),
        std::min(c.y * (1.f - amount) + amount, 1.f),
        std::min(c.z * (1.f - amount) + amount, 1.f),
        c.w
    };
}

#endif // JAYGAME_GRADECOLORS_H
