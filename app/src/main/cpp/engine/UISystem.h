#ifndef JAYGAME_UISYSTEM_H
#define JAYGAME_UISYSTEM_H

#include "MathTypes.h"
#include "InputEvent.h"
#include "TextRenderer.h"
#include <vector>
#include <string>
#include <functional>

class SpriteBatch;
class TextureAsset;

// Base UI widget
class UIWidget {
public:
    Rect bounds;
    bool visible = true;
    bool enabled = true;
    int zOrder = 0;

    virtual ~UIWidget() = default;
    virtual void render(SpriteBatch& batch, const TextureAsset& white,
                        const TextRenderer& text) const = 0;
    virtual bool onTouch(const InputEvent& event) { return false; }
};

// Simple colored button with text label
class UIButton : public UIWidget {
public:
    enum State { Normal, Pressed, Disabled };

    std::string label;
    Vec4 normalColor = {0.2f, 0.6f, 0.3f, 0.9f};
    Vec4 pressedColor = {0.15f, 0.45f, 0.22f, 0.95f};
    Vec4 disabledColor = {0.4f, 0.4f, 0.4f, 0.6f};
    Vec4 textColor = {1.f, 1.f, 1.f, 1.f};
    float textScale = 3.f;
    float borderWidth = 2.f;
    Vec4 borderColor = {1.f, 1.f, 1.f, 0.4f};

    std::function<void()> onClick;

    void render(SpriteBatch& batch, const TextureAsset& white,
                const TextRenderer& text) const override;
    bool onTouch(const InputEvent& event) override;

private:
    State state_ = Normal;
};

// Panel: colored rectangle background, can contain children
class UIPanel : public UIWidget {
public:
    Vec4 backgroundColor = {0.1f, 0.1f, 0.15f, 0.85f};
    float borderWidth = 2.f;
    Vec4 borderColor = {0.5f, 0.5f, 0.6f, 0.5f};

    std::vector<UIWidget*> children;

    void render(SpriteBatch& batch, const TextureAsset& white,
                const TextRenderer& text) const override;
    bool onTouch(const InputEvent& event) override;
};

// Text label
class UILabel : public UIWidget {
public:
    std::string text;
    Vec4 color = {1.f, 1.f, 1.f, 1.f};
    float scale = 3.f;
    TextAlign align = TextAlign::Left;

    void render(SpriteBatch& batch, const TextureAsset& white,
                const TextRenderer& textRenderer) const override;
};

// Progress bar
class UIProgressBar : public UIWidget {
public:
    float value = 0.f;   // 0.0 - 1.0
    float maxValue = 1.f;
    Vec4 bgColor = {0.2f, 0.2f, 0.2f, 0.7f};
    Vec4 fillColor = {0.3f, 0.7f, 1.0f, 0.9f};
    float borderWidth = 1.f;
    Vec4 borderColor = {0.5f, 0.5f, 0.5f, 0.5f};

    void render(SpriteBatch& batch, const TextureAsset& white,
                const TextRenderer& text) const override;
};

// Container that manages widget lifecycle and event dispatch
class UIContainer {
public:
    ~UIContainer();

    void addWidget(UIWidget* widget);
    void clear();

    void render(SpriteBatch& batch, const TextureAsset& white,
                const TextRenderer& text) const;
    bool onTouch(const InputEvent& event);

private:
    std::vector<UIWidget*> widgets_;
};

#endif // JAYGAME_UISYSTEM_H
