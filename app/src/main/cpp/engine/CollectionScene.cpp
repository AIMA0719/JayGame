#include "CollectionScene.h"
#include "GameEngine.h"
#include "UnitData.h"
#include "PlayerData.h"
#include "Currency.h"
#include "UnitUpgrade.h"
#include "SaveSystem.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <cstdio>

CollectionScene::CollectionScene(GameEngine& engine) : engine_(engine) {}

void CollectionScene::onEnter() {
    aout << "CollectionScene::onEnter" << std::endl;
    auto assetManager = engine_.getApp()->activity->assetManager;
    whiteTexture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");
    setupUI();
}

void CollectionScene::onExit() {
    ui_.clear();
    backButton_ = nullptr;
    whiteTexture_.reset();
}

void CollectionScene::setupUI() {
    ui_.clear();

    auto* back = new UIButton();
    back->bounds = {20.f, 20.f, 120.f, 50.f};
    back->label = "BACK";
    back->textScale = 2.5f;
    back->normalColor = {0.5f, 0.3f, 0.3f, 0.85f};
    back->onClick = [this]() {
        engine_.getSceneManager().pop();
    };
    backButton_ = back;
    ui_.addWidget(back);

    auto* title = new UILabel();
    title->bounds = {640.f, 25.f, 400.f, 30.f};
    title->text = "COLLECTION";
    title->scale = 4.f;
    title->color = {0.8f, 0.7f, 1.f, 1.f};
    title->align = TextAlign::Center;
    ui_.addWidget(title);
}

void CollectionScene::onUpdate(float dt) {
    if (upgradeFlash_ > 0.f) upgradeFlash_ -= dt;
}

void CollectionScene::onRender(float alpha, SpriteBatch& batch) {
    if (!whiteTexture_) return;
    auto& text = engine_.getTextRenderer();

    batch.begin();

    // Background
    batch.draw(*whiteTexture_, 0.f, 0.f, 1280.f, 720.f,
               0.f, 0.f, 1.f, 1.f,
               0.06f, 0.05f, 0.1f, 1.f);

    ui_.render(batch, *whiteTexture_, text);

    renderUnitGrid(batch);

    if (selectedUnit_ >= 0) {
        renderDetailPanel(batch);
    }

    batch.end();
}

void CollectionScene::renderUnitGrid(SpriteBatch& batch) {
    auto& text = engine_.getTextRenderer();
    auto& pd = PlayerData::get();

    float startX = 40.f;
    float startY = 90.f;
    float cardW = 140.f;
    float cardH = 180.f;
    float gapX = 12.f;
    float gapY = 12.f;
    int cols = 5;

    int totalUnits = static_cast<int>(UNIT_TABLE_SIZE);

    for (int i = 0; i < totalUnits; i++) {
        int col = i % cols;
        int row = i / cols;
        float x = startX + col * (cardW + gapX);
        float y = startY + row * (cardH + gapY) - scrollOffset_;

        if (y + cardH < 0 || y > 720.f) continue;

        const auto& def = getUnitDef(i);
        bool isOwned = pd.units[i].owned;

        // Card background
        Vec4 bg = isOwned ? Vec4{0.12f, 0.12f, 0.18f, 0.85f}
                           : Vec4{0.08f, 0.08f, 0.1f, 0.6f};
        if (i == selectedUnit_) {
            bg = {0.2f, 0.25f, 0.35f, 0.9f};
        }
        batch.draw(*whiteTexture_, x, y, cardW, cardH,
                   0.f, 0.f, 1.f, 1.f, bg.x, bg.y, bg.z, bg.w);

        // Rarity border
        Vec4 rarityColor;
        switch (def.rarity) {
            case UnitRarity::Normal:    rarityColor = {0.5f, 0.5f, 0.5f, 0.4f}; break;
            case UnitRarity::Rare:      rarityColor = {0.3f, 0.5f, 1.0f, 0.5f}; break;
            case UnitRarity::Epic:      rarityColor = {0.7f, 0.3f, 0.9f, 0.5f}; break;
            case UnitRarity::Legendary: rarityColor = {1.0f, 0.8f, 0.2f, 0.6f}; break;
        }
        float bw = 2.f;
        batch.draw(*whiteTexture_, x, y, cardW, bw,
                   0.f, 0.f, 1.f, 1.f, rarityColor.x, rarityColor.y, rarityColor.z, rarityColor.w);
        batch.draw(*whiteTexture_, x, y + cardH - bw, cardW, bw,
                   0.f, 0.f, 1.f, 1.f, rarityColor.x, rarityColor.y, rarityColor.z, rarityColor.w);

        // Unit icon
        Vec4 unitColor;
        if (isOwned) {
            switch (def.element) {
                case UnitElement::Physical: unitColor = {1.0f, 0.6f, 0.3f, 0.9f}; break;
                case UnitElement::Magic:    unitColor = {0.4f, 0.6f, 1.0f, 0.9f}; break;
                case UnitElement::Support:  unitColor = {0.4f, 1.0f, 0.5f, 0.9f}; break;
            }
        } else {
            unitColor = {0.2f, 0.2f, 0.25f, 0.5f};
        }

        float iconSize = 60.f;
        batch.draw(*whiteTexture_,
                   x + (cardW - iconSize) * 0.5f, y + 15.f,
                   iconSize, iconSize,
                   0.f, 0.f, 1.f, 1.f,
                   unitColor.x, unitColor.y, unitColor.z, unitColor.w);

        // Name
        Vec4 nameColor = isOwned ? Vec4{1.f, 1.f, 1.f, 0.9f}
                                  : Vec4{0.4f, 0.4f, 0.4f, 0.6f};
        text.drawText(batch, isOwned ? def.name : "???",
                      x + cardW * 0.5f, y + 85.f,
                      2.f, nameColor, TextAlign::Center);

        // Level
        if (isOwned) {
            char lvBuf[16];
            snprintf(lvBuf, sizeof(lvBuf), "LV.%d", pd.units[i].level);
            text.drawText(batch, lvBuf, x + cardW * 0.5f, y + 108.f,
                          2.f, {0.5f, 0.8f, 1.f, 0.9f}, TextAlign::Center);

            // Card count
            char cardBuf[32];
            if (pd.units[i].level < MAX_UNIT_LEVEL) {
                const auto* cost = UnitUpgrade::getCost(pd.units[i].level);
                snprintf(cardBuf, sizeof(cardBuf), "%d/%d", pd.units[i].cards, cost ? cost->cards : 0);
            } else {
                snprintf(cardBuf, sizeof(cardBuf), "MAX");
            }
            text.drawText(batch, cardBuf, x + cardW * 0.5f, y + 130.f,
                          1.5f, {0.7f, 0.7f, 0.7f, 0.8f}, TextAlign::Center);
        }

        // Rarity text
        const char* rarityText = "";
        switch (def.rarity) {
            case UnitRarity::Normal:    rarityText = "NORMAL"; break;
            case UnitRarity::Rare:      rarityText = "RARE"; break;
            case UnitRarity::Epic:      rarityText = "EPIC"; break;
            case UnitRarity::Legendary: rarityText = "LEGEND"; break;
        }
        text.drawText(batch, rarityText, x + cardW * 0.5f, y + 155.f,
                      1.5f, rarityColor, TextAlign::Center);

        if (!isOwned) {
            text.drawText(batch, "LOCKED", x + cardW * 0.5f, y + 155.f,
                          2.f, {0.6f, 0.3f, 0.3f, 0.7f}, TextAlign::Center);
        }
    }
}

void CollectionScene::renderDetailPanel(SpriteBatch& batch) {
    if (selectedUnit_ < 0 || selectedUnit_ >= static_cast<int>(UNIT_TABLE_SIZE)) return;
    auto& text = engine_.getTextRenderer();
    auto& pd = PlayerData::get();
    const auto& def = getUnitDef(selectedUnit_);

    float px = 800.f, py = 90.f, pw = 440.f, ph = 580.f;
    batch.draw(*whiteTexture_, px, py, pw, ph,
               0.f, 0.f, 1.f, 1.f,
               0.1f, 0.1f, 0.15f, 0.95f);

    // Border
    float bw = 2.f;
    batch.draw(*whiteTexture_, px, py, pw, bw,
               0.f, 0.f, 1.f, 1.f, 0.5f, 0.5f, 0.7f, 0.6f);
    batch.draw(*whiteTexture_, px, py + ph - bw, pw, bw,
               0.f, 0.f, 1.f, 1.f, 0.5f, 0.5f, 0.7f, 0.6f);

    // Name
    text.drawText(batch, def.name, px + pw * 0.5f, py + 20.f,
                  4.f, {1.f, 1.f, 1.f, 1.f}, TextAlign::Center);

    // Large icon
    Vec4 unitColor;
    switch (def.element) {
        case UnitElement::Physical: unitColor = {1.0f, 0.6f, 0.3f, 0.9f}; break;
        case UnitElement::Magic:    unitColor = {0.4f, 0.6f, 1.0f, 0.9f}; break;
        case UnitElement::Support:  unitColor = {0.4f, 1.0f, 0.5f, 0.9f}; break;
    }
    float iconSize = 100.f;
    batch.draw(*whiteTexture_,
               px + (pw - iconSize) * 0.5f, py + 65.f,
               iconSize, iconSize,
               0.f, 0.f, 1.f, 1.f,
               unitColor.x, unitColor.y, unitColor.z, unitColor.w);

    // Stats
    char buf[64];
    float statY = py + 190.f;
    float lineH = 30.f;

    int unitLevel = pd.units[selectedUnit_].level;
    float atkMul = UnitUpgrade::getATKMultiplier(unitLevel);
    float spdMul = UnitUpgrade::getSpdMultiplier(unitLevel);

    snprintf(buf, sizeof(buf), "LEVEL: %d/%d", unitLevel, MAX_UNIT_LEVEL);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.5f, 0.8f, 1.f, 1.f});
    statY += lineH;

    snprintf(buf, sizeof(buf), "ATK: %.0f", def.baseATK * atkMul);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.9f, 0.7f, 0.5f, 1.f});
    statY += lineH;

    snprintf(buf, sizeof(buf), "SPD: %.2f", def.atkSpeed * spdMul);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.5f, 0.8f, 0.9f, 1.f});
    statY += lineH;

    snprintf(buf, sizeof(buf), "RANGE: %.0f", def.range);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.6f, 0.9f, 0.6f, 1.f});
    statY += lineH;

    // Element
    const char* elemText = "";
    switch (def.element) {
        case UnitElement::Physical: elemText = "PHYSICAL"; break;
        case UnitElement::Magic:    elemText = "MAGIC"; break;
        case UnitElement::Support:  elemText = "SUPPORT"; break;
    }
    snprintf(buf, sizeof(buf), "ELEM: %s", elemText);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.7f, 0.7f, 0.8f, 1.f});
    statY += lineH;

    // Ability
    const char* abilText = "NONE";
    switch (def.ability) {
        case AbilityType::Splash:  abilText = "SPLASH"; break;
        case AbilityType::Slow:    abilText = "SLOW"; break;
        case AbilityType::DoT:     abilText = "POISON"; break;
        case AbilityType::Chain:   abilText = "CHAIN"; break;
        case AbilityType::Buff:    abilText = "BUFF"; break;
        case AbilityType::Debuff:  abilText = "DEBUFF"; break;
        case AbilityType::Shield:  abilText = "SHIELD"; break;
        case AbilityType::Execute: abilText = "EXECUTE"; break;
        case AbilityType::SPBonus: abilText = "SP BONUS"; break;
        case AbilityType::Summon:  abilText = "SUMMON"; break;
        default: break;
    }
    snprintf(buf, sizeof(buf), "ABILITY: %s", abilText);
    text.drawText(batch, buf, px + 30.f, statY, 2.5f, {0.9f, 0.8f, 0.5f, 1.f});
    statY += lineH + 10.f;

    // Upgrade section
    if (pd.units[selectedUnit_].owned && unitLevel < MAX_UNIT_LEVEL) {
        const auto* cost = UnitUpgrade::getCost(unitLevel);
        if (cost) {
            // Cards needed
            snprintf(buf, sizeof(buf), "CARDS: %d/%d", pd.units[selectedUnit_].cards, cost->cards);
            bool hasCards = pd.units[selectedUnit_].cards >= cost->cards;
            text.drawText(batch, buf, px + 30.f, statY, 2.f,
                          hasCards ? Vec4{0.3f, 1.f, 0.4f, 1.f} : Vec4{1.f, 0.4f, 0.3f, 1.f});
            statY += 25.f;

            // Gold needed
            snprintf(buf, sizeof(buf), "GOLD: %d/%d", pd.gold, cost->gold);
            bool hasGold = Currency::canSpendGold(cost->gold);
            text.drawText(batch, buf, px + 30.f, statY, 2.f,
                          hasGold ? Vec4{0.3f, 1.f, 0.4f, 1.f} : Vec4{1.f, 0.4f, 0.3f, 1.f});
            statY += 30.f;

            // Upgrade button
            bool canUp = UnitUpgrade::canUpgrade(selectedUnit_);
            float btnX = px + 30.f, btnY = statY, btnW = pw - 60.f, btnH = 50.f;

            Vec4 btnColor = canUp ? Vec4{0.2f, 0.6f, 0.3f, 0.9f}
                                   : Vec4{0.4f, 0.4f, 0.4f, 0.5f};

            // Flash on success
            if (upgradeFlash_ > 0.f) {
                btnColor = {0.3f, 1.f, 0.4f, 0.9f};
            }

            batch.draw(*whiteTexture_, btnX, btnY, btnW, btnH,
                       0.f, 0.f, 1.f, 1.f,
                       btnColor.x, btnColor.y, btnColor.z, btnColor.w);

            // Border
            batch.draw(*whiteTexture_, btnX, btnY, btnW, 2.f,
                       0.f, 0.f, 1.f, 1.f, 1.f, 1.f, 1.f, 0.3f);
            batch.draw(*whiteTexture_, btnX, btnY + btnH - 2.f, btnW, 2.f,
                       0.f, 0.f, 1.f, 1.f, 1.f, 1.f, 1.f, 0.3f);

            const char* btnText = canUp ? "UPGRADE" : "NOT ENOUGH";
            text.drawText(batch, btnText, px + pw * 0.5f, btnY + 15.f,
                          3.f, {1.f, 1.f, 1.f, canUp ? 1.f : 0.5f}, TextAlign::Center);

            // Store upgrade button rect for tap detection
            upgradeButtonRect_ = {btnX, btnY, btnW, btnH};
        }
    } else if (unitLevel >= MAX_UNIT_LEVEL) {
        text.drawText(batch, "MAX LEVEL!", px + pw * 0.5f, statY,
                      3.f, {1.f, 0.85f, 0.2f, 1.f}, TextAlign::Center);
    }
}

void CollectionScene::tryUpgrade() {
    if (selectedUnit_ < 0) return;
    if (UnitUpgrade::upgrade(selectedUnit_)) {
        upgradeFlash_ = 0.5f;
        SaveSystem::get().save();
        aout << "Upgraded unit " << selectedUnit_ << " to level "
             << PlayerData::get().units[selectedUnit_].level << std::endl;
    }
}

void CollectionScene::onInput(const InputEvent& event) {
    if (ui_.onTouch(event)) return;

    if (event.action == InputAction::Tap) {
        Vec2 pos = event.worldPos;

        // Check upgrade button tap
        if (selectedUnit_ >= 0 && upgradeButtonRect_.w > 0) {
            if (upgradeButtonRect_.contains(pos)) {
                tryUpgrade();
                return;
            }
        }

        // Check unit card taps
        float startX = 40.f;
        float startY = 90.f;
        float cardW = 140.f;
        float cardH = 180.f;
        float gapX = 12.f;
        float gapY = 12.f;
        int cols = 5;
        int totalUnits = static_cast<int>(UNIT_TABLE_SIZE);

        for (int i = 0; i < totalUnits; i++) {
            int col = i % cols;
            int row = i / cols;
            float x = startX + col * (cardW + gapX);
            float y = startY + row * (cardH + gapY) - scrollOffset_;

            Rect cardRect(x, y, cardW, cardH);
            if (cardRect.contains(pos)) {
                selectedUnit_ = (selectedUnit_ == i) ? -1 : i;
                upgradeButtonRect_ = {0, 0, 0, 0};
                return;
            }
        }

        // Tap outside cards = deselect
        selectedUnit_ = -1;
        upgradeButtonRect_ = {0, 0, 0, 0};
    }
}
