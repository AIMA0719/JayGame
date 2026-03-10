#ifndef JAYGAME_TEXTRENDERER_H
#define JAYGAME_TEXTRENDERER_H

#include "MathTypes.h"
#include <GLES3/gl3.h>
#include <string>
#include <memory>

class SpriteBatch;
class TextureAsset;

enum class TextAlign { Left, Center, Right };

class TextRenderer {
public:
    static constexpr float GLYPH_W = 5.f;  // base glyph width in pixels
    static constexpr float GLYPH_H = 7.f;  // base glyph height
    static constexpr float GLYPH_SPACING = 1.f;

    TextRenderer() = default;
    ~TextRenderer() = default;

    // Initialize: creates a procedural font texture from embedded 5x7 pixel font
    bool init();
    void shutdown();

    // Draw ASCII text
    void drawText(SpriteBatch& batch, const std::string& text,
                  float x, float y, float scale = 3.f,
                  Vec4 color = {1.f, 1.f, 1.f, 1.f},
                  TextAlign align = TextAlign::Left) const;

    // Draw integer number (optimized, no string allocation)
    void drawNumber(SpriteBatch& batch, int number,
                    float x, float y, float scale = 3.f,
                    Vec4 color = {1.f, 1.f, 1.f, 1.f},
                    TextAlign align = TextAlign::Left) const;

    // Draw float with 1 decimal place
    void drawFloat(SpriteBatch& batch, float number,
                   float x, float y, float scale = 3.f,
                   Vec4 color = {1.f, 1.f, 1.f, 1.f},
                   TextAlign align = TextAlign::Left) const;

    // Measure text width at given scale
    float measureText(const std::string& text, float scale = 3.f) const;
    float measureNumber(int number, float scale = 3.f) const;

    const TextureAsset* getFontTexture() const { return fontTexture_.get(); }

private:
    std::shared_ptr<TextureAsset> fontTexture_;
    GLuint fontTextureId_ = 0;

    // Font atlas: 16x6 grid of characters (96 printable ASCII: 32-127)
    static constexpr int ATLAS_COLS = 16;
    static constexpr int ATLAS_ROWS = 6;
    static constexpr int ATLAS_GLYPH_W = 5;
    static constexpr int ATLAS_GLYPH_H = 7;
    static constexpr int ATLAS_TEX_W = ATLAS_COLS * ATLAS_GLYPH_W; // 80
    static constexpr int ATLAS_TEX_H = ATLAS_ROWS * ATLAS_GLYPH_H; // 42

    void drawChar(SpriteBatch& batch, char c, float x, float y,
                  float scale, const Vec4& color) const;
    void getCharUV(char c, float& u0, float& v0, float& u1, float& v1) const;

    static void generateFontBitmap(unsigned char* pixels, int width, int height);
};

#endif // JAYGAME_TEXTRENDERER_H
