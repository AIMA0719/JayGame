#include "SpriteBatch.h"
#include "ParticleSystem.h" // for BlendMode enum
#include "TextureAsset.h"
#include "AndroidOut.h"

static const char *kBatchVertexShader = R"glsl(#version 300 es
layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec4 aColor;

out vec2 vTexCoord;
out vec4 vColor;

uniform mat4 uProjection;

void main() {
    vTexCoord = aTexCoord;
    vColor = aColor;
    gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
}
)glsl";

static const char *kBatchFragmentShader = R"glsl(#version 300 es
precision mediump float;

in vec2 vTexCoord;
in vec4 vColor;

uniform sampler2D uTexture;

out vec4 outColor;

void main() {
    outColor = texture(uTexture, vTexCoord) * vColor;
}
)glsl";

SpriteBatch::SpriteBatch() {
    vertices_.reserve(MAX_VERTICES);
}

SpriteBatch::~SpriteBatch() {
    shutdown();
}

static GLuint compileShader(GLenum type, const char *source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint len = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &len);
        if (len > 0) {
            std::vector<char> log(len);
            glGetShaderInfoLog(shader, len, nullptr, log.data());
            aout << "Shader compile error: " << log.data() << std::endl;
        }
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

bool SpriteBatch::createShader() {
    GLuint vs = compileShader(GL_VERTEX_SHADER, kBatchVertexShader);
    if (!vs) return false;

    GLuint fs = compileShader(GL_FRAGMENT_SHADER, kBatchFragmentShader);
    if (!fs) {
        glDeleteShader(vs);
        return false;
    }

    shaderProgram_ = glCreateProgram();
    glAttachShader(shaderProgram_, vs);
    glAttachShader(shaderProgram_, fs);
    glLinkProgram(shaderProgram_);

    glDeleteShader(vs);
    glDeleteShader(fs);

    GLint linked = 0;
    glGetProgramiv(shaderProgram_, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint len = 0;
        glGetProgramiv(shaderProgram_, GL_INFO_LOG_LENGTH, &len);
        if (len > 0) {
            std::vector<char> log(len);
            glGetProgramInfoLog(shaderProgram_, len, nullptr, log.data());
            aout << "Shader link error: " << log.data() << std::endl;
        }
        glDeleteProgram(shaderProgram_);
        shaderProgram_ = 0;
        return false;
    }

    uniformProjection_ = glGetUniformLocation(shaderProgram_, "uProjection");
    uniformTexture_ = glGetUniformLocation(shaderProgram_, "uTexture");

    return true;
}

bool SpriteBatch::init() {
    if (!createShader()) return false;

    // Create VAO
    glGenVertexArrays(1, &vao_);
    glBindVertexArray(vao_);

    // Create dynamic VBO
    glGenBuffers(1, &vbo_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferData(GL_ARRAY_BUFFER, MAX_VERTICES * sizeof(BatchVertex), nullptr, GL_DYNAMIC_DRAW);

    // Position (location 0)
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, sizeof(BatchVertex),
                          reinterpret_cast<void *>(offsetof(BatchVertex, x)));
    glEnableVertexAttribArray(0);

    // TexCoord (location 1)
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(BatchVertex),
                          reinterpret_cast<void *>(offsetof(BatchVertex, u)));
    glEnableVertexAttribArray(1);

    // Color (location 2)
    glVertexAttribPointer(2, 4, GL_FLOAT, GL_FALSE, sizeof(BatchVertex),
                          reinterpret_cast<void *>(offsetof(BatchVertex, r)));
    glEnableVertexAttribArray(2);

    // Create static IBO with pre-computed quad indices
    std::vector<uint16_t> indices(MAX_INDICES);
    for (int i = 0; i < MAX_SPRITES; i++) {
        int vi = i * 4;
        int ii = i * 6;
        indices[ii + 0] = vi + 0;
        indices[ii + 1] = vi + 1;
        indices[ii + 2] = vi + 2;
        indices[ii + 3] = vi + 0;
        indices[ii + 4] = vi + 2;
        indices[ii + 5] = vi + 3;
    }

    glGenBuffers(1, &ibo_);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo_);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size() * sizeof(uint16_t), indices.data(), GL_STATIC_DRAW);

    glBindVertexArray(0);

    aout << "SpriteBatch initialized (max " << MAX_SPRITES << " sprites)" << std::endl;
    return true;
}

void SpriteBatch::shutdown() {
    if (vao_) { glDeleteVertexArrays(1, &vao_); vao_ = 0; }
    if (vbo_) { glDeleteBuffers(1, &vbo_); vbo_ = 0; }
    if (ibo_) { glDeleteBuffers(1, &ibo_); ibo_ = 0; }
    if (shaderProgram_) { glDeleteProgram(shaderProgram_); shaderProgram_ = 0; }
}

void SpriteBatch::setProjectionMatrix(const Mat4 &proj) {
    projection_ = proj;
}

void SpriteBatch::begin() {
    drawing_ = true;
    spriteCount_ = 0;
    drawCallCount_ = 0;
    totalSpriteCount_ = 0;
    currentTextureId_ = 0;
    vertices_.clear();

    glUseProgram(shaderProgram_);
    glUniformMatrix4fv(uniformProjection_, 1, GL_FALSE, projection_.data());
    glActiveTexture(GL_TEXTURE0);
    glUniform1i(uniformTexture_, 0);

    glBindVertexArray(vao_);
}

void SpriteBatch::draw(const TextureAsset &texture,
                       float x, float y, float width, float height,
                       float srcX, float srcY, float srcW, float srcH,
                       float r, float g, float b, float a,
                       float rotation, float originX, float originY) {
    draw(texture, {x, y}, {width, height}, {srcX, srcY, srcW, srcH},
         {r, g, b, a}, rotation, {originX, originY});
}

void SpriteBatch::draw(const TextureAsset &texture,
                       const Vec2 &pos, const Vec2 &size,
                       const Rect &uvRect,
                       const Vec4 &color,
                       float rotation,
                       const Vec2 &origin) {
    if (!drawing_) return;

    GLuint texId = texture.getTextureID();

    // Flush if texture changed or batch is full
    if (currentTextureId_ != 0 && currentTextureId_ != texId) {
        flush();
    }
    if (spriteCount_ >= MAX_SPRITES) {
        flush();
    }
    currentTextureId_ = texId;

    // Calculate corners relative to origin
    float ox = -origin.x * size.x;
    float oy = -origin.y * size.y;
    float ex = ox + size.x;
    float ey = oy + size.y;

    // Apply rotation if needed
    float x0, y0, x1, y1, x2, y2, x3, y3;

    if (std::abs(rotation) < 1e-6f) {
        x0 = pos.x + ox; y0 = pos.y + oy;
        x1 = pos.x + ex; y1 = pos.y + oy;
        x2 = pos.x + ex; y2 = pos.y + ey;
        x3 = pos.x + ox; y3 = pos.y + ey;
    } else {
        float cosR = std::cos(rotation);
        float sinR = std::sin(rotation);

        x0 = pos.x + ox * cosR - oy * sinR;
        y0 = pos.y + ox * sinR + oy * cosR;
        x1 = pos.x + ex * cosR - oy * sinR;
        y1 = pos.y + ex * sinR + oy * cosR;
        x2 = pos.x + ex * cosR - ey * sinR;
        y2 = pos.y + ex * sinR + ey * cosR;
        x3 = pos.x + ox * cosR - ey * sinR;
        y3 = pos.y + ox * sinR + ey * cosR;
    }

    float u0 = uvRect.x;
    float v0 = uvRect.y;
    float u1 = uvRect.x + uvRect.w;
    float v1 = uvRect.y + uvRect.h;

    vertices_.push_back({x0, y0, u0, v0, color.x, color.y, color.z, color.w});
    vertices_.push_back({x1, y1, u1, v0, color.x, color.y, color.z, color.w});
    vertices_.push_back({x2, y2, u1, v1, color.x, color.y, color.z, color.w});
    vertices_.push_back({x3, y3, u0, v1, color.x, color.y, color.z, color.w});

    spriteCount_++;
    totalSpriteCount_++;
}

void SpriteBatch::drawRawQuad(GLuint textureId,
                               float x, float y, float width, float height,
                               float srcX, float srcY, float srcW, float srcH,
                               float r, float g, float b, float a) {
    if (!drawing_) return;

    // Flush if texture changed or batch full
    if (currentTextureId_ != 0 && currentTextureId_ != textureId) {
        flush();
    }
    if (spriteCount_ >= MAX_SPRITES) {
        flush();
    }
    currentTextureId_ = textureId;

    float x0 = x, y0 = y;
    float x1 = x + width, y1 = y;
    float x2 = x + width, y2 = y + height;
    float x3 = x, y3 = y + height;

    float u0 = srcX, v0 = srcY;
    float u1 = srcX + srcW, v1 = srcY + srcH;

    vertices_.push_back({x0, y0, u0, v0, r, g, b, a});
    vertices_.push_back({x1, y1, u1, v0, r, g, b, a});
    vertices_.push_back({x2, y2, u1, v1, r, g, b, a});
    vertices_.push_back({x3, y3, u0, v1, r, g, b, a});

    spriteCount_++;
    totalSpriteCount_++;
}

void SpriteBatch::flush() {
    if (spriteCount_ == 0) return;

    // Upload vertex data
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferSubData(GL_ARRAY_BUFFER, 0,
                    vertices_.size() * sizeof(BatchVertex), vertices_.data());

    // Bind texture
    glBindTexture(GL_TEXTURE_2D, currentTextureId_);

    // Draw
    glDrawElements(GL_TRIANGLES, spriteCount_ * INDICES_PER_SPRITE,
                   GL_UNSIGNED_SHORT, nullptr);

    drawCallCount_++;
    spriteCount_ = 0;
    vertices_.clear();
}

void SpriteBatch::setBlendMode(BlendMode mode) {
    if (!drawing_) return;
    flush(); // flush current batch before changing GL state
    if (mode == BlendMode::Additive) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    } else {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
}

void SpriteBatch::end() {
    if (!drawing_) return;

    flush();
    drawing_ = false;

    // Ensure normal blend is restored
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glBindVertexArray(0);
    glUseProgram(0);
}
