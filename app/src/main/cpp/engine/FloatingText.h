#ifndef JAYGAME_FLOATINGTEXT_H
#define JAYGAME_FLOATINGTEXT_H

#include "MathTypes.h"
#include "TextRenderer.h"

class SpriteBatch;

// ─── Floating damage/text numbers ───
// Pool of floating text entries that rise and fade

struct FloatingTextEntry {
    bool active = false;
    Vec2 position;
    float velocity_y;     // upward speed
    float life;
    float maxLife;
    int value;            // damage number
    Vec4 color;
    float scale;
    bool isCrit;          // larger, with emphasis
};

class FloatingTextSystem {
public:
    static constexpr int MAX_ENTRIES = 128;

    FloatingTextSystem() {
        for (auto& e : entries_) e.active = false;
    }

    void spawn(Vec2 pos, int value, Vec4 color, float scale = 2.5f, bool crit = false) {
        for (auto& e : entries_) {
            if (e.active) continue;
            e.active = true;
            // Slight random horizontal offset to avoid stacking
            e.position = {pos.x + randOffset(), pos.y - 10.f};
            e.velocity_y = crit ? -70.f : -50.f; // rise speed
            e.life = crit ? 1.0f : 0.7f;
            e.maxLife = e.life;
            e.value = value;
            e.color = color;
            e.scale = crit ? scale * 1.5f : scale;
            e.isCrit = crit;
            return;
        }
    }

    void update(float dt) {
        for (auto& e : entries_) {
            if (!e.active) continue;
            e.life -= dt;
            if (e.life <= 0.f) {
                e.active = false;
                continue;
            }
            e.position.y += e.velocity_y * dt;
            // Decelerate
            e.velocity_y *= 0.97f;
        }
    }

    void render(SpriteBatch& batch, const TextRenderer& text) {
        for (const auto& e : entries_) {
            if (!e.active) continue;
            float t = e.life / e.maxLife; // 1.0 → 0.0
            float alpha = t; // fade out linearly
            // Scale punch: starts bigger, settles
            float punchScale = e.scale * (1.f + (1.f - t) * 0.3f * (e.isCrit ? 1.f : 0.f));

            Vec4 col = e.color;
            col.w = alpha;

            text.drawNumber(batch, e.value,
                           e.position.x, e.position.y,
                           punchScale, col, TextAlign::Center);
        }
    }

    int activeCount() const {
        int c = 0;
        for (const auto& e : entries_) if (e.active) c++;
        return c;
    }

private:
    FloatingTextEntry entries_[MAX_ENTRIES];

    static float randOffset() {
        return static_cast<float>((std::rand() % 20) - 10);
    }
};

#endif // JAYGAME_FLOATINGTEXT_H
