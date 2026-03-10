#ifndef JAYGAME_PROJECTILEEFFECTS_H
#define JAYGAME_PROJECTILEEFFECTS_H

#include "MathTypes.h"
#include "ParticleSystem.h"
#include "GradeColors.h"
#include "UnitData.h"
#include <cmath>

// ─── Trail particle configs per family ───
// Called every frame for each active projectile to spawn trail particles

inline void spawnProjectileTrail(ParticleSystem& particles, Vec2 pos, Vec2 vel,
                                  int unitDefId) {
    int familyIdx = unitDefId % 5;
    int grade = unitDefId / 5;

    // Trail intensity scales with grade
    float intensity = 0.6f + grade * 0.06f;
    float trailSize = 4.f + grade * 0.5f;

    // Perpendicular direction for spread
    float vLen = vel.length();
    Vec2 perpDir = (vLen > 1.f) ? Vec2{-vel.y / vLen, vel.x / vLen} : Vec2{0.f, 1.f};
    Vec2 backDir = (vLen > 1.f) ? Vec2{-vel.x / vLen, -vel.y / vLen} : Vec2{0.f, 0.f};

    switch (familyIdx) {
        case 0: { // Fire — orange/red ember trail
            Vec4 cStart = {1.f, 0.5f, 0.1f, intensity};
            Vec4 cEnd = {0.8f, 0.1f, 0.0f, 0.f};
            for (int i = 0; i < 2; i++) {
                float spread = ParticleSystem::randRange(-6.f, 6.f);
                particles.spawn(
                    {pos.x + perpDir.x * spread, pos.y + perpDir.y * spread},
                    {backDir.x * 30.f + ParticleSystem::randRange(-10.f, 10.f),
                     backDir.y * 30.f + ParticleSystem::randRange(-10.f, 10.f)},
                    cStart, cEnd,
                    ParticleSystem::randRange(0.15f, 0.3f),
                    trailSize, 1.f,
                    0.f, BlendMode::Additive
                );
            }
            break;
        }
        case 1: { // Frost — blue ice crystal trail
            Vec4 cStart = {0.5f, 0.8f, 1.f, intensity * 0.8f};
            Vec4 cEnd = {0.3f, 0.5f, 0.9f, 0.f};
            for (int i = 0; i < 2; i++) {
                float spread = ParticleSystem::randRange(-5.f, 5.f);
                particles.spawn(
                    {pos.x + perpDir.x * spread, pos.y + perpDir.y * spread},
                    {backDir.x * 20.f + ParticleSystem::randRange(-8.f, 8.f),
                     backDir.y * 20.f + ParticleSystem::randRange(-15.f, -5.f)},
                    cStart, cEnd,
                    ParticleSystem::randRange(0.2f, 0.4f),
                    trailSize * 0.8f, 0.5f,
                    10.f, BlendMode::Normal // slight gravity for falling snow feel
                );
            }
            break;
        }
        case 2: { // Poison — green drip trail
            Vec4 cStart = {0.3f, 0.9f, 0.2f, intensity * 0.9f};
            Vec4 cEnd = {0.1f, 0.5f, 0.1f, 0.f};
            particles.spawn(
                pos,
                {backDir.x * 15.f + ParticleSystem::randRange(-5.f, 5.f),
                 backDir.y * 15.f + ParticleSystem::randRange(5.f, 20.f)},
                cStart, cEnd,
                ParticleSystem::randRange(0.3f, 0.5f),
                trailSize * 0.7f, 2.f, // grows slightly (bubble)
                30.f, BlendMode::Normal // gravity for drip
            );
            break;
        }
        case 3: { // Lightning — electric arc trail
            Vec4 cStart = {0.9f, 0.9f, 1.f, intensity};
            Vec4 cEnd = {0.5f, 0.5f, 1.f, 0.f};
            // Fast-fading bright sparks in zigzag
            for (int i = 0; i < 3; i++) {
                float zigzag = (i % 2 == 0) ? 12.f : -12.f;
                particles.spawn(
                    {pos.x + perpDir.x * zigzag, pos.y + perpDir.y * zigzag},
                    {ParticleSystem::randRange(-40.f, 40.f),
                     ParticleSystem::randRange(-40.f, 40.f)},
                    cStart, cEnd,
                    ParticleSystem::randRange(0.05f, 0.15f), // very short lived
                    trailSize * 0.6f, 0.f,
                    0.f, BlendMode::Additive
                );
            }
            break;
        }
        case 4: { // Support — golden sparkle trail
            Vec4 cStart = {1.f, 0.85f, 0.3f, intensity * 0.7f};
            Vec4 cEnd = {1.f, 0.95f, 0.6f, 0.f};
            particles.spawn(
                {pos.x + ParticleSystem::randRange(-4.f, 4.f),
                 pos.y + ParticleSystem::randRange(-4.f, 4.f)},
                {ParticleSystem::randRange(-15.f, 15.f),
                 ParticleSystem::randRange(-20.f, -5.f)},
                cStart, cEnd,
                ParticleSystem::randRange(0.2f, 0.4f),
                trailSize * 0.5f, 0.f,
                -5.f, BlendMode::Additive // negative gravity = floats up
            );
            break;
        }
    }
}

// ─── Impact effect when projectile hits enemy ───
inline void spawnProjectileImpact(ParticleSystem& particles, Vec2 pos, int unitDefId) {
    int familyIdx = unitDefId % 5;
    int grade = unitDefId / 5;

    int burstCount = 6 + grade * 2;
    float burstSpeed = 60.f + grade * 10.f;

    switch (familyIdx) {
        case 0: { // Fire — explosion burst
            particles.burst(pos, burstCount, burstSpeed, 30.f,
                           {1.f, 0.5f, 0.1f, 0.9f}, {0.8f, 0.1f, 0.0f, 0.f},
                           0.3f, 6.f + grade, 1.f, BlendMode::Additive);
            // Central flash
            particles.spawn(pos, {0.f, 0.f},
                           {1.f, 0.8f, 0.3f, 1.f}, {1.f, 0.3f, 0.0f, 0.f},
                           0.15f, 16.f + grade * 3.f, 0.f,
                           0.f, BlendMode::Additive);
            break;
        }
        case 1: { // Frost — ice shatter
            particles.burst(pos, burstCount, burstSpeed * 0.7f, 20.f,
                           {0.6f, 0.85f, 1.f, 0.9f}, {0.3f, 0.5f, 0.9f, 0.f},
                           0.4f, 5.f + grade, 1.f, BlendMode::Normal);
            // Frost ring
            particles.spawn(pos, {0.f, 0.f},
                           {0.5f, 0.8f, 1.f, 0.7f}, {0.5f, 0.8f, 1.f, 0.f},
                           0.3f, 20.f + grade * 3.f, 30.f + grade * 4.f,
                           0.f, BlendMode::Additive);
            break;
        }
        case 2: { // Poison — toxic splash
            particles.burst(pos, burstCount, burstSpeed * 0.5f, 15.f,
                           {0.3f, 0.9f, 0.2f, 0.8f}, {0.1f, 0.5f, 0.1f, 0.f},
                           0.5f, 5.f + grade, 3.f, BlendMode::Normal);
            // Poison cloud (slow, lingering)
            for (int i = 0; i < 3; i++) {
                particles.spawn(
                    {pos.x + ParticleSystem::randRange(-8.f, 8.f),
                     pos.y + ParticleSystem::randRange(-8.f, 8.f)},
                    {ParticleSystem::randRange(-10.f, 10.f),
                     ParticleSystem::randRange(-15.f, -5.f)},
                    {0.2f, 0.7f, 0.1f, 0.4f}, {0.1f, 0.4f, 0.05f, 0.f},
                    0.6f, 8.f, 14.f, // grows into cloud
                    -3.f, BlendMode::Normal
                );
            }
            break;
        }
        case 3: { // Lightning — electric discharge
            // Fast radial sparks
            particles.burst(pos, burstCount + 4, burstSpeed * 1.3f, 40.f,
                           {0.9f, 0.9f, 1.f, 1.f}, {0.5f, 0.5f, 1.f, 0.f},
                           0.15f, 3.f, 0.f, BlendMode::Additive);
            // Bright flash
            particles.spawn(pos, {0.f, 0.f},
                           {1.f, 1.f, 1.f, 1.f}, {0.7f, 0.7f, 1.f, 0.f},
                           0.1f, 24.f + grade * 4.f, 0.f,
                           0.f, BlendMode::Additive);
            break;
        }
        case 4: { // Support — healing/buff burst
            particles.burst(pos, burstCount, burstSpeed * 0.6f, 20.f,
                           {1.f, 0.85f, 0.3f, 0.8f}, {1.f, 0.95f, 0.6f, 0.f},
                           0.4f, 4.f + grade, 0.f, BlendMode::Additive);
            // Rising sparkles
            for (int i = 0; i < 4; i++) {
                particles.spawn(
                    {pos.x + ParticleSystem::randRange(-10.f, 10.f), pos.y},
                    {ParticleSystem::randRange(-5.f, 5.f), ParticleSystem::randRange(-50.f, -30.f)},
                    {1.f, 0.9f, 0.4f, 0.9f}, {1.f, 1.f, 0.8f, 0.f},
                    0.5f, 3.f, 0.f,
                    -10.f, BlendMode::Additive
                );
            }
            break;
        }
    }
}

#endif // JAYGAME_PROJECTILEEFFECTS_H
