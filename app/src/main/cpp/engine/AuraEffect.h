#ifndef JAYGAME_AURAEFFECT_H
#define JAYGAME_AURAEFFECT_H

#include "MathTypes.h"
#include "ParticleSystem.h"
#include "GradeColors.h"
#include "UnitData.h"
#include <cmath>

// ─── Aura effect system ───
// Spawns continuous flame-like particles around units based on grade and family.
// Grade 0 (Common): no aura
// Grade 1 (Rare): subtle shimmer (3 tendrils)
// Grade 2 (Hero): moderate glow (5 tendrils)
// Grade 3 (Legend): strong flames (8 tendrils)
// Grade 4 (Ancient): intense fire (10 tendrils)
// Grade 5 (Mythic): blazing aura (13 tendrils)
// Grade 6 (Immortal): prismatic dual-layer (16 tendrils + sparks)

struct AuraConfig {
    int tendrilCount;       // number of flame tendrils per spawn cycle
    float height;           // max upward reach (pixels)
    float width;            // horizontal spread (pixels)
    float particleSize;     // start size
    float particleSizeEnd;  // end size
    float alpha;            // start alpha
    float spawnInterval;    // seconds between spawn cycles
    float lifeMin;
    float lifeMax;
    bool additiveBurst;     // Grade 5+ get additive sparks
    int sparkCount;         // number of additive spark particles per cycle
};

// Pre-computed aura configs per grade (indexed 0-6)
inline constexpr AuraConfig AURA_CONFIGS[] = {
    // Grade 0 Common: no aura
    { 0,  0.f,  0.f, 0.f, 0.f, 0.f, 1.f, 0.f, 0.f, false, 0 },
    // Grade 1 Rare: subtle shimmer
    { 3, 25.f, 12.f, 5.f, 1.f, 0.35f, 0.12f, 0.3f, 0.6f, false, 0 },
    // Grade 2 Hero: moderate glow
    { 5, 35.f, 16.f, 6.f, 1.5f, 0.45f, 0.10f, 0.35f, 0.7f, false, 0 },
    // Grade 3 Legend: strong flames
    { 8, 45.f, 20.f, 7.f, 2.f, 0.55f, 0.08f, 0.4f, 0.8f, false, 0 },
    // Grade 4 Ancient: intense fire
    {10, 55.f, 24.f, 8.f, 2.f, 0.65f, 0.07f, 0.4f, 0.85f, false, 0 },
    // Grade 5 Mythic: blazing aura + sparks
    {13, 65.f, 28.f, 9.f, 2.5f, 0.75f, 0.06f, 0.45f, 0.9f, true, 4 },
    // Grade 6 Immortal: prismatic dual-layer + heavy sparks
    {16, 80.f, 32.f, 10.f, 3.f, 0.85f, 0.05f, 0.5f, 1.0f, true, 8 },
};

// ─── Spawn aura particles for a single unit ───
inline void spawnUnitAura(ParticleSystem& particles, Vec2 unitPos, int unitDefId,
                          int level, float animTime) {
    int grade = unitDefId / 5; // 0-6
    if (grade < 0 || grade >= GRADE_COUNT) return;
    if (grade == 0) return; // Common: no aura

    const AuraConfig& cfg = AURA_CONFIGS[grade];
    Vec4 gradeColor = getGradeColor(grade);
    int familyIdx = unitDefId % 5;
    Vec4 familyColor = getFamilyColor(familyIdx);

    // Blend: 55% grade + 45% family
    Vec4 baseColor = blendColors(gradeColor, familyColor, 0.55f);
    baseColor.w = cfg.alpha;

    // End color: same hue but faded
    Vec4 endColor = baseColor;
    endColor.w = 0.f;

    // Level bonus: slightly brighter/larger at higher levels
    float levelBonus = 1.f + (level - 1) * 0.05f;

    // Base position: unit feet (slightly below center)
    float baseY = unitPos.y + 20.f; // feet position

    for (int i = 0; i < cfg.tendrilCount; i++) {
        // Distribute tendrils in an ellipse around unit base
        float angle = (static_cast<float>(i) / static_cast<float>(cfg.tendrilCount)) * 6.2832f;
        // Add time-based rotation for flowing effect
        angle += animTime * 0.8f;

        float spawnX = unitPos.x + std::cos(angle) * cfg.width * 0.5f;
        float spawnY = baseY + std::sin(angle) * cfg.width * 0.2f; // flattened ellipse

        // Velocity: upward with sinusoidal X wobble
        float wobble = std::sin(animTime * 3.f + static_cast<float>(i) * 1.7f) * 15.f;
        float velX = wobble;
        float velY = -(cfg.height / cfg.lifeMax) * (0.7f + ParticleSystem::randRange(0.f, 0.6f));

        float life = ParticleSystem::randRange(cfg.lifeMin, cfg.lifeMax);
        float size = cfg.particleSize * levelBonus;
        float sizeEnd = cfg.particleSizeEnd;

        particles.spawn(
            {spawnX, spawnY}, {velX, velY},
            baseColor, endColor,
            life, size, sizeEnd,
            0.f, BlendMode::Normal
        );
    }

    // Additive sparks for Grade 5+ (Mythic/Immortal)
    if (cfg.additiveBurst) {
        Vec4 sparkColor = brighten(baseColor, 0.6f);
        sparkColor.w = cfg.alpha * 0.7f;
        Vec4 sparkEnd = sparkColor;
        sparkEnd.w = 0.f;

        for (int i = 0; i < cfg.sparkCount; i++) {
            float angle = ParticleSystem::randRange(0.f, 6.2832f);
            float speed = ParticleSystem::randRange(20.f, 50.f);
            float spawnX = unitPos.x + ParticleSystem::randRange(-cfg.width * 0.3f, cfg.width * 0.3f);
            float spawnY = baseY + ParticleSystem::randRange(-8.f, 8.f);

            particles.spawn(
                {spawnX, spawnY},
                {std::cos(angle) * speed, -std::abs(std::sin(angle)) * speed * 1.5f},
                sparkColor, sparkEnd,
                ParticleSystem::randRange(0.2f, 0.5f),
                3.f * levelBonus, 0.5f,
                0.f, BlendMode::Additive
            );
        }

        // Immortal grade: secondary color ring (prismatic effect)
        if (grade == 6) {
            // Cycle through prismatic colors over time
            float hueShift = std::fmod(animTime * 0.5f, 1.f);
            Vec4 prismColor;
            // Simple HSV-like cycle: R→G→B
            if (hueShift < 0.333f) {
                float t = hueShift * 3.f;
                prismColor = {1.f - t, t, 0.f, 0.5f};
            } else if (hueShift < 0.666f) {
                float t = (hueShift - 0.333f) * 3.f;
                prismColor = {0.f, 1.f - t, t, 0.5f};
            } else {
                float t = (hueShift - 0.666f) * 3.f;
                prismColor = {t, 0.f, 1.f - t, 0.5f};
            }
            Vec4 prismEnd = prismColor;
            prismEnd.w = 0.f;

            for (int i = 0; i < 4; i++) {
                float a = (static_cast<float>(i) / 4.f) * 6.2832f + animTime * 1.5f;
                particles.spawn(
                    {unitPos.x + std::cos(a) * 20.f, baseY + std::sin(a) * 8.f},
                    {std::cos(a) * 10.f, -40.f},
                    prismColor, prismEnd,
                    0.4f, 5.f, 1.f,
                    0.f, BlendMode::Additive
                );
            }
        }
    }
}

// ─── Pedestal glow (drawn as a flat ellipse under unit) ───
inline void drawPedestalGlow(SpriteBatch& batch, const SpriteAtlas& atlas,
                              Vec2 unitPos, int grade) {
    if (grade <= 0) return;

    const auto& tex = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();
    Vec4 color = getGradeColor(grade);

    // Scale glow with grade
    float baseWidth = 40.f + grade * 8.f;
    float baseHeight = 12.f + grade * 2.f;
    float alpha = 0.15f + grade * 0.05f;

    color.w = alpha;
    float footY = unitPos.y + 24.f; // below unit sprite

    batch.draw(tex,
               unitPos.x - baseWidth * 0.5f, footY - baseHeight * 0.5f,
               baseWidth, baseHeight,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               color.x, color.y, color.z, color.w);
}

// ─── Level badge (star count indicator) ───
inline void drawLevelBadge(SpriteBatch& batch, const SpriteAtlas& atlas,
                            Vec2 unitPos, int level) {
    if (level <= 1) return;

    const auto& tex = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();

    // Draw small dots for level (max 7)
    float dotSize = 4.f;
    float spacing = 6.f;
    float totalWidth = (level - 1) * spacing;
    float startX = unitPos.x - totalWidth * 0.5f;
    float dotY = unitPos.y - 32.f; // above unit sprite

    for (int i = 0; i < level; i++) {
        // Gold dots for level
        float dx = startX + i * spacing;
        batch.draw(tex,
                   dx - dotSize * 0.5f, dotY - dotSize * 0.5f,
                   dotSize, dotSize,
                   wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                   1.f, 0.85f, 0.2f, 0.9f);
    }
}

#endif // JAYGAME_AURAEFFECT_H
