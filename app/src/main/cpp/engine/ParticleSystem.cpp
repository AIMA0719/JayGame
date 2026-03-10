#include "ParticleSystem.h"
#include "SpriteBatch.h"
#include "SpriteAtlas.h"
#include <cstdlib>
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

ParticleSystem::ParticleSystem() {
    clear();
}

float ParticleSystem::randRange(float min, float max) {
    if (min >= max) return min;
    float t = static_cast<float>(std::rand()) / static_cast<float>(RAND_MAX);
    return min + t * (max - min);
}

Vec2 ParticleSystem::randRange(Vec2 min, Vec2 max) {
    return {randRange(min.x, max.x), randRange(min.y, max.y)};
}

void ParticleSystem::clear() {
    for (int i = 0; i < MAX_PARTICLES; i++) {
        particles_[i].active = false;
    }
    activeCount_ = 0;
    nextFree_ = 0;
}

void ParticleSystem::spawn(Vec2 pos, Vec2 vel, Vec4 colorStart, Vec4 colorEnd,
                           float life, float size, float sizeEnd,
                           float gravity, BlendMode blend,
                           float rotation, float rotationSpeed) {
    // Find a free slot starting from hint
    int start = nextFree_;
    for (int i = 0; i < MAX_PARTICLES; i++) {
        int idx = (start + i) % MAX_PARTICLES;
        if (!particles_[idx].active) {
            Particle& p = particles_[idx];
            p.position = pos;
            p.velocity = vel;
            p.color = colorStart;
            p.colorEnd = colorEnd;
            p.life = life;
            p.maxLife = life;
            p.size = size;
            p.sizeEnd = sizeEnd;
            p.rotation = rotation;
            p.rotationSpeed = rotationSpeed;
            p.gravity = gravity;
            p.blend = blend;
            p.active = true;
            activeCount_++;
            nextFree_ = (idx + 1) % MAX_PARTICLES;
            return;
        }
    }
    // Pool full — overwrite oldest (slot 0 as fallback)
    Particle& p = particles_[nextFree_];
    p.position = pos;
    p.velocity = vel;
    p.color = colorStart;
    p.colorEnd = colorEnd;
    p.life = life;
    p.maxLife = life;
    p.size = size;
    p.sizeEnd = sizeEnd;
    p.rotation = rotation;
    p.rotationSpeed = rotationSpeed;
    p.gravity = gravity;
    p.blend = blend;
    p.active = true;
    nextFree_ = (nextFree_ + 1) % MAX_PARTICLES;
}

void ParticleSystem::emit(const EmitterConfig& config, Vec2 worldPos, int count) {
    for (int i = 0; i < count; i++) {
        Vec2 offset = randRange(config.positionMin, config.positionMax);
        Vec2 vel = randRange(config.velocityMin, config.velocityMax);
        float life = randRange(config.lifeMin, config.lifeMax);
        float rot = randRange(config.rotationMin, config.rotationMax);
        float rotSpd = randRange(config.rotationSpeedMin, config.rotationSpeedMax);

        spawn(worldPos + offset, vel,
              config.colorStart, config.colorEnd,
              life, config.sizeStart, config.sizeEnd,
              config.gravity, config.blend,
              rot, rotSpd);
    }
}

void ParticleSystem::burst(Vec2 pos, int count, float speed, float speedVariance,
                           Vec4 colorStart, Vec4 colorEnd, float life,
                           float size, float sizeEnd, BlendMode blend) {
    for (int i = 0; i < count; i++) {
        float angle = static_cast<float>(i) / static_cast<float>(count) * 2.f * static_cast<float>(M_PI);
        float spd = speed + randRange(-speedVariance, speedVariance);
        Vec2 vel = {std::cos(angle) * spd, std::sin(angle) * spd};
        float l = life + randRange(-life * 0.2f, life * 0.2f);

        spawn(pos, vel, colorStart, colorEnd, l, size, sizeEnd, 0.f, blend);
    }
}

void ParticleSystem::update(float dt) {
    activeCount_ = 0;
    for (int i = 0; i < MAX_PARTICLES; i++) {
        Particle& p = particles_[i];
        if (!p.active) continue;

        p.life -= dt;
        if (p.life <= 0.f) {
            p.active = false;
            continue;
        }

        // Physics
        p.velocity.y += p.gravity * dt;
        p.position += p.velocity * dt;
        p.rotation += p.rotationSpeed * dt;

        activeCount_++;
    }
}

void ParticleSystem::render(SpriteBatch& batch, const SpriteAtlas& atlas) {
    if (activeCount_ == 0) return;

    const SpriteFrame& white = atlas.getWhitePixel();
    const auto& tex = *atlas.getTexture();

    // Pass 1: Normal blend particles
    for (int i = 0; i < MAX_PARTICLES; i++) {
        const Particle& p = particles_[i];
        if (!p.active || p.blend != BlendMode::Normal) continue;

        float t = 1.f - (p.life / p.maxLife); // 0 = birth, 1 = death
        float sz = p.size + (p.sizeEnd - p.size) * t;
        if (sz < 0.5f) continue;

        Vec4 col = {
            p.color.x + (p.colorEnd.x - p.color.x) * t,
            p.color.y + (p.colorEnd.y - p.color.y) * t,
            p.color.z + (p.colorEnd.z - p.color.z) * t,
            p.color.w + (p.colorEnd.w - p.color.w) * t,
        };

        batch.draw(tex, p.position, {sz, sz}, white.uvRect, col, p.rotation);
    }

    // Pass 2: Additive blend particles — flush, switch blend, draw, restore
    bool hasAdditive = false;
    for (int i = 0; i < MAX_PARTICLES; i++) {
        if (particles_[i].active && particles_[i].blend == BlendMode::Additive) {
            hasAdditive = true;
            break;
        }
    }

    if (hasAdditive) {
        batch.setBlendMode(BlendMode::Additive);

        for (int i = 0; i < MAX_PARTICLES; i++) {
            const Particle& p = particles_[i];
            if (!p.active || p.blend != BlendMode::Additive) continue;

            float t = 1.f - (p.life / p.maxLife);
            float sz = p.size + (p.sizeEnd - p.size) * t;
            if (sz < 0.5f) continue;

            Vec4 col = {
                p.color.x + (p.colorEnd.x - p.color.x) * t,
                p.color.y + (p.colorEnd.y - p.color.y) * t,
                p.color.z + (p.colorEnd.z - p.color.z) * t,
                p.color.w + (p.colorEnd.w - p.color.w) * t,
            };

            batch.draw(tex, p.position, {sz, sz}, white.uvRect, col, p.rotation);
        }

        batch.setBlendMode(BlendMode::Normal);
    }
}
