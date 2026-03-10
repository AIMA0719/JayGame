#ifndef JAYGAME_SPRITE_H
#define JAYGAME_SPRITE_H

#include <memory>
#include "MathTypes.h"
#include "TextureAsset.h"

struct TextureRegion {
    std::shared_ptr<TextureAsset> texture;
    Rect uvRect = {0.f, 0.f, 1.f, 1.f}; // normalized UV coordinates

    TextureRegion() = default;
    TextureRegion(std::shared_ptr<TextureAsset> tex)
        : texture(std::move(tex)) {}
    TextureRegion(std::shared_ptr<TextureAsset> tex, const Rect &uv)
        : texture(std::move(tex)), uvRect(uv) {}
};

#endif // JAYGAME_SPRITE_H
