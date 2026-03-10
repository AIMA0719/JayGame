# Battle Visual Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace all colored rectangles in the C++ battle screen with pixel art sprites from a single 2048x2048 atlas, adding frame animation for units/enemies/projectiles.

**Architecture:** Load one sprite atlas PNG at battle start. A new SpriteAtlas class maps sprite names to UV rects in the atlas. An Animation struct drives frame cycling. All render() methods switch from flat-color quads to textured sprite regions.

**Tech Stack:** C++17, OpenGL ES 3.0, SpriteBatch (existing), TextureAsset (existing PNG loader), pixel art atlas

---

## Atlas Layout Reference

2048x2048 PNG, 64x64 cells = 32 columns × 32 rows.

| Rows | Content | Cells per item | Items |
|------|---------|---------------|-------|
| 0-3 | 15 units × 4 frames (idle×2, attack×2) | 4 cols × 4 rows per unit | 15 units |
| 4-5 | 6 enemy types × 3 frames (walk×2, hit×1) | 3 cols × 2 rows per enemy | 6 enemies |
| 6 | 6 projectile types × 2 frames | 2 cols × 1 row per projectile | 6 projectiles |
| 7-8 | Tilemap tiles (grass, stone, path, water, grid bg) | 1 col × 1 row each | ~20 tiles |
| 9-10 | HUD elements (button frames, panels, bar borders, icons) | varies | ~16 items |
| 11 | Bitmap font 8x8 (extended ASCII subset) | 1 col each | 96 chars |
| 12-15 | Effects (splash, slow cloud, poison, chain, shield, buff aura) | varies | ~12 effects |

---

### Task 1: Create SpriteAtlas Header and Source

**Files:**
- Create: `app/src/main/cpp/engine/SpriteAtlas.h`
- Create: `app/src/main/cpp/engine/SpriteAtlas.cpp`
- Modify: `app/src/main/cpp/CMakeLists.txt:10-37` (add SpriteAtlas.cpp)

**Step 1: Create SpriteAtlas.h**

```cpp
#ifndef JAYGAME_SPRITEATLAS_H
#define JAYGAME_SPRITEATLAS_H

#include "MathTypes.h"
#include "TextureAsset.h"
#include <memory>
#include <string>
#include <unordered_map>

struct SpriteFrame {
    Rect uvRect; // normalized UV (0..1)
};

struct Animation {
    SpriteFrame frames[8];
    int frameCount = 1;
    float frameDuration = 0.15f; // seconds per frame

    const SpriteFrame& getFrame(float time) const {
        if (frameCount <= 1) return frames[0];
        int idx = static_cast<int>(time / frameDuration) % frameCount;
        return frames[idx];
    }
};

// Sprite types for units (indexed by unitDefId 0-14)
struct UnitSprite {
    Animation idle;   // 2 frames
    Animation attack; // 2 frames
};

// Enemy types (indexed 0-5: normal, fast, tank, flying, boss, miniboss)
struct EnemySprite {
    Animation walk; // 2 frames
    SpriteFrame hit;
};

// Projectile types (indexed by ability: arrow, fireball, ice, poison, lightning, generic)
struct ProjectileSprite {
    Animation fly; // 2 frames
};

class SpriteAtlas {
public:
    SpriteAtlas() = default;

    // Load atlas PNG and initialize all UV mappings
    bool load(AAssetManager* assetManager);

    // Access
    const std::shared_ptr<TextureAsset>& getTexture() const { return texture_; }

    const UnitSprite& getUnitSprite(int unitDefId) const;
    const EnemySprite& getEnemySprite(int enemyType) const;
    const ProjectileSprite& getProjectileSprite(int projType) const;

    // Named tile regions
    const SpriteFrame& getTile(const std::string& name) const;

    // HUD regions
    const SpriteFrame& getHud(const std::string& name) const;

    // Effect animations
    const Animation& getEffect(const std::string& name) const;

    // White pixel region (for solid color fills like HP bars)
    const SpriteFrame& getWhitePixel() const { return whitePixel_; }

private:
    std::shared_ptr<TextureAsset> texture_;

    // Atlas is 2048x2048 with 64x64 cells
    static constexpr float ATLAS_SIZE = 2048.f;
    static constexpr float CELL_SIZE = 64.f;

    // Helper: get UV rect for cell (col, row)
    static Rect cellUV(int col, int row) {
        float u = static_cast<float>(col) * CELL_SIZE / ATLAS_SIZE;
        float v = static_cast<float>(row) * CELL_SIZE / ATLAS_SIZE;
        float s = CELL_SIZE / ATLAS_SIZE;
        return {u, v, s, s};
    }

    void initUnitSprites();
    void initEnemySprites();
    void initProjectileSprites();
    void initTiles();
    void initHud();
    void initEffects();

    UnitSprite unitSprites_[15];
    EnemySprite enemySprites_[6];
    ProjectileSprite projectileSprites_[6];

    std::unordered_map<std::string, SpriteFrame> tiles_;
    std::unordered_map<std::string, SpriteFrame> hud_;
    std::unordered_map<std::string, Animation> effects_;

    SpriteFrame whitePixel_; // 1x1 white pixel in atlas for solid fills
};

#endif // JAYGAME_SPRITEATLAS_H
```

**Step 2: Create SpriteAtlas.cpp**

```cpp
#include "SpriteAtlas.h"
#include "AndroidOut.h"

bool SpriteAtlas::load(AAssetManager* assetManager) {
    texture_ = TextureAsset::loadAsset(assetManager, "atlas.png");
    if (!texture_) {
        aout << "ERROR: Failed to load atlas.png!" << std::endl;
        return false;
    }

    initUnitSprites();
    initEnemySprites();
    initProjectileSprites();
    initTiles();
    initHud();
    initEffects();

    // White pixel: last cell bottom-right corner (31, 31)
    whitePixel_ = {cellUV(31, 31)};

    aout << "SpriteAtlas loaded successfully." << std::endl;
    return true;
}

void SpriteAtlas::initUnitSprites() {
    // Rows 0-3: 15 units, each uses 4 cols (idle×2, attack×2)
    // Layout: units 0-7 on rows 0-1 (each unit is 2 rows tall but 1 row of 4 frames)
    // Actually: 15 units × 4 frames = 60 cells. 32 cols / 4 = 8 units per row.
    // Row 0: units 0-7 (cols 0-31, 4 cols each)
    // Row 1: units 8-14 (cols 0-27)
    // Each unit occupies 4 consecutive cells in a single row.

    for (int i = 0; i < 15; i++) {
        int row = i / 8;        // which row (0 or 1)
        int colBase = (i % 8) * 4; // starting column

        auto& us = unitSprites_[i];

        // Idle: 2 frames
        us.idle.frameCount = 2;
        us.idle.frameDuration = 0.4f;
        us.idle.frames[0] = {cellUV(colBase + 0, row)};
        us.idle.frames[1] = {cellUV(colBase + 1, row)};

        // Attack: 2 frames
        us.attack.frameCount = 2;
        us.attack.frameDuration = 0.15f;
        us.attack.frames[0] = {cellUV(colBase + 2, row)};
        us.attack.frames[1] = {cellUV(colBase + 3, row)};
    }
}

void SpriteAtlas::initEnemySprites() {
    // Row 4: 6 enemies × 3 frames = 18 cells
    // Each enemy: walk×2, hit×1
    for (int i = 0; i < 6; i++) {
        int colBase = i * 3;
        auto& es = enemySprites_[i];

        es.walk.frameCount = 2;
        es.walk.frameDuration = 0.3f;
        es.walk.frames[0] = {cellUV(colBase + 0, 4)};
        es.walk.frames[1] = {cellUV(colBase + 1, 4)};

        es.hit = {cellUV(colBase + 2, 4)};
    }
}

void SpriteAtlas::initProjectileSprites() {
    // Row 6: 6 projectile types × 2 frames = 12 cells
    for (int i = 0; i < 6; i++) {
        int colBase = i * 2;
        auto& ps = projectileSprites_[i];

        ps.fly.frameCount = 2;
        ps.fly.frameDuration = 0.1f;
        ps.fly.frames[0] = {cellUV(colBase + 0, 6)};
        ps.fly.frames[1] = {cellUV(colBase + 1, 6)};
    }
}

void SpriteAtlas::initTiles() {
    // Row 7: tilemap tiles
    tiles_["grass"]    = {cellUV(0, 7)};
    tiles_["stone"]    = {cellUV(1, 7)};
    tiles_["path"]     = {cellUV(2, 7)};
    tiles_["path_h"]   = {cellUV(3, 7)};
    tiles_["path_v"]   = {cellUV(4, 7)};
    tiles_["path_corner_tl"] = {cellUV(5, 7)};
    tiles_["path_corner_tr"] = {cellUV(6, 7)};
    tiles_["path_corner_bl"] = {cellUV(7, 7)};
    tiles_["path_corner_br"] = {cellUV(8, 7)};
    tiles_["water"]    = {cellUV(9, 7)};
    tiles_["grid_bg"]  = {cellUV(10, 7)};
    tiles_["grid_cell"] = {cellUV(11, 7)};
}

void SpriteAtlas::initHud() {
    // Row 9: HUD elements
    hud_["btn_normal"]  = {cellUV(0, 9)};
    hud_["btn_pressed"] = {cellUV(1, 9)};
    hud_["btn_disabled"]= {cellUV(2, 9)};
    hud_["panel"]       = {cellUV(3, 9)};
    hud_["bar_border"]  = {cellUV(4, 9)};
    hud_["bar_fill_hp"] = {cellUV(5, 9)};
    hud_["bar_fill_sp"] = {cellUV(6, 9)};
    hud_["bar_fill_wave"]={cellUV(7, 9)};
    hud_["icon_hp"]     = {cellUV(8, 9)};
    hud_["icon_sp"]     = {cellUV(9, 9)};
    hud_["icon_wave"]   = {cellUV(10, 9)};
    hud_["icon_summon"] = {cellUV(11, 9)};
    hud_["icon_star"]   = {cellUV(12, 9)};
    // Debuff icons
    hud_["debuff_slow"] = {cellUV(0, 10)};
    hud_["debuff_dot"]  = {cellUV(1, 10)};
    hud_["debuff_armor"]= {cellUV(2, 10)};
    hud_["buff_atk"]    = {cellUV(3, 10)};
}

void SpriteAtlas::initEffects() {
    // Row 12-15: effects
    auto makeAnim = [&](int col, int row, int frames, float dur) {
        Animation a;
        a.frameCount = frames;
        a.frameDuration = dur;
        for (int i = 0; i < frames; i++) {
            a.frames[i] = {cellUV(col + i, row)};
        }
        return a;
    };

    effects_["splash"]    = makeAnim(0, 12, 4, 0.08f);
    effects_["slow"]      = makeAnim(4, 12, 3, 0.15f);
    effects_["poison"]    = makeAnim(7, 12, 3, 0.15f);
    effects_["chain"]     = makeAnim(10, 12, 4, 0.06f);
    effects_["shield"]    = makeAnim(0, 13, 3, 0.2f);
    effects_["buff_aura"] = makeAnim(3, 13, 3, 0.2f);
}

const UnitSprite& SpriteAtlas::getUnitSprite(int unitDefId) const {
    int idx = (unitDefId >= 0 && unitDefId < 15) ? unitDefId : 0;
    return unitSprites_[idx];
}

const EnemySprite& SpriteAtlas::getEnemySprite(int enemyType) const {
    int idx = (enemyType >= 0 && enemyType < 6) ? enemyType : 0;
    return enemySprites_[idx];
}

const ProjectileSprite& SpriteAtlas::getProjectileSprite(int projType) const {
    int idx = (projType >= 0 && projType < 6) ? projType : 0;
    return projectileSprites_[idx];
}

const SpriteFrame& SpriteAtlas::getTile(const std::string& name) const {
    auto it = tiles_.find(name);
    if (it != tiles_.end()) return it->second;
    static SpriteFrame fallback = {Rect{0.f, 0.f, CELL_SIZE / ATLAS_SIZE, CELL_SIZE / ATLAS_SIZE}};
    return fallback;
}

const SpriteFrame& SpriteAtlas::getHud(const std::string& name) const {
    auto it = hud_.find(name);
    if (it != hud_.end()) return it->second;
    static SpriteFrame fallback = {Rect{0.f, 0.f, CELL_SIZE / ATLAS_SIZE, CELL_SIZE / ATLAS_SIZE}};
    return fallback;
}

const Animation& SpriteAtlas::getEffect(const std::string& name) const {
    auto it = effects_.find(name);
    if (it != effects_.end()) return it->second;
    static Animation fallback;
    return fallback;
}
```

**Step 3: Add SpriteAtlas.cpp to CMakeLists.txt**

Add `engine/SpriteAtlas.cpp` after line 28 (`engine/MergeSystem.cpp`) in the `add_library` list.

**Step 4: Commit**

```bash
git add app/src/main/cpp/engine/SpriteAtlas.h app/src/main/cpp/engine/SpriteAtlas.cpp app/src/main/cpp/CMakeLists.txt
git commit -m "feat: add SpriteAtlas class for atlas UV mapping and animation"
```

---

### Task 2: Create Placeholder Atlas PNG

**Files:**
- Create: `app/src/main/assets/atlas.png`

**Step 1: Generate a 2048x2048 placeholder atlas**

Use a Python script to generate a placeholder PNG with labeled cells matching the atlas layout. Each cell is 64x64 with a distinct color per category (units=blue, enemies=red, projectiles=yellow, tiles=green, HUD=gray, font=white, effects=purple). Include cell border lines and text labels.

```python
# Script: generate_placeholder_atlas.py
from PIL import Image, ImageDraw, ImageFont

SIZE = 2048
CELL = 64
COLS = SIZE // CELL  # 32
ROWS = SIZE // CELL  # 32

img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Category colors
UNIT_COLOR = (60, 80, 180, 255)
ENEMY_COLOR = (180, 60, 60, 255)
PROJ_COLOR = (200, 180, 60, 255)
TILE_COLOR = (60, 140, 60, 255)
HUD_COLOR = (120, 120, 120, 255)
FONT_COLOR = (200, 200, 200, 255)
EFFECT_COLOR = (140, 60, 180, 255)

def fill_cell(col, row, color, label=""):
    x, y = col * CELL, row * CELL
    draw.rectangle([x+1, y+1, x+CELL-2, y+CELL-2], fill=color, outline=(255,255,255,128))
    if label:
        draw.text((x+2, y+2), label[:8], fill=(255,255,255,255))

# Units: rows 0-1
for i in range(15):
    r = i // 8
    cb = (i % 8) * 4
    for f in range(4):
        lbl = f"U{i}F{f}"
        fill_cell(cb+f, r, UNIT_COLOR, lbl)

# Enemies: row 4
for i in range(6):
    for f in range(3):
        fill_cell(i*3+f, 4, ENEMY_COLOR, f"E{i}F{f}")

# Projectiles: row 6
for i in range(6):
    for f in range(2):
        fill_cell(i*2+f, 6, PROJ_COLOR, f"P{i}F{f}")

# Tiles: row 7
tile_names = ["grass","stone","path","pathH","pathV","pTL","pTR","pBL","pBR","water","gBg","gCl"]
for i, name in enumerate(tile_names):
    fill_cell(i, 7, TILE_COLOR, name)

# HUD: rows 9-10
hud_names = ["btnN","btnP","btnD","panel","barB","barH","barS","barW","iHP","iSP","iWv","iSm","iSt"]
for i, name in enumerate(hud_names):
    fill_cell(i, 9, HUD_COLOR, name)
hud2 = ["dSlw","dDoT","dArm","bAtk"]
for i, name in enumerate(hud2):
    fill_cell(i, 10, HUD_COLOR, name)

# Font: row 11 (placeholder blocks)
for i in range(96):
    col = i % 32
    row = 11 + i // 32
    fill_cell(col, row, FONT_COLOR)

# Effects: rows 12-13
eff = [("splash",0,12,4),("slow",4,12,3),("poison",7,12,3),("chain",10,12,4),
       ("shield",0,13,3),("aura",3,13,3)]
for name, c, r, cnt in eff:
    for f in range(cnt):
        fill_cell(c+f, r, EFFECT_COLOR, f"{name[:4]}{f}")

# White pixel: cell (31,31)
draw.rectangle([31*CELL, 31*CELL, 32*CELL-1, 32*CELL-1], fill=(255,255,255,255))

img.save("atlas.png")
print("Atlas generated: 2048x2048")
```

Run: `python generate_placeholder_atlas.py`
Copy output to `app/src/main/assets/atlas.png`

**Step 2: Commit**

```bash
git add app/src/main/assets/atlas.png
git commit -m "feat: add placeholder 2048x2048 sprite atlas"
```

---

### Task 3: Add Enemy Type and Animation Timer

**Files:**
- Modify: `app/src/main/cpp/engine/Enemy.h` (add `enemyType`, `animTime_`)
- Modify: `app/src/main/cpp/engine/Enemy.cpp:8` (init enemyType), `Enemy.cpp:27` (update animTime)
- Modify: `app/src/main/cpp/engine/Projectile.h` (add `projType`)
- Modify: `app/src/main/cpp/engine/Unit.h` (add `animTime_`, `attacking_`)

**Step 1: Add fields to Enemy.h**

Add to Enemy's public members:
```cpp
int enemyType = 0;   // 0=normal, 1=fast, 2=tank, 3=flying, 4=boss, 5=miniboss
float animTime_ = 0.f;
```

**Step 2: Initialize in Enemy::init()**

In `Enemy.cpp:8`, add `enemyType` parameter to `init()`:
```cpp
void Enemy::init(float hp_, float speed_, float armor_, bool boss, int spReward_, int type = 0) {
    // ... existing init ...
    enemyType = type;
    animTime_ = 0.f;
}
```

**Step 3: Update animTime in Enemy::update()**

At the start of `Enemy::update()` (after the `if (!active ...)` guard), add:
```cpp
animTime_ += dt;
```

**Step 4: Add fields to Unit.h**

Add to Unit's public members:
```cpp
float animTime_ = 0.f;
bool attacking_ = false;
```

**Step 5: Update animTime in Unit::update()**

At the start of `Unit::update()`, add `animTime_ += dt;`. Set `attacking_ = true` when firing, reset after a short duration.

**Step 6: Add projType to Projectile.h**

Add to Projectile's public members:
```cpp
int projType = 0; // 0=arrow, 1=fireball, 2=ice, 3=poison, 4=lightning, 5=generic
```

Initialize in `Projectile::init()`: derive `projType` from source unit's ability type.

**Step 7: Commit**

```bash
git add app/src/main/cpp/engine/Enemy.h app/src/main/cpp/engine/Enemy.cpp \
  app/src/main/cpp/engine/Unit.h app/src/main/cpp/engine/Unit.cpp \
  app/src/main/cpp/engine/Projectile.h app/src/main/cpp/engine/Projectile.cpp
git commit -m "feat: add animation timer and sprite type fields to entities"
```

---

### Task 4: Integrate SpriteAtlas into BattleScene

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.h:72-76` (replace 4 textures with 1 SpriteAtlas)
- Modify: `app/src/main/cpp/engine/BattleScene.cpp:30-35` (load atlas in onEnter)
- Modify: `app/src/main/cpp/engine/BattleScene.cpp:71-78` (clean up in onExit)

**Step 1: Update BattleScene.h**

Replace:
```cpp
std::shared_ptr<TextureAsset> unitTexture_;
std::shared_ptr<TextureAsset> enemyTexture_;
std::shared_ptr<TextureAsset> projectileTexture_;
std::shared_ptr<TextureAsset> cellTexture_;
```

With:
```cpp
#include "SpriteAtlas.h"
SpriteAtlas atlas_;
```

**Step 2: Update BattleScene::onEnter() texture loading**

Replace lines 30-35:
```cpp
// Load sprite atlas
if (!atlas_.load(assetManager)) {
    aout << "ERROR: Failed to load sprite atlas!" << std::endl;
    return;
}
```

**Step 3: Update BattleScene::onExit()**

Replace texture reset lines with nothing (SpriteAtlas cleans up via shared_ptr).

**Step 4: Update all render calls that reference unitTexture_/enemyTexture_/etc.**

Replace `*unitTexture_`, `*enemyTexture_`, `*projectileTexture_`, `*cellTexture_` with `*atlas_.getTexture()` and the appropriate UV coordinates from `atlas_`.

For the null check in `onRender()`, change:
```cpp
if (!unitTexture_) return;
```
to:
```cpp
if (!atlas_.getTexture()) return;
```

**Step 5: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.h app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: replace individual textures with SpriteAtlas in BattleScene"
```

---

### Task 5: Update Unit::render() to Use Atlas Sprites

**Files:**
- Modify: `app/src/main/cpp/engine/Unit.h` (change render signature)
- Modify: `app/src/main/cpp/engine/Unit.cpp:69-121` (complete render rewrite)

**Step 1: Update render signature**

Change `render()` in Unit.h and Unit.cpp:
```cpp
void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;
```

**Step 2: Rewrite Unit::render()**

```cpp
void Unit::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = entity.transform.interpolatedPosition(alpha);
    Vec2 sz = entity.sprite.size;

    // Select animation frame
    const auto& sprite = atlas.getUnitSprite(unitDefId);
    const auto& anim = attacking_ ? sprite.attack : sprite.idle;
    const auto& frame = anim.getFrame(animTime_);

    // Tint by element (subtle, since we have actual sprites now)
    const UnitDef& def = getUnitDef(unitDefId);
    Vec4 tint = {1.f, 1.f, 1.f, 1.f};
    float brightness = 1.0f + (level - 1) * 0.03f;
    tint.x = std::fmin(brightness, 1.0f);
    tint.y = std::fmin(brightness, 1.0f);
    tint.z = std::fmin(brightness, 1.0f);

    batch.draw(tex, pos, sz, frame.uvRect, tint, 0.f, {0.5f, 0.5f});

    // Level stars using atlas star icon
    const auto& starFrame = atlas.getHud("icon_star");
    float starY = pos.y + sz.y * 0.5f + 4.f;
    float starSize = 8.f;
    float totalStarW = level * starSize + (level - 1) * 2.f;
    float starX = pos.x - totalStarW * 0.5f;
    for (int i = 0; i < level; i++) {
        batch.draw(tex,
                   {starX + i * (starSize + 2.f), starY}, {starSize, starSize},
                   starFrame.uvRect,
                   {1.f, 0.9f, 0.2f, 0.9f}, 0.f, {0.f, 0.f});
    }

    // Buff indicator using atlas buff icon
    if (unitBuffs.getAtkBonus() > 0.f) {
        const auto& buffFrame = atlas.getHud("buff_atk");
        float iconX = pos.x + sz.x * 0.5f - 12.f;
        float iconY = pos.y - sz.y * 0.5f - 2.f;
        batch.draw(tex,
                   {iconX, iconY}, {12.f, 12.f},
                   buffFrame.uvRect,
                   {1.f, 0.5f, 0.5f, 0.8f}, 0.f, {0.f, 0.f});
    }
}
```

**Step 3: Update BattleScene::onRender() unit render call**

Change:
```cpp
unit.render(alpha, batch, *unitTexture_);
```
to:
```cpp
unit.render(alpha, batch, atlas_);
```

**Step 4: Commit**

```bash
git add app/src/main/cpp/engine/Unit.h app/src/main/cpp/engine/Unit.cpp \
  app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: render units with atlas sprites and animations"
```

---

### Task 6: Update Enemy::render() to Use Atlas Sprites

**Files:**
- Modify: `app/src/main/cpp/engine/Enemy.h` (change render signature)
- Modify: `app/src/main/cpp/engine/Enemy.cpp:108-169` (complete render rewrite)

**Step 1: Update render signature**

```cpp
void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;
```

**Step 2: Rewrite Enemy::render()**

```cpp
void Enemy::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = Vec2::lerp(prevPosition, position, alpha);

    // Select sprite and animation
    const auto& sprite = atlas.getEnemySprite(enemyType);
    const auto& frame = sprite.walk.getFrame(animTime_);

    // Boss tint: slightly red; normal: white
    Vec4 tint = isBoss ? Vec4{1.f, 0.85f, 0.85f, 1.f} : Vec4{1.f, 1.f, 1.f, 1.f};

    batch.draw(tex, pos, {size, size}, frame.uvRect, tint, 0.f, {0.5f, 0.5f});

    // Health bar using atlas bar sprites
    float barWidth = size;
    float barHeight = 6.f;
    float barX = pos.x - barWidth * 0.5f;
    float barY = pos.y - size * 0.5f - 10.f;

    // Bar background
    const auto& whitePixel = atlas.getWhitePixel();
    batch.draw(tex, barX, barY, barWidth, barHeight,
               whitePixel.uvRect.x, whitePixel.uvRect.y,
               whitePixel.uvRect.w, whitePixel.uvRect.h,
               0.2f, 0.2f, 0.2f, 0.8f);

    // Bar fill
    float hpRatio = (maxHp > 0.f) ? std::max(0.f, std::min(1.f, hp / maxHp)) : 0.f;
    float fillR = 1.f - hpRatio;
    float fillG = hpRatio;
    batch.draw(tex, barX, barY, barWidth * hpRatio, barHeight,
               whitePixel.uvRect.x, whitePixel.uvRect.y,
               whitePixel.uvRect.w, whitePixel.uvRect.h,
               fillR, fillG, 0.1f, 0.9f);

    // Debuff icons using atlas sprites
    float indicatorY = barY + barHeight + 2.f;
    float indicatorX = barX;
    constexpr float INDICATOR_SIZE = 8.f;
    constexpr float INDICATOR_GAP = 2.f;

    if (buffs.hasBuffType(BuffType::Slow)) {
        const auto& icon = atlas.getHud("debuff_slow");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f,1.f,1.f,0.9f}, 0.f, {0.f,0.f});
        indicatorX += INDICATOR_SIZE + INDICATOR_GAP;
    }
    if (buffs.hasBuffType(BuffType::DoT)) {
        const auto& icon = atlas.getHud("debuff_dot");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f,1.f,1.f,0.9f}, 0.f, {0.f,0.f});
        indicatorX += INDICATOR_SIZE + INDICATOR_GAP;
    }
    if (buffs.hasBuffType(BuffType::ArmorBreak)) {
        const auto& icon = atlas.getHud("debuff_armor");
        batch.draw(tex, {indicatorX, indicatorY}, {INDICATOR_SIZE, INDICATOR_SIZE},
                   icon.uvRect, {1.f,1.f,1.f,0.9f}, 0.f, {0.f,0.f});
    }
}
```

**Step 3: Update BattleScene enemy render call**

```cpp
enemy.render(alpha, batch, atlas_);
```

**Step 4: Commit**

```bash
git add app/src/main/cpp/engine/Enemy.h app/src/main/cpp/engine/Enemy.cpp \
  app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: render enemies with atlas sprites and animations"
```

---

### Task 7: Update Projectile::render() to Use Atlas Sprites

**Files:**
- Modify: `app/src/main/cpp/engine/Projectile.h` (change render signature)
- Modify: `app/src/main/cpp/engine/Projectile.cpp:61-71` (rewrite render)

**Step 1: Update render signature**

```cpp
void render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const;
```

**Step 2: Rewrite Projectile::render()**

```cpp
void Projectile::render(float alpha, SpriteBatch& batch, const SpriteAtlas& atlas) const {
    if (!active) return;

    const auto& tex = *atlas.getTexture();
    Vec2 pos = Vec2::lerp(prevPosition_, position, alpha);

    const auto& sprite = atlas.getProjectileSprite(projType);
    // Use a simple elapsed time based on lifetime countdown
    float elapsed = 3.f - lifetime;
    const auto& frame = sprite.fly.getFrame(elapsed);

    // Calculate rotation from velocity direction
    float rotation = 0.f;
    if (velocity.lengthSq() > 1.f) {
        rotation = std::atan2(velocity.y, velocity.x);
    }

    batch.draw(tex, pos, {size, size}, frame.uvRect,
               {1.f, 1.f, 1.f, 1.f}, rotation, {0.5f, 0.5f});
}
```

**Step 3: Update BattleScene projectile render call**

```cpp
proj.render(alpha, batch, atlas_);
```

**Step 4: Set projType when creating projectiles**

In `Unit::update()`, after `proj->init(...)`, derive projType from unit ability:

```cpp
// Map ability type to projectile sprite type
switch (getUnitDef(unitDefId).ability) {
    case AbilityType::Splash:  proj->projType = 1; break; // fireball
    case AbilityType::Slow:    proj->projType = 2; break; // ice
    case AbilityType::DoT:     proj->projType = 3; break; // poison
    case AbilityType::Chain:   proj->projType = 4; break; // lightning
    default:                   proj->projType = 0; break; // arrow
}
```

**Step 5: Commit**

```bash
git add app/src/main/cpp/engine/Projectile.h app/src/main/cpp/engine/Projectile.cpp \
  app/src/main/cpp/engine/Unit.cpp app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: render projectiles with atlas sprites and rotation"
```

---

### Task 8: Update Grid Rendering and Path with Atlas Tiles

**Files:**
- Modify: `app/src/main/cpp/engine/Grid.h:119-139` (rewrite render to use atlas)
- Modify: `app/src/main/cpp/engine/BattleScene.cpp:309-377` (update grid/path rendering)

**Step 1: Update Grid::render() signature and implementation**

Change `Grid::render()`:
```cpp
void render(SpriteBatch& batch, const SpriteAtlas& atlas) const {
    const auto& tex = *atlas.getTexture();
    const auto& cellBg = atlas.getTile("grid_cell");
    const auto& whitePixel = atlas.getWhitePixel();

    // Draw cell backgrounds
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            float x = GRID_X + c * CELL_W;
            float y = GRID_Y + r * CELL_H;
            batch.draw(tex, x, y, CELL_W, CELL_H,
                       cellBg.uvRect.x, cellBg.uvRect.y,
                       cellBg.uvRect.w, cellBg.uvRect.h,
                       1.f, 1.f, 1.f, 0.3f);
        }
    }

    // Draw grid lines
    constexpr float LINE_W = 2.f;
    for (int c = 0; c <= COLS; c++) {
        float x = GRID_X + c * CELL_W - LINE_W * 0.5f;
        batch.draw(tex, x, GRID_Y, LINE_W, GRID_H,
                   whitePixel.uvRect.x, whitePixel.uvRect.y,
                   whitePixel.uvRect.w, whitePixel.uvRect.h,
                   0.5f, 0.4f, 0.3f, 0.6f);
    }
    for (int r = 0; r <= ROWS; r++) {
        float y = GRID_Y + r * CELL_H - LINE_W * 0.5f;
        batch.draw(tex, GRID_X, y, GRID_W, LINE_W,
                   whitePixel.uvRect.x, whitePixel.uvRect.y,
                   whitePixel.uvRect.w, whitePixel.uvRect.h,
                   0.5f, 0.4f, 0.3f, 0.6f);
    }
}
```

**Step 2: Update BattleScene grid render call**

```cpp
grid_.render(batch, atlas_);
```

**Step 3: Update BattleScene::renderPath() to use atlas path tiles**

Use `atlas_.getTile("path_h")` and `atlas_.getTile("path_v")` for horizontal/vertical segments, and corner tiles at waypoint turns.

**Step 4: Update BattleScene background**

At the start of `onRender()`, draw a tiled grass background behind everything:
```cpp
const auto& grassTile = atlas_.getTile("grass");
const auto& tex = *atlas_.getTexture();
for (float y = 0.f; y < 720.f; y += 64.f) {
    for (float x = 0.f; x < 1280.f; x += 64.f) {
        batch.draw(tex, x, y, 64.f, 64.f,
                   grassTile.uvRect.x, grassTile.uvRect.y,
                   grassTile.uvRect.w, grassTile.uvRect.h,
                   1.f, 1.f, 1.f, 1.f);
    }
}
```

**Step 5: Update MergeSystem::render() signature**

Change from `render(SpriteBatch&, const TextureAsset&, Grid&)` to `render(SpriteBatch&, const SpriteAtlas&, Grid&)`.

**Step 6: Commit**

```bash
git add app/src/main/cpp/engine/Grid.h app/src/main/cpp/engine/BattleScene.cpp \
  app/src/main/cpp/engine/MergeSystem.h app/src/main/cpp/engine/MergeSystem.cpp
git commit -m "feat: render grid and path with atlas tiles, add grass background"
```

---

### Task 9: Update HUD Rendering with Atlas Sprites

**Files:**
- Modify: `app/src/main/cpp/engine/BattleScene.cpp:379-480` (rewrite renderHUD)

**Step 1: Rewrite renderHUD() to use atlas panels and icons**

Replace all `*cellTexture_` references with atlas sprites. Use `atlas_.getHud("panel")` for backgrounds, `atlas_.getHud("icon_hp")` / `icon_sp` / `icon_wave` / `icon_summon` for icons, `atlas_.getHud("bar_fill_*")` for progress bars, `atlas_.getHud("btn_normal")` / `btn_pressed` for buttons.

Key changes:
- Wave info panel: Use `atlas_.getHud("panel")` as 9-patch background
- HP/SP bars: Use `atlas_.getWhitePixel()` for fills (keep dynamic coloring)
- Summon button: Use `atlas_.getHud("btn_normal")` or `btn_disabled` based on `canSummon`
- Wave delay overlay: Use panel sprite with icon

**Step 2: Commit**

```bash
git add app/src/main/cpp/engine/BattleScene.cpp
git commit -m "feat: render HUD with atlas sprites for panels and icons"
```

---

### Task 10: Update TextureAsset for Pixel Art (NEAREST filter)

**Files:**
- Modify: `app/src/main/cpp/TextureAsset.cpp:50-53` (change filter to GL_NEAREST for atlas)

**Step 1: Change texture filtering for pixel art**

In `TextureAsset::loadAsset()`, change the texture filtering for the atlas to use `GL_NEAREST` instead of `GL_LINEAR`:

```cpp
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
```

Remove mipmaps (don't call `glGenerateMipmap`) since pixel art looks best without filtering.

Alternative: Add a parameter to `loadAsset()` for filter mode, so we can keep LINEAR for non-pixel-art textures.

**Step 2: Commit**

```bash
git add app/src/main/cpp/TextureAsset.cpp app/src/main/cpp/TextureAsset.h
git commit -m "feat: use GL_NEAREST filtering for pixel art atlas"
```

---

### Task 11: Build and Verify

**Step 1: Build the project**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Fix any compilation errors**

Check all modified files compile correctly. Common issues:
- Missing `#include "SpriteAtlas.h"` in files that reference it
- Signature mismatches between header and implementation
- Forward declaration issues

**Step 3: Commit fixes if any**

```bash
git add -u
git commit -m "fix: resolve compilation errors from atlas integration"
```

---

### Task 12: Create Production Pixel Art Atlas

**Files:**
- Replace: `app/src/main/assets/atlas.png`

**Step 1: Design and create pixel art sprites**

Create proper 64x64 pixel art sprites for all categories following the atlas layout. This is the most creative and time-intensive task. Options:
1. Hand-draw using Aseprite/Piskel/LibreSprite
2. Use AI image generation with pixel art style prompts
3. Use free pixel art asset packs (itch.io, OpenGameArt) and compose into atlas

Each sprite should be:
- 64x64 pixel art, crisp edges, no anti-aliasing
- Medieval fantasy theme matching the UI (warm browns, golds, deep blues)
- Distinct silhouettes for each unit/enemy type
- Clear animation keyframes

**Step 2: Replace placeholder atlas**

Copy the production atlas to `app/src/main/assets/atlas.png`.

**Step 3: Commit**

```bash
git add app/src/main/assets/atlas.png
git commit -m "feat: add production pixel art sprite atlas"
```

---

## Summary

| Task | Description | Files Changed |
|------|-------------|--------------|
| 1 | SpriteAtlas class (UV mapping + animation) | SpriteAtlas.h/cpp, CMakeLists.txt |
| 2 | Placeholder atlas PNG | assets/atlas.png |
| 3 | Entity type/animation fields | Enemy.h/cpp, Unit.h/cpp, Projectile.h/cpp |
| 4 | BattleScene atlas integration | BattleScene.h/cpp |
| 5 | Unit render with sprites | Unit.h/cpp, BattleScene.cpp |
| 6 | Enemy render with sprites | Enemy.h/cpp, BattleScene.cpp |
| 7 | Projectile render with sprites | Projectile.h/cpp, Unit.cpp, BattleScene.cpp |
| 8 | Grid/path/background tiles | Grid.h, BattleScene.cpp, MergeSystem.h/cpp |
| 9 | HUD with atlas sprites | BattleScene.cpp |
| 10 | GL_NEAREST for pixel art | TextureAsset.cpp/h |
| 11 | Build verification | All modified files |
| 12 | Production pixel art | assets/atlas.png |
