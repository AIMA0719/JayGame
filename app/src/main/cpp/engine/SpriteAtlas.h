#ifndef JAYGAME_SPRITEATLAS_H
#define JAYGAME_SPRITEATLAS_H

#include <memory>
#include <string>
#include <unordered_map>
#include "MathTypes.h"

class TextureAsset;
struct AAssetManager;

// ---------- Data structs ----------

struct SpriteFrame {
    Rect uvRect; // normalized UV (0..1)
};

struct Animation {
    SpriteFrame frames[8]; // max 8 frames
    int frameCount = 1;
    float frameDuration = 0.15f;

    const SpriteFrame& getFrame(float time) const;
};

struct UnitSprite {
    Animation idle;   // 2 frames
    Animation attack; // 2 frames
};

struct EnemySprite {
    Animation walk;   // 2 frames
    SpriteFrame hit;
};

struct ProjectileSprite {
    Animation fly; // 2 frames
};

// ---------- Atlas ----------

class SpriteAtlas {
public:
    bool load(AAssetManager* assetManager);

    const std::shared_ptr<TextureAsset>& getTexture() const;

    const UnitSprite&       getUnitSprite(int unitDefId) const;
    const EnemySprite&      getEnemySprite(int enemyType) const;
    const ProjectileSprite& getProjectileSprite(int projType) const;
    const SpriteFrame&      getTile(const std::string& name) const;
    const SpriteFrame&      getHud(const std::string& name) const;
    const Animation&        getEffect(const std::string& name) const;
    const SpriteFrame&      getWhitePixel() const;

private:
    static constexpr float ATLAS_SIZE = 2048.f;
    static constexpr float CELL_SIZE  = 64.f;

    static constexpr int MAX_UNITS       = 15;
    static constexpr int MAX_ENEMIES     = 6;
    static constexpr int MAX_PROJECTILES = 6;

    static Rect cellUV(int col, int row);

    void initUnits();
    void initEnemies();
    void initProjectiles();
    void initTiles();
    void initHud();
    void initEffects();
    void initWhitePixel();

    std::shared_ptr<TextureAsset> texture_;

    UnitSprite       units_[MAX_UNITS];
    EnemySprite      enemies_[MAX_ENEMIES];
    ProjectileSprite projectiles_[MAX_PROJECTILES];

    std::unordered_map<std::string, SpriteFrame> tiles_;
    std::unordered_map<std::string, SpriteFrame> hud_;
    std::unordered_map<std::string, Animation>   effects_;

    SpriteFrame whitePixel_;

    // Static fallbacks for safe returns
    static const UnitSprite       sFallbackUnit;
    static const EnemySprite      sFallbackEnemy;
    static const ProjectileSprite sFallbackProjectile;
    static const SpriteFrame      sFallbackFrame;
    static const Animation        sFallbackAnim;
};

#endif // JAYGAME_SPRITEATLAS_H
