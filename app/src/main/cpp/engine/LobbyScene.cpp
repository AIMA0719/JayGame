#include "LobbyScene.h"
#include "GameEngine.h"
#include "BattleScene.h"
#include "DeckEditScene.h"
#include "CollectionScene.h"
#include "SettingsScene.h"
#include "UnitData.h"
#include "PlayerData.h"
#include "Currency.h"
#include "DailyLogin.h"
#include "SeasonPass.h"
#include "Achievement.h"
#include "SaveSystem.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <cstdio>

LobbyScene::LobbyScene(GameEngine& engine) : engine_(engine) {}

void LobbyScene::onEnter() {
    aout << "LobbyScene::onEnter" << std::endl;

    auto assetManager = engine_.getApp()->activity->assetManager;
    whiteTexture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");

    setupUI();
    checkDailyLogin();
}

void LobbyScene::onExit() {
    aout << "LobbyScene::onExit" << std::endl;
    ui_.clear();
    battleButton_ = nullptr;
    deckButton_ = nullptr;
    collectionButton_ = nullptr;
    settingsButton_ = nullptr;
    titleLabel_ = nullptr;
    trophyLabel_ = nullptr;
    goldLabel_ = nullptr;
    diamondLabel_ = nullptr;
    rankLabel_ = nullptr;
    seasonLabel_ = nullptr;
    whiteTexture_.reset();
}

void LobbyScene::checkDailyLogin() {
    // Use a simple date string (in real app, get from system)
    // For now use "2026-03-09" as placeholder
    std::string today = "2026-03-09";
    if (DailyLogin::canClaim(today)) {
        dailyRewardDay_ = DailyLogin::claim(today);
        if (dailyRewardDay_ >= 0) {
            showDailyPopup_ = true;
            dailyPopupTimer_ = 5.f;
            SaveSystem::get().save();
            aout << "Daily login claimed! Day " << (dailyRewardDay_ + 1) << std::endl;
        }
    }
}

void LobbyScene::setupUI() {
    ui_.clear();
    auto& pd = PlayerData::get();

    // Title
    auto* title = new UILabel();
    title->bounds = {440.f, 40.f, 400.f, 40.f};
    title->text = "JAY GAME";
    title->scale = 5.f;
    title->color = {1.f, 0.9f, 0.3f, 1.f};
    title->align = TextAlign::Center;
    titleLabel_ = title;
    ui_.addWidget(title);

    // Player info panel
    auto* infoPanel = new UIPanel();
    infoPanel->bounds = {40.f, 100.f, 500.f, 80.f};
    infoPanel->backgroundColor = {0.15f, 0.15f, 0.2f, 0.8f};
    ui_.addWidget(infoPanel);

    // Trophy label
    auto* trophyLbl = new UILabel();
    trophyLbl->bounds = {60.f, 110.f, 200.f, 20.f};
    trophyLbl->text = "TROPHY: 0";
    trophyLbl->scale = 2.5f;
    trophyLbl->color = {0.9f, 0.8f, 0.2f, 1.f};
    trophyLabel_ = trophyLbl;
    ui_.addWidget(trophyLbl);

    // Rank label
    auto* rankLbl = new UILabel();
    rankLbl->bounds = {300.f, 110.f, 200.f, 20.f};
    rankLbl->text = "BRONZE";
    rankLbl->scale = 2.5f;
    rankLbl->color = {0.8f, 0.5f, 0.3f, 1.f};
    rankLabel_ = rankLbl;
    ui_.addWidget(rankLbl);

    // Gold label
    auto* goldLbl = new UILabel();
    goldLbl->bounds = {60.f, 145.f, 200.f, 20.f};
    goldLbl->text = "GOLD: 500";
    goldLbl->scale = 2.5f;
    goldLbl->color = {1.f, 0.85f, 0.0f, 1.f};
    goldLabel_ = goldLbl;
    ui_.addWidget(goldLbl);

    // Diamond label
    auto* diamondLbl = new UILabel();
    diamondLbl->bounds = {300.f, 145.f, 200.f, 20.f};
    diamondLbl->text = "DIA: 0";
    diamondLbl->scale = 2.5f;
    diamondLbl->color = {0.5f, 0.8f, 1.f, 1.f};
    diamondLabel_ = diamondLbl;
    ui_.addWidget(diamondLbl);

    // Season pass info panel
    auto* seasonPanel = new UIPanel();
    seasonPanel->bounds = {560.f, 100.f, 350.f, 80.f};
    seasonPanel->backgroundColor = {0.12f, 0.15f, 0.2f, 0.8f};
    seasonPanel->borderColor = {0.4f, 0.7f, 0.4f, 0.5f};
    ui_.addWidget(seasonPanel);

    auto* seasonLbl = new UILabel();
    seasonLbl->bounds = {580.f, 115.f, 300.f, 20.f};
    seasonLbl->text = "SEASON PASS";
    seasonLbl->scale = 2.5f;
    seasonLbl->color = {0.5f, 0.9f, 0.5f, 1.f};
    seasonLabel_ = seasonLbl;
    ui_.addWidget(seasonLbl);

    // Deck preview panel
    auto* deckPanel = new UIPanel();
    deckPanel->bounds = {40.f, 200.f, 600.f, 120.f};
    deckPanel->backgroundColor = {0.12f, 0.12f, 0.18f, 0.8f};
    deckPanel->borderColor = {0.4f, 0.6f, 0.8f, 0.5f};
    ui_.addWidget(deckPanel);

    // Deck title
    auto* deckTitle = new UILabel();
    deckTitle->bounds = {60.f, 210.f, 100.f, 20.f};
    deckTitle->text = "DECK";
    deckTitle->scale = 2.5f;
    deckTitle->color = {0.7f, 0.8f, 1.f, 1.f};
    ui_.addWidget(deckTitle);

    // Battle button
    auto* battle = new UIButton();
    battle->bounds = {840.f, 200.f, 380.f, 120.f};
    battle->label = "BATTLE";
    battle->textScale = 5.f;
    battle->normalColor = {0.2f, 0.5f, 0.8f, 0.9f};
    battle->pressedColor = {0.15f, 0.4f, 0.6f, 0.95f};
    battle->borderColor = {0.5f, 0.8f, 1.f, 0.6f};
    battle->onClick = [this]() { onBattlePressed(); };
    battleButton_ = battle;
    ui_.addWidget(battle);

    // Deck Edit button
    auto* deck = new UIButton();
    deck->bounds = {40.f, 400.f, 280.f, 70.f};
    deck->label = "DECK EDIT";
    deck->textScale = 3.f;
    deck->normalColor = {0.3f, 0.4f, 0.6f, 0.85f};
    deck->pressedColor = {0.2f, 0.3f, 0.5f, 0.9f};
    deck->onClick = [this]() { onDeckPressed(); };
    deckButton_ = deck;
    ui_.addWidget(deck);

    // Collection button
    auto* collection = new UIButton();
    collection->bounds = {360.f, 400.f, 280.f, 70.f};
    collection->label = "COLLECTION";
    collection->textScale = 3.f;
    collection->normalColor = {0.5f, 0.3f, 0.6f, 0.85f};
    collection->pressedColor = {0.4f, 0.2f, 0.5f, 0.9f};
    collection->onClick = [this]() { onCollectionPressed(); };
    collectionButton_ = collection;
    ui_.addWidget(collection);

    // Settings button
    auto* settings = new UIButton();
    settings->bounds = {40.f, 500.f, 280.f, 70.f};
    settings->label = "SETTINGS";
    settings->textScale = 3.f;
    settings->normalColor = {0.4f, 0.4f, 0.4f, 0.85f};
    settings->pressedColor = {0.3f, 0.3f, 0.3f, 0.9f};
    settings->onClick = [this]() { onSettingsPressed(); };
    settingsButton_ = settings;
    ui_.addWidget(settings);
}

void LobbyScene::onUpdate(float dt) {
    auto& pd = PlayerData::get();

    // Update labels from PlayerData
    if (trophyLabel_) {
        char buf[32];
        snprintf(buf, sizeof(buf), "TROPHY: %d", pd.trophies);
        trophyLabel_->text = buf;
    }
    if (goldLabel_) {
        char buf[32];
        snprintf(buf, sizeof(buf), "GOLD: %d", pd.gold);
        goldLabel_->text = buf;
    }
    if (diamondLabel_) {
        char buf[32];
        snprintf(buf, sizeof(buf), "DIA: %d", pd.diamonds);
        diamondLabel_->text = buf;
    }
    if (rankLabel_) {
        TrophyRank rank = getTrophyRank(pd.trophies);
        rankLabel_->text = getRankName(rank);
        switch (rank) {
            case TrophyRank::Bronze:  rankLabel_->color = {0.8f, 0.5f, 0.3f, 1.f}; break;
            case TrophyRank::Silver:  rankLabel_->color = {0.7f, 0.7f, 0.8f, 1.f}; break;
            case TrophyRank::Gold:    rankLabel_->color = {1.f, 0.85f, 0.2f, 1.f}; break;
            case TrophyRank::Diamond: rankLabel_->color = {0.5f, 0.8f, 1.f, 1.f}; break;
            case TrophyRank::Master:  rankLabel_->color = {1.f, 0.4f, 0.4f, 1.f}; break;
        }
    }
    if (seasonLabel_) {
        char buf[48];
        int tier = SeasonPass::getCurrentTier();
        snprintf(buf, sizeof(buf), "SEASON PASS  TIER %d/%d", tier, SeasonPass::MAX_TIERS);
        seasonLabel_->text = buf;
    }

    // Daily popup timer
    if (showDailyPopup_) {
        dailyPopupTimer_ -= dt;
        if (dailyPopupTimer_ <= 0.f) showDailyPopup_ = false;
    }

    // Achievement popup
    AchievementSystem::get().updatePopup(dt);
}

void LobbyScene::onRender(float alpha, SpriteBatch& batch) {
    if (!whiteTexture_) return;
    auto& pd = PlayerData::get();

    batch.begin();

    // Dark background
    batch.draw(*whiteTexture_, 0.f, 0.f, 1280.f, 720.f,
               0.f, 0.f, 1.f, 1.f,
               0.08f, 0.08f, 0.12f, 1.f);

    // Render UI
    ui_.render(batch, *whiteTexture_, engine_.getTextRenderer());

    // Season pass progress bar
    float spBarX = 580.f, spBarY = 150.f, spBarW = 310.f, spBarH = 12.f;
    batch.draw(*whiteTexture_, spBarX, spBarY, spBarW, spBarH,
               0.f, 0.f, 1.f, 1.f, 0.2f, 0.2f, 0.2f, 0.6f);
    float spProgress = SeasonPass::getTierProgress();
    batch.draw(*whiteTexture_, spBarX, spBarY, spBarW * spProgress, spBarH,
               0.f, 0.f, 1.f, 1.f, 0.3f, 0.8f, 0.4f, 0.8f);

    // Deck preview: 5 colored squares
    for (int i = 0; i < 5; i++) {
        float sx = 60.f + i * 110.f;
        float sy = 240.f;
        float sw = 90.f;
        float sh = 60.f;

        Vec4 color = {0.5f, 0.5f, 0.5f, 0.8f};
        if (pd.deck[i] >= 0 && pd.deck[i] < static_cast<int>(UNIT_TABLE_SIZE)) {
            const auto& def = getUnitDef(pd.deck[i]);
            switch (def.family) {
                case UnitFamily::Fire:      color = {1.0f, 0.6f, 0.3f, 0.9f}; break;
                case UnitFamily::Frost:     color = {0.4f, 0.6f, 1.0f, 0.9f}; break;
                case UnitFamily::Poison:    color = {0.5f, 1.0f, 0.3f, 0.9f}; break;
                case UnitFamily::Lightning: color = {0.8f, 0.8f, 1.0f, 0.9f}; break;
                case UnitFamily::Support:   color = {0.4f, 1.0f, 0.5f, 0.9f}; break;
            }

            // Show unit level from PlayerData
            auto& text = engine_.getTextRenderer();
            char lvBuf[8];
            int unitLv = pd.units[pd.deck[i]].level;
            snprintf(lvBuf, sizeof(lvBuf), "LV%d", unitLv);
            text.drawText(batch, lvBuf, sx + sw * 0.5f, sy + sh + 5.f,
                          1.5f, {0.8f, 0.8f, 0.8f, 0.7f}, TextAlign::Center);
        }

        batch.draw(*whiteTexture_, sx, sy, sw, sh,
                   0.f, 0.f, 1.f, 1.f,
                   color.x, color.y, color.z, color.w);

        // Slot border
        batch.draw(*whiteTexture_, sx, sy, sw, 2.f,
                   0.f, 0.f, 1.f, 1.f, 1.f, 1.f, 1.f, 0.3f);
        batch.draw(*whiteTexture_, sx, sy + sh - 2.f, sw, 2.f,
                   0.f, 0.f, 1.f, 1.f, 1.f, 1.f, 1.f, 0.3f);
    }

    // Daily login popup
    if (showDailyPopup_) {
        renderDailyPopup(batch);
    }

    // Achievement popup
    if (AchievementSystem::get().hasPopup()) {
        renderAchievementPopup(batch);
    }

    batch.end();
}

void LobbyScene::renderDailyPopup(SpriteBatch& batch) {
    auto& text = engine_.getTextRenderer();

    float px = 340.f, py = 250.f, pw = 600.f, ph = 200.f;
    batch.draw(*whiteTexture_, px, py, pw, ph,
               0.f, 0.f, 1.f, 1.f,
               0.1f, 0.15f, 0.2f, 0.95f);

    // Border
    batch.draw(*whiteTexture_, px, py, pw, 3.f,
               0.f, 0.f, 1.f, 1.f, 0.3f, 0.8f, 0.4f, 0.8f);
    batch.draw(*whiteTexture_, px, py + ph - 3.f, pw, 3.f,
               0.f, 0.f, 1.f, 1.f, 0.3f, 0.8f, 0.4f, 0.8f);

    text.drawText(batch, "DAILY LOGIN REWARD!", px + pw * 0.5f, py + 20.f,
                  3.5f, {0.3f, 1.f, 0.4f, 1.f}, TextAlign::Center);

    char buf[64];
    if (dailyRewardDay_ >= 0 && dailyRewardDay_ < 7) {
        const auto& reward = DailyLogin::DAILY_REWARDS[dailyRewardDay_];
        snprintf(buf, sizeof(buf), "DAY %d", dailyRewardDay_ + 1);
        text.drawText(batch, buf, px + pw * 0.5f, py + 70.f,
                      4.f, {1.f, 0.9f, 0.3f, 1.f}, TextAlign::Center);

        snprintf(buf, sizeof(buf), "+%d GOLD  +%d DIA  +%d CARDS",
                 reward.gold, reward.diamonds, reward.cards);
        text.drawText(batch, buf, px + pw * 0.5f, py + 130.f,
                      2.5f, {0.9f, 0.8f, 0.5f, 1.f}, TextAlign::Center);
    }

    text.drawText(batch, "TAP TO CLOSE", px + pw * 0.5f, py + 170.f,
                  2.f, {0.5f, 0.5f, 0.5f, 0.7f}, TextAlign::Center);
}

void LobbyScene::renderAchievementPopup(SpriteBatch& batch) {
    auto& ach = AchievementSystem::get();
    auto& text = engine_.getTextRenderer();
    int id = ach.getPopupId();
    if (id < 0 || id >= ACHIEVEMENT_COUNT) return;

    float fade = std::min(1.f, ach.getPopupTimer());

    float px = 340.f, py = 20.f, pw = 600.f, ph = 70.f;
    batch.draw(*whiteTexture_, px, py, pw, ph,
               0.f, 0.f, 1.f, 1.f,
               0.15f, 0.1f, 0.25f, 0.9f * fade);

    batch.draw(*whiteTexture_, px, py, pw, 2.f,
               0.f, 0.f, 1.f, 1.f, 1.f, 0.8f, 0.2f, 0.8f * fade);

    text.drawText(batch, "ACHIEVEMENT UNLOCKED!", px + pw * 0.5f, py + 10.f,
                  2.f, {1.f, 0.8f, 0.2f, fade}, TextAlign::Center);
    text.drawText(batch, ACHIEVEMENTS[id].name, px + pw * 0.5f, py + 38.f,
                  2.5f, {1.f, 1.f, 1.f, fade}, TextAlign::Center);
}

void LobbyScene::onInput(const InputEvent& event) {
    // Close daily popup on tap
    if (showDailyPopup_ && event.action == InputAction::Tap) {
        showDailyPopup_ = false;
        return;
    }

    ui_.onTouch(event);
}

void LobbyScene::onBattlePressed() {
    aout << "Battle pressed!" << std::endl;
    engine_.getSceneManager().push(
        std::make_unique<BattleScene>(engine_));
}

void LobbyScene::onDeckPressed() {
    aout << "Deck Edit pressed!" << std::endl;
    engine_.getSceneManager().push(
        std::make_unique<DeckEditScene>(engine_));
}

void LobbyScene::onCollectionPressed() {
    aout << "Collection pressed!" << std::endl;
    engine_.getSceneManager().push(
        std::make_unique<CollectionScene>(engine_));
}

void LobbyScene::onSettingsPressed() {
    aout << "Settings pressed!" << std::endl;
    engine_.getSceneManager().push(
        std::make_unique<SettingsScene>(engine_));
}
