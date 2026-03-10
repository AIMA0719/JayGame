#include "UISystem.h"
#include "SpriteBatch.h"
#include "TextureAsset.h"
#include "TextRenderer.h"

// ---- UIButton ----

void UIButton::render(SpriteBatch& batch, const TextureAsset& white,
                       const TextRenderer& textRenderer) const {
    if (!visible) return;

    Vec4 bg;
    if (!enabled) bg = disabledColor;
    else if (state_ == Pressed) bg = pressedColor;
    else bg = normalColor;

    // Background
    batch.draw(white, bounds.x, bounds.y, bounds.w, bounds.h,
               0.f, 0.f, 1.f, 1.f, bg.x, bg.y, bg.z, bg.w);

    // Border
    if (borderWidth > 0.f) {
        float bw = borderWidth;
        batch.draw(white, bounds.x, bounds.y, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x, bounds.y + bounds.h - bw, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x, bounds.y, bw, bounds.h,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x + bounds.w - bw, bounds.y, bw, bounds.h,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
    }

    // Label text (centered)
    if (!label.empty()) {
        float textW = textRenderer.measureText(label, textScale);
        float textH = TextRenderer::GLYPH_H * textScale;
        float tx = bounds.x + (bounds.w - textW) * 0.5f;
        float ty = bounds.y + (bounds.h - textH) * 0.5f;
        textRenderer.drawText(batch, label, tx, ty, textScale, textColor);
    }
}

bool UIButton::onTouch(const InputEvent& event) {
    if (!visible || !enabled) return false;

    switch (event.action) {
        case InputAction::Down:
            if (bounds.contains(event.worldPos)) {
                state_ = Pressed;
                return true;
            }
            break;
        case InputAction::Up:
        case InputAction::Tap:
            if (state_ == Pressed) {
                state_ = Normal;
                if (bounds.contains(event.worldPos) && onClick) {
                    onClick();
                }
                return true;
            }
            break;
        case InputAction::Cancel:
            state_ = Normal;
            return false;
        default:
            break;
    }
    return false;
}

// ---- UIPanel ----

void UIPanel::render(SpriteBatch& batch, const TextureAsset& white,
                      const TextRenderer& textRenderer) const {
    if (!visible) return;

    // Background
    batch.draw(white, bounds.x, bounds.y, bounds.w, bounds.h,
               0.f, 0.f, 1.f, 1.f,
               backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);

    // Border
    if (borderWidth > 0.f) {
        float bw = borderWidth;
        batch.draw(white, bounds.x, bounds.y, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x, bounds.y + bounds.h - bw, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x, bounds.y, bw, bounds.h,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x + bounds.w - bw, bounds.y, bw, bounds.h,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
    }

    // Children
    for (const auto* child : children) {
        child->render(batch, white, textRenderer);
    }
}

bool UIPanel::onTouch(const InputEvent& event) {
    if (!visible || !enabled) return false;

    // Dispatch to children in reverse order (top first)
    for (int i = static_cast<int>(children.size()) - 1; i >= 0; i--) {
        if (children[i]->onTouch(event)) return true;
    }

    // Panel itself consumes touch if inside
    if (bounds.contains(event.worldPos)) return true;
    return false;
}

// ---- UILabel ----

void UILabel::render(SpriteBatch& batch, const TextureAsset& white,
                      const TextRenderer& textRenderer) const {
    if (!visible || text.empty()) return;

    float tx = bounds.x;
    float ty = bounds.y;

    switch (align) {
        case TextAlign::Center:
            tx = bounds.x + bounds.w * 0.5f;
            break;
        case TextAlign::Right:
            tx = bounds.x + bounds.w;
            break;
        case TextAlign::Left:
            break;
    }

    textRenderer.drawText(batch, text, tx, ty, scale, color, align);
}

// ---- UIProgressBar ----

void UIProgressBar::render(SpriteBatch& batch, const TextureAsset& white,
                            const TextRenderer& textRenderer) const {
    if (!visible) return;

    // Background
    batch.draw(white, bounds.x, bounds.y, bounds.w, bounds.h,
               0.f, 0.f, 1.f, 1.f,
               bgColor.x, bgColor.y, bgColor.z, bgColor.w);

    // Fill
    float ratio = (maxValue > 0.f) ? (value / maxValue) : 0.f;
    if (ratio < 0.f) ratio = 0.f;
    if (ratio > 1.f) ratio = 1.f;

    float fillW = bounds.w * ratio;
    batch.draw(white, bounds.x, bounds.y, fillW, bounds.h,
               0.f, 0.f, 1.f, 1.f,
               fillColor.x, fillColor.y, fillColor.z, fillColor.w);

    // Border
    if (borderWidth > 0.f) {
        float bw = borderWidth;
        batch.draw(white, bounds.x, bounds.y, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
        batch.draw(white, bounds.x, bounds.y + bounds.h - bw, bounds.w, bw,
                   0.f, 0.f, 1.f, 1.f,
                   borderColor.x, borderColor.y, borderColor.z, borderColor.w);
    }
}

// ---- UIContainer ----

UIContainer::~UIContainer() {
    clear();
}

void UIContainer::addWidget(UIWidget* widget) {
    widgets_.push_back(widget);
}

void UIContainer::clear() {
    for (auto* w : widgets_) {
        delete w;
    }
    widgets_.clear();
}

void UIContainer::render(SpriteBatch& batch, const TextureAsset& white,
                          const TextRenderer& text) const {
    for (const auto* widget : widgets_) {
        widget->render(batch, white, text);
    }
}

bool UIContainer::onTouch(const InputEvent& event) {
    for (int i = static_cast<int>(widgets_.size()) - 1; i >= 0; i--) {
        if (widgets_[i]->onTouch(event)) return true;
    }
    return false;
}
