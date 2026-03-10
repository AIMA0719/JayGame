#ifndef JAYGAME_SPRITEBATCH_H
#define JAYGAME_SPRITEBATCH_H

#include <GLES3/gl3.h>
#include <vector>
#include <memory>
#include "MathTypes.h"

class TextureAsset;

// Vertex layout for batched sprite rendering
struct BatchVertex {
    float x, y;       // position
    float u, v;       // texcoord
    float r, g, b, a; // color/tint
};

class SpriteBatch {
public:
    static constexpr int MAX_SPRITES = 4096;
    static constexpr int VERTICES_PER_SPRITE = 4;
    static constexpr int INDICES_PER_SPRITE = 6;
    static constexpr int MAX_VERTICES = MAX_SPRITES * VERTICES_PER_SPRITE;
    static constexpr int MAX_INDICES = MAX_SPRITES * INDICES_PER_SPRITE;

    SpriteBatch();
    ~SpriteBatch();

    // Non-copyable
    SpriteBatch(const SpriteBatch &) = delete;
    SpriteBatch &operator=(const SpriteBatch &) = delete;

    bool init();
    void shutdown();

    void setProjectionMatrix(const Mat4 &proj);

    void begin();

    // Draw a textured quad
    void draw(const TextureAsset &texture,
              float x, float y, float width, float height,
              float srcX = 0.f, float srcY = 0.f, float srcW = 1.f, float srcH = 1.f,
              float r = 1.f, float g = 1.f, float b = 1.f, float a = 1.f,
              float rotation = 0.f, float originX = 0.5f, float originY = 0.5f);

    // Draw with full transform
    void draw(const TextureAsset &texture,
              const Vec2 &pos, const Vec2 &size,
              const Rect &uvRect,
              const Vec4 &color,
              float rotation = 0.f,
              const Vec2 &origin = {0.5f, 0.5f});

    // Draw with raw GL texture ID (for procedural textures like font atlas)
    void drawRawQuad(GLuint textureId,
                     float x, float y, float width, float height,
                     float srcX, float srcY, float srcW, float srcH,
                     float r, float g, float b, float a);

    void end();

    int getDrawCallCount() const { return drawCallCount_; }
    int getSpriteCount() const { return totalSpriteCount_; }

private:
    void flush();
    bool createShader();

    GLuint vao_ = 0;
    GLuint vbo_ = 0;
    GLuint ibo_ = 0;
    GLuint shaderProgram_ = 0;
    GLint uniformProjection_ = -1;
    GLint uniformTexture_ = -1;

    std::vector<BatchVertex> vertices_;
    int spriteCount_ = 0;
    int drawCallCount_ = 0;
    int totalSpriteCount_ = 0;
    GLuint currentTextureId_ = 0;
    bool drawing_ = false;

    Mat4 projection_;
};

#endif // JAYGAME_SPRITEBATCH_H
