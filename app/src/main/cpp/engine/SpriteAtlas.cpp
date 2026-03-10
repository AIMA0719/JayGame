#include "SpriteAtlas.h"
#include "../TextureAsset.h"
#include "../AndroidOut.h"
#include <cmath>

// ---------- Static fallbacks ----------

const UnitSprite       SpriteAtlas::sFallbackUnit{};
const EnemySprite      SpriteAtlas::sFallbackEnemy{};
const ProjectileSprite SpriteAtlas::sFallbackProjectile{};
const SpriteFrame      SpriteAtlas::sFallbackFrame{};
const Animation        SpriteAtlas::sFallbackAnim{};

// ---------- Animation ----------

const SpriteFrame& Animation::getFrame(float time) const {
    if (frameCount <= 1) return frames[0];
    float totalDuration = frameDuration * static_cast<float>(frameCount);
    float t = std::fmod(time, totalDuration);
    if (t < 0.f) t += totalDuration;
    int idx = static_cast<int>(t / frameDuration);
    if (idx >= frameCount) idx = frameCount - 1;
    return frames[idx];
}

// ---------- cellUV ----------

Rect SpriteAtlas::cellUV(int col, int row) {
    float u = static_cast<float>(col) * CELL_SIZE / ATLAS_SIZE;
    float v = static_cast<float>(row) * CELL_SIZE / ATLAS_SIZE;
    float s = CELL_SIZE / ATLAS_SIZE;
    return {u, v, s, s};
}

// ---------- load ----------

bool SpriteAtlas::load(AAssetManager* assetManager) {
    texture_ = TextureAsset::loadAsset(assetManager, "atlas.png");
    if (!texture_) {
        aout << "SpriteAtlas: failed to load atlas.png" << std::endl;
        return false;
    }

    initUnits();
    initEnemies();
    initProjectiles();
    initTiles();
    initHud();
    initEffects();
    initWhitePixel();

    return true;
}

// ---------- Getters ----------

const std::shared_ptr<TextureAsset>& SpriteAtlas::getTexture() const {
    return texture_;
}

const UnitSprite& SpriteAtlas::getUnitSprite(int unitDefId) const {
    if (unitDefId < 0 || unitDefId >= MAX_UNITS) return sFallbackUnit;
    return units_[unitDefId];
}

const EnemySprite& SpriteAtlas::getEnemySprite(int enemyType) const {
    if (enemyType < 0 || enemyType >= MAX_ENEMIES) return sFallbackEnemy;
    return enemies_[enemyType];
}

const ProjectileSprite& SpriteAtlas::getProjectileSprite(int projType) const {
    if (projType < 0 || projType >= MAX_PROJECTILES) return sFallbackProjectile;
    return projectiles_[projType];
}

const SpriteFrame& SpriteAtlas::getTile(const std::string& name) const {
    auto it = tiles_.find(name);
    if (it == tiles_.end()) return sFallbackFrame;
    return it->second;
}

const SpriteFrame& SpriteAtlas::getHud(const std::string& name) const {
    auto it = hud_.find(name);
    if (it == hud_.end()) return sFallbackFrame;
    return it->second;
}

const Animation& SpriteAtlas::getEffect(const std::string& name) const {
    auto it = effects_.find(name);
    if (it == effects_.end()) return sFallbackAnim;
    return it->second;
}

const SpriteFrame& SpriteAtlas::getWhitePixel() const {
    return whitePixel_;
}

// ---------- Init: Units (rows 0-4) ----------
// 35 units, 4 frames each (idle0, idle1, attack0, attack1)
// 8 units per row, 4 columns per unit
// Row 0: units 0-7, Row 1: units 8-15, Row 2: units 16-23,
// Row 3: units 24-31, Row 4: units 32-34

void SpriteAtlas::initUnits() {
    for (int i = 0; i < MAX_UNITS; i++) {
        int row = i / 8;          // 0..4
        int slot = i % 8;         // 0..7
        int baseCol = slot * 4;   // each unit occupies 4 columns

        auto& u = units_[i];

        // Idle: 2 frames
        u.idle.frameCount = 2;
        u.idle.frameDuration = 0.15f;
        u.idle.frames[0] = {cellUV(baseCol + 0, row)};
        u.idle.frames[1] = {cellUV(baseCol + 1, row)};

        // Attack: 2 frames
        u.attack.frameCount = 2;
        u.attack.frameDuration = 0.15f;
        u.attack.frames[0] = {cellUV(baseCol + 2, row)};
        u.attack.frames[1] = {cellUV(baseCol + 3, row)};
    }
}

// ---------- Init: Enemies (row 5) ----------
// 6 enemies, 3 frames each (walk0, walk1, hit)
// 3 columns per enemy (shifted from row 4 to row 5 due to 35 units)

void SpriteAtlas::initEnemies() {
    const int row = 5;
    for (int i = 0; i < MAX_ENEMIES; i++) {
        int baseCol = i * 3;
        auto& e = enemies_[i];

        // Walk: 2 frames
        e.walk.frameCount = 2;
        e.walk.frameDuration = 0.15f;
        e.walk.frames[0] = {cellUV(baseCol + 0, row)};
        e.walk.frames[1] = {cellUV(baseCol + 1, row)};

        // Hit: single frame
        e.hit = {cellUV(baseCol + 2, row)};
    }
}

// ---------- Init: Projectiles (row 6) ----------
// 6 projectile types, 2 frames each (kept at row 6, no overlap)

void SpriteAtlas::initProjectiles() {
    const int row = 6;
    for (int i = 0; i < MAX_PROJECTILES; i++) {
        int baseCol = i * 2;
        auto& p = projectiles_[i];

        p.fly.frameCount = 2;
        p.fly.frameDuration = 0.15f;
        p.fly.frames[0] = {cellUV(baseCol + 0, row)};
        p.fly.frames[1] = {cellUV(baseCol + 1, row)};
    }
}

// ---------- Init: Tiles (row 7) ----------
// ~12 tiles across row 7

void SpriteAtlas::initTiles() {
    const int row = 7;
    int col = 0;
    const char* names[] = {
        "grass", "stone", "path_h", "path_v",
        "path_turn_tl", "path_turn_tr", "path_turn_bl", "path_turn_br",
        "path_cross", "water", "grid_bg", "grid_cell"
    };
    for (auto name : names) {
        tiles_[name] = {cellUV(col, row)};
        col++;
    }
}

// ---------- Init: HUD (rows 9-10) ----------
// ~17 items spread across rows 9 and 10

void SpriteAtlas::initHud() {
    int col = 0;
    int row = 9;

    const char* row9Names[] = {
        "btn_normal", "btn_pressed", "btn_disabled",
        "panel_bg", "panel_border",
        "bar_border", "bar_fill_hp", "bar_fill_mp",
        "icon_gold", "icon_wave", "icon_heart",
        "icon_attack", "icon_speed", "icon_range",
        "icon_merge", "icon_sell"
    };

    for (auto name : row9Names) {
        if (col >= 32) {
            col = 0;
            row++;
        }
        hud_[name] = {cellUV(col, row)};
        col++;
    }

    // Debuff/buff icons
    if (col >= 32) { col = 0; row++; }
    hud_["icon_buff"] = {cellUV(col, row)}; col++;
    if (col >= 32) { col = 0; row++; }
    hud_["debuff_slow"] = {cellUV(col, row)}; col++;
    if (col >= 32) { col = 0; row++; }
    hud_["debuff_dot"] = {cellUV(col, row)}; col++;
    if (col >= 32) { col = 0; row++; }
    hud_["debuff_armor"] = {cellUV(col, row)}; col++;

    // Panel used by wave delay HUD
    if (col >= 32) { col = 0; row++; }
    hud_["panel"] = {cellUV(col, row)};
}

// ---------- Init: Effects (rows 12-13) ----------
// 6 effects, variable frame counts

void SpriteAtlas::initEffects() {
    struct EffectDef {
        const char* name;
        int row;
        int startCol;
        int frameCount;
        float frameDuration;
    };

    const EffectDef defs[] = {
        {"splash",    12, 0,  4, 0.10f},
        {"slow",      12, 4,  2, 0.20f},
        {"poison",    12, 6,  3, 0.15f},
        {"chain",     12, 9,  3, 0.12f},
        {"shield",    13, 0,  2, 0.20f},
        {"buff_aura", 13, 2,  4, 0.15f},
    };

    for (auto& d : defs) {
        Animation anim;
        anim.frameCount = d.frameCount;
        anim.frameDuration = d.frameDuration;
        for (int f = 0; f < d.frameCount; f++) {
            anim.frames[f] = {cellUV(d.startCol + f, d.row)};
        }
        effects_[d.name] = anim;
    }
}

// ---------- Init: White pixel (cell 31,31) ----------

void SpriteAtlas::initWhitePixel() {
    whitePixel_ = {cellUV(31, 31)};
}
