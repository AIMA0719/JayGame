#ifndef JAYGAME_PARTICLESYSTEM_H
#define JAYGAME_PARTICLESYSTEM_H

#include "MathTypes.h"
#include <cstdint>
#include <cstring>

class SpriteBatch;
class SpriteAtlas;

// ─── Blend mode for particles ───
enum class BlendMode : uint8_t {
    Normal = 0,   // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
    Additive = 1, // GL_SRC_ALPHA, GL_ONE (for glows, beams, auras)
};

// ─── Single particle ───
struct Particle {
    Vec2 position;
    Vec2 velocity;
    Vec4 color;         // RGBA start color
    Vec4 colorEnd;      // RGBA end color (lerped over lifetime)
    float life = 0.f;   // remaining life (seconds)
    float maxLife = 1.f;
    float size = 4.f;
    float sizeEnd = 0.f;   // size at end of life
    float rotation = 0.f;
    float rotationSpeed = 0.f;
    float gravity = 0.f;   // Y acceleration (positive = downward in screen coords)
    BlendMode blend = BlendMode::Normal;
    bool active = false;
};

// ─── Emitter configuration (preset for spawning) ───
struct EmitterConfig {
    Vec2 positionMin;
    Vec2 positionMax;       // random offset range from spawn point
    Vec2 velocityMin;
    Vec2 velocityMax;
    Vec4 colorStart{1.f, 1.f, 1.f, 1.f};
    Vec4 colorEnd{1.f, 1.f, 1.f, 0.f};
    float lifeMin = 0.3f;
    float lifeMax = 0.8f;
    float sizeStart = 4.f;
    float sizeEnd = 0.f;
    float gravity = 0.f;
    float rotationMin = 0.f;
    float rotationMax = 0.f;
    float rotationSpeedMin = 0.f;
    float rotationSpeedMax = 0.f;
    BlendMode blend = BlendMode::Normal;
};

// ─── Particle System ───
class ParticleSystem {
public:
    static constexpr int MAX_PARTICLES = 8192;

    ParticleSystem();

    // Spawn a single particle with explicit parameters
    void spawn(Vec2 pos, Vec2 vel, Vec4 colorStart, Vec4 colorEnd,
               float life, float size, float sizeEnd,
               float gravity = 0.f, BlendMode blend = BlendMode::Normal,
               float rotation = 0.f, float rotationSpeed = 0.f);

    // Spawn N particles from an emitter config at a world position
    void emit(const EmitterConfig& config, Vec2 worldPos, int count);

    // Spawn a burst of particles in a radial pattern
    void burst(Vec2 pos, int count, float speed, float speedVariance,
               Vec4 colorStart, Vec4 colorEnd, float life,
               float size, float sizeEnd, BlendMode blend = BlendMode::Additive);

    // Update all active particles
    void update(float dt);

    // Render particles using SpriteBatch white pixel
    // Renders Normal blend particles first, then Additive blend particles
    void render(SpriteBatch& batch, const SpriteAtlas& atlas);

    // Stats
    int activeCount() const { return activeCount_; }
    void clear();

    // Random helpers (public for use by AuraEffect etc.)
    static float randRange(float min, float max);
    static Vec2 randRange(Vec2 min, Vec2 max);

private:
    Particle particles_[MAX_PARTICLES];
    int activeCount_ = 0;
    int nextFree_ = 0; // hint for next free slot
};

#endif // JAYGAME_PARTICLESYSTEM_H
