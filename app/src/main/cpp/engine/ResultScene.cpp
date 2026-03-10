#include "ResultScene.h"
#include "GameEngine.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "PlayerData.h"
#include "Currency.h"
#include "Achievement.h"
#include "SeasonPass.h"
#include "SaveSystem.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <cmath>
#include <cstdio>
#include <cstdlib>

ResultScene::ResultScene(GameEngine& engine, const BattleResult& result)
    : engine_(engine), result_(result) {}

void ResultScene::onEnter() {
    aout << "ResultScene::onEnter - "
         << (result_.victory ? "VICTORY" : "DEFEAT")
         << " wave=" << result_.waveReached << std::endl;

    auto assetManager = engine_.getApp()->activity->assetManager;
    whiteTexture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");
    animTimer_ = 0.f;

    // Apply rewards to PlayerData
    auto& pd = PlayerData::get();

    // Gold reward
    Currency::addGold(result_.goldEarned);

    // Trophy change
    Currency::addTrophies(result_.trophyChange);

    // Card reward: 1 card per 5 waves cleared (random base unit)
    int cardReward = result_.waveReached / 5;
    for (int i = 0; i < cardReward; i++) {
        int unitId = std::rand() % 10;
        pd.units[unitId].cards++;
    }
    cardsEarned_ = cardReward;

    // Stats update
    if (result_.victory) {
        pd.totalWins++;
    } else {
        pd.totalLosses++;
    }
    if (result_.waveReached > pd.highestWave) {
        pd.highestWave = result_.waveReached;
    }
    pd.totalKills += result_.killCount;
    pd.totalMerges += result_.mergeCount;

    // Special win flags
    if (result_.victory && result_.perfectWin) {
        pd.wonWithoutDamage = true;
    }
    if (result_.victory && result_.monoTypeWin) {
        pd.wonWithSingleType = true;
    }

    // Season pass XP
    SeasonPass::addBattleXP();
    int tiersClaimed = SeasonPass::claimAvailableTiers();
    if (tiersClaimed > 0) {
        aout << "Season pass: claimed " << tiersClaimed << " tiers" << std::endl;
    }

    // Check achievements
    int unlocked = AchievementSystem::get().checkAndUnlock();
    while (unlocked >= 0) {
        unlocked = AchievementSystem::get().checkAndUnlock();
    }

    // Save
    SaveSystem::get().save();

    setupUI();
}

void ResultScene::onExit() {
    ui_.clear();
    continueButton_ = nullptr;
    whiteTexture_.reset();
}

void ResultScene::setupUI() {
    ui_.clear();

    auto* btn = new UIButton();
    btn->bounds = {490.f, 580.f, 300.f, 80.f};
    btn->label = "CONTINUE";
    btn->textScale = 4.f;
    btn->normalColor = {0.2f, 0.6f, 0.3f, 0.9f};
    btn->pressedColor = {0.15f, 0.45f, 0.22f, 0.95f};
    btn->onClick = [this]() { onContinuePressed(); };
    continueButton_ = btn;
    ui_.addWidget(btn);
}

void ResultScene::onUpdate(float dt) {
    animTimer_ += dt;
    AchievementSystem::get().updatePopup(dt);
}

void ResultScene::onRender(float alpha, SpriteBatch& batch) {
    if (!whiteTexture_) return;

    auto& text = engine_.getTextRenderer();

    batch.begin();

    // Dark background
    batch.draw(*whiteTexture_, 0.f, 0.f, 1280.f, 720.f,
               0.f, 0.f, 1.f, 1.f,
               0.05f, 0.05f, 0.1f, 1.f);

    // Result panel
    batch.draw(*whiteTexture_, 240.f, 60.f, 800.f, 500.f,
               0.f, 0.f, 1.f, 1.f,
               0.1f, 0.1f, 0.15f, 0.9f);

    // Panel border
    float bw = 3.f;
    Vec4 bc = result_.victory ? Vec4{0.3f, 0.8f, 0.3f, 0.7f}
                               : Vec4{0.8f, 0.3f, 0.3f, 0.7f};
    batch.draw(*whiteTexture_, 240.f, 60.f, 800.f, bw,
               0.f, 0.f, 1.f, 1.f, bc.x, bc.y, bc.z, bc.w);
    batch.draw(*whiteTexture_, 240.f, 557.f, 800.f, bw,
               0.f, 0.f, 1.f, 1.f, bc.x, bc.y, bc.z, bc.w);
    batch.draw(*whiteTexture_, 240.f, 60.f, bw, 500.f,
               0.f, 0.f, 1.f, 1.f, bc.x, bc.y, bc.z, bc.w);
    batch.draw(*whiteTexture_, 1037.f, 60.f, bw, 500.f,
               0.f, 0.f, 1.f, 1.f, bc.x, bc.y, bc.z, bc.w);

    // Title
    Vec4 titleColor = result_.victory ? Vec4{0.3f, 1.f, 0.4f, 1.f}
                                       : Vec4{1.f, 0.3f, 0.3f, 1.f};
    const char* titleText = result_.victory ? "VICTORY!" : "DEFEAT";
    float pulse = 0.8f + 0.2f * std::sin(animTimer_ * 3.f);
    titleColor.w = pulse;
    text.drawText(batch, titleText, 640.f, 100.f, 6.f, titleColor, TextAlign::Center);

    // Stats
    char buf[64];

    text.drawText(batch, "WAVE REACHED:", 320.f, 220.f, 3.f, {0.7f, 0.7f, 0.8f, 1.f});
    snprintf(buf, sizeof(buf), "%d", result_.waveReached);
    text.drawText(batch, buf, 720.f, 220.f, 4.f, {1.f, 1.f, 1.f, 1.f});

    text.drawText(batch, "GOLD EARNED:", 320.f, 280.f, 3.f, {0.7f, 0.7f, 0.8f, 1.f});
    snprintf(buf, sizeof(buf), "+%d", result_.goldEarned);
    text.drawText(batch, buf, 720.f, 280.f, 4.f, {1.f, 0.85f, 0.0f, 1.f});

    text.drawText(batch, "CARDS:", 320.f, 340.f, 3.f, {0.7f, 0.7f, 0.8f, 1.f});
    snprintf(buf, sizeof(buf), "+%d", cardsEarned_);
    text.drawText(batch, buf, 720.f, 340.f, 4.f, {0.5f, 0.8f, 1.f, 1.f});

    text.drawText(batch, "TROPHY:", 320.f, 400.f, 3.f, {0.7f, 0.7f, 0.8f, 1.f});
    snprintf(buf, sizeof(buf), "%+d", result_.trophyChange);
    Vec4 trophyColor = result_.trophyChange >= 0 ? Vec4{0.3f, 1.f, 0.4f, 1.f}
                                                   : Vec4{1.f, 0.4f, 0.3f, 1.f};
    text.drawText(batch, buf, 720.f, 400.f, 4.f, trophyColor);

    text.drawText(batch, "KILLS:", 320.f, 460.f, 3.f, {0.7f, 0.7f, 0.8f, 1.f});
    snprintf(buf, sizeof(buf), "%d", result_.killCount);
    text.drawText(batch, buf, 720.f, 460.f, 4.f, {1.f, 0.6f, 0.3f, 1.f});

    // UI widgets
    ui_.render(batch, *whiteTexture_, text);

    // Achievement popup
    if (AchievementSystem::get().hasPopup()) {
        auto& ach = AchievementSystem::get();
        int id = ach.getPopupId();
        if (id >= 0 && id < ACHIEVEMENT_COUNT) {
            float fade = std::min(1.f, ach.getPopupTimer());
            float px = 340.f, py = 10.f, pw = 600.f, ph = 50.f;
            batch.draw(*whiteTexture_, px, py, pw, ph,
                       0.f, 0.f, 1.f, 1.f,
                       0.15f, 0.1f, 0.25f, 0.9f * fade);
            text.drawText(batch, "ACHIEVEMENT!", px + 10.f, py + 5.f,
                          2.f, {1.f, 0.8f, 0.2f, fade});
            text.drawText(batch, ACHIEVEMENTS[id].name, px + 10.f, py + 28.f,
                          2.f, {1.f, 1.f, 1.f, fade});
        }
    }

    batch.end();
}

void ResultScene::onInput(const InputEvent& event) {
    ui_.onTouch(event);
}

void ResultScene::onContinuePressed() {
    aout << "Continue pressed - returning to lobby" << std::endl;
    engine_.getSceneManager().pop(); // pop ResultScene
    engine_.getSceneManager().pop(); // pop BattleScene
}
