#include "DeckEditScene.h"
#include "GameEngine.h"
#include "UnitData.h"
#include "PlayerData.h"
#include "SaveSystem.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <cstdio>
#include <algorithm>

DeckEditScene::DeckEditScene(GameEngine& engine) : engine_(engine) {}

void DeckEditScene::onEnter() {
    aout << "DeckEditScene::onEnter" << std::endl;
    auto assetManager = engine_.getApp()->activity->assetManager;
    whiteTexture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");

    // Copy deck from PlayerData
    auto& pd = PlayerData::get();
    for (int i = 0; i < 5; i++) {
        deck_[i] = pd.deck[i];
    }

    setupUI();
}

void DeckEditScene::onExit() {
    ui_.clear();
    backButton_ = nullptr;
    whiteTexture_.reset();
}

void DeckEditScene::setupUI() {
    ui_.clear();

    // Back button
    auto* back = new UIButton();
    back->bounds = {20.f, 20.f, 120.f, 50.f};
    back->label = "BACK";
    back->textScale = 2.5f;
    back->normalColor = {0.5f, 0.3f, 0.3f, 0.85f};
    back->onClick = [this]() {
        // Save deck to PlayerData
        auto& pd = PlayerData::get();
        for (int i = 0; i < 5; i++) pd.deck[i] = deck_[i];
        SaveSystem::get().save();
        aout << "Back from DeckEdit - deck saved" << std::endl;
        engine_.getSceneManager().pop();
    };
    backButton_ = back;
    ui_.addWidget(back);

    // Title
    auto* title = new UILabel();
    title->bounds = {640.f, 25.f, 400.f, 30.f};
    title->text = "DECK EDIT";
    title->scale = 4.f;
    title->color = {0.7f, 0.85f, 1.f, 1.f};
    title->align = TextAlign::Center;
    ui_.addWidget(title);
}

void DeckEditScene::onUpdate(float dt) {
    // nothing dynamic
}

void DeckEditScene::onRender(float alpha, SpriteBatch& batch) {
    if (!whiteTexture_) return;

    auto& text = engine_.getTextRenderer();

    batch.begin();

    // Background
    batch.draw(*whiteTexture_, 0.f, 0.f, 1280.f, 720.f,
               0.f, 0.f, 1.f, 1.f,
               0.06f, 0.06f, 0.1f, 1.f);

    // UI widgets
    ui_.render(batch, *whiteTexture_, text);

    // Deck area label
    text.drawText(batch, "YOUR DECK", 640.f, 90.f, 3.f,
                  {0.6f, 0.7f, 0.9f, 1.f}, TextAlign::Center);

    // Deck slots
    renderDeckSlots(batch);

    // Inventory label
    text.drawText(batch, "AVAILABLE UNITS", 640.f, 280.f, 3.f,
                  {0.6f, 0.7f, 0.9f, 1.f}, TextAlign::Center);

    // Inventory grid
    renderInventory(batch);

    batch.end();
}

void DeckEditScene::renderDeckSlots(SpriteBatch& batch) {
    auto& text = engine_.getTextRenderer();
    float startX = 190.f;
    float y = 120.f;
    float slotW = 160.f;
    float slotH = 120.f;
    float gap = 20.f;

    for (int i = 0; i < 5; i++) {
        float x = startX + i * (slotW + gap);

        // Slot background
        Vec4 bg = (i == selectedDeckSlot_)
            ? Vec4{0.3f, 0.5f, 0.7f, 0.8f}
            : Vec4{0.15f, 0.15f, 0.2f, 0.8f};
        batch.draw(*whiteTexture_, x, y, slotW, slotH,
                   0.f, 0.f, 1.f, 1.f, bg.x, bg.y, bg.z, bg.w);

        // Slot border
        float bw = 2.f;
        batch.draw(*whiteTexture_, x, y, slotW, bw,
                   0.f, 0.f, 1.f, 1.f, 0.5f, 0.6f, 0.8f, 0.5f);
        batch.draw(*whiteTexture_, x, y + slotH - bw, slotW, bw,
                   0.f, 0.f, 1.f, 1.f, 0.5f, 0.6f, 0.8f, 0.5f);
        batch.draw(*whiteTexture_, x, y, bw, slotH,
                   0.f, 0.f, 1.f, 1.f, 0.5f, 0.6f, 0.8f, 0.5f);
        batch.draw(*whiteTexture_, x + slotW - bw, y, bw, slotH,
                   0.f, 0.f, 1.f, 1.f, 0.5f, 0.6f, 0.8f, 0.5f);

        // Unit in slot
        if (deck_[i] >= 0 && deck_[i] < static_cast<int>(UNIT_TABLE_SIZE)) {
            const auto& def = getUnitDef(deck_[i]);
            Vec4 unitColor;
            switch (def.element) {
                case UnitElement::Physical: unitColor = {1.0f, 0.6f, 0.3f, 0.9f}; break;
                case UnitElement::Magic:    unitColor = {0.4f, 0.6f, 1.0f, 0.9f}; break;
                case UnitElement::Support:  unitColor = {0.4f, 1.0f, 0.5f, 0.9f}; break;
            }

            float unitSize = 60.f;
            batch.draw(*whiteTexture_,
                       x + (slotW - unitSize) * 0.5f,
                       y + 10.f,
                       unitSize, unitSize,
                       0.f, 0.f, 1.f, 1.f,
                       unitColor.x, unitColor.y, unitColor.z, unitColor.w);

            // Unit name
            text.drawText(batch, def.name, x + slotW * 0.5f, y + slotH - 25.f,
                          2.f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);
        }

        // Slot number
        char slotNum[4];
        snprintf(slotNum, sizeof(slotNum), "%d", i + 1);
        text.drawText(batch, slotNum, x + 5.f, y + 5.f, 2.f,
                      {0.5f, 0.5f, 0.6f, 0.6f});
    }
}

void DeckEditScene::renderInventory(SpriteBatch& batch) {
    auto& text = engine_.getTextRenderer();
    float startX = 90.f;
    float startY = 320.f;
    float cardW = 200.f;
    float cardH = 150.f;
    float gapX = 20.f;
    float gapY = 15.f;
    int cols = 5;

    for (int i = 0; i < INVENTORY_SIZE; i++) {
        int col = i % cols;
        int row = i / cols;
        float x = startX + col * (cardW + gapX);
        float y = startY + row * (cardH + gapY) - scrollOffset_;

        if (y + cardH < 0 || y > 720.f) continue; // cull

        const auto& def = getUnitDef(i);

        // Card background
        Vec4 bg = (i == selectedInventoryIdx_)
            ? Vec4{0.3f, 0.5f, 0.3f, 0.8f}
            : Vec4{0.12f, 0.12f, 0.18f, 0.8f};
        batch.draw(*whiteTexture_, x, y, cardW, cardH,
                   0.f, 0.f, 1.f, 1.f, bg.x, bg.y, bg.z, bg.w);

        // Border
        float bw = 2.f;
        Vec4 borderCol;
        switch (def.rarity) {
            case UnitRarity::Normal:    borderCol = {0.5f, 0.5f, 0.5f, 0.5f}; break;
            case UnitRarity::Rare:      borderCol = {0.3f, 0.5f, 1.0f, 0.6f}; break;
            case UnitRarity::Epic:      borderCol = {0.7f, 0.3f, 0.9f, 0.6f}; break;
            case UnitRarity::Legendary: borderCol = {1.0f, 0.8f, 0.2f, 0.7f}; break;
        }
        batch.draw(*whiteTexture_, x, y, cardW, bw,
                   0.f, 0.f, 1.f, 1.f, borderCol.x, borderCol.y, borderCol.z, borderCol.w);
        batch.draw(*whiteTexture_, x, y + cardH - bw, cardW, bw,
                   0.f, 0.f, 1.f, 1.f, borderCol.x, borderCol.y, borderCol.z, borderCol.w);
        batch.draw(*whiteTexture_, x, y, bw, cardH,
                   0.f, 0.f, 1.f, 1.f, borderCol.x, borderCol.y, borderCol.z, borderCol.w);
        batch.draw(*whiteTexture_, x + cardW - bw, y, bw, cardH,
                   0.f, 0.f, 1.f, 1.f, borderCol.x, borderCol.y, borderCol.z, borderCol.w);

        // Unit icon
        Vec4 unitColor;
        switch (def.element) {
            case UnitElement::Physical: unitColor = {1.0f, 0.6f, 0.3f, 0.9f}; break;
            case UnitElement::Magic:    unitColor = {0.4f, 0.6f, 1.0f, 0.9f}; break;
            case UnitElement::Support:  unitColor = {0.4f, 1.0f, 0.5f, 0.9f}; break;
        }
        float iconSize = 50.f;
        batch.draw(*whiteTexture_,
                   x + (cardW - iconSize) * 0.5f, y + 15.f,
                   iconSize, iconSize,
                   0.f, 0.f, 1.f, 1.f,
                   unitColor.x, unitColor.y, unitColor.z, unitColor.w);

        // Unit name
        text.drawText(batch, def.name, x + cardW * 0.5f, y + 80.f,
                      2.5f, {1.f, 1.f, 1.f, 0.9f}, TextAlign::Center);

        // ATK stat
        char buf[32];
        snprintf(buf, sizeof(buf), "ATK:%.0f", def.baseATK);
        text.drawText(batch, buf, x + 10.f, y + cardH - 35.f,
                      1.8f, {0.8f, 0.7f, 0.6f, 0.8f});

        // Range stat
        snprintf(buf, sizeof(buf), "RNG:%.0f", def.range);
        text.drawText(batch, buf, x + cardW - 10.f, y + cardH - 35.f,
                      1.8f, {0.6f, 0.7f, 0.8f, 0.8f}, TextAlign::Right);
    }
}

void DeckEditScene::onInput(const InputEvent& event) {
    // UI widgets first
    if (ui_.onTouch(event)) return;

    if (event.action == InputAction::Tap) {
        Vec2 pos = event.worldPos;

        // Check deck slot taps
        float startX = 190.f;
        float y = 120.f;
        float slotW = 160.f;
        float slotH = 120.f;
        float gap = 20.f;

        for (int i = 0; i < 5; i++) {
            float sx = startX + i * (slotW + gap);
            Rect slotRect(sx, y, slotW, slotH);
            if (slotRect.contains(pos)) {
                if (selectedInventoryIdx_ >= 0) {
                    // Place selected inventory unit into deck slot
                    deck_[i] = selectedInventoryIdx_;
                    selectedInventoryIdx_ = -1;
                    aout << "Set deck slot " << i << " to unit " << deck_[i] << std::endl;
                } else {
                    selectedDeckSlot_ = (selectedDeckSlot_ == i) ? -1 : i;
                }
                return;
            }
        }

        // Check inventory taps
        float invStartX = 90.f;
        float invStartY = 320.f;
        float cardW = 200.f;
        float cardH = 150.f;
        float gapX = 20.f;
        float gapY = 15.f;
        int cols = 5;

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            int col = i % cols;
            int row = i / cols;
            float cx = invStartX + col * (cardW + gapX);
            float cy = invStartY + row * (cardH + gapY) - scrollOffset_;

            Rect cardRect(cx, cy, cardW, cardH);
            if (cardRect.contains(pos)) {
                if (selectedDeckSlot_ >= 0) {
                    // Place into selected deck slot
                    deck_[selectedDeckSlot_] = i;
                    selectedDeckSlot_ = -1;
                    aout << "Set deck slot to unit " << i << std::endl;
                } else {
                    selectedInventoryIdx_ = (selectedInventoryIdx_ == i) ? -1 : i;
                }
                return;
            }
        }
    }
}
