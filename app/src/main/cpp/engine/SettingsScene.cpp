#include "SettingsScene.h"
#include "GameEngine.h"
#include "PlayerData.h"
#include "SaveSystem.h"
#include "TextureAsset.h"
#include "TextRenderer.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>

SettingsScene::SettingsScene(GameEngine& engine) : engine_(engine) {}

void SettingsScene::onEnter() {
    aout << "SettingsScene::onEnter" << std::endl;
    auto assetManager = engine_.getApp()->activity->assetManager;
    whiteTexture_ = TextureAsset::loadAsset(assetManager, "android_robot.png");

    // Load settings from PlayerData
    auto& pd = PlayerData::get();
    soundEnabled_ = pd.soundEnabled;
    musicEnabled_ = pd.musicEnabled;

    setupUI();
}

void SettingsScene::onExit() {
    ui_.clear();
    soundToggle_ = nullptr;
    musicToggle_ = nullptr;
    whiteTexture_.reset();
}

void SettingsScene::setupUI() {
    ui_.clear();

    // Back button
    auto* back = new UIButton();
    back->bounds = {20.f, 20.f, 120.f, 50.f};
    back->label = "BACK";
    back->textScale = 2.5f;
    back->normalColor = {0.5f, 0.3f, 0.3f, 0.85f};
    back->onClick = [this]() {
        engine_.getSceneManager().pop();
    };
    ui_.addWidget(back);

    // Title
    auto* title = new UILabel();
    title->bounds = {640.f, 25.f, 400.f, 30.f};
    title->text = "SETTINGS";
    title->scale = 4.f;
    title->color = {0.8f, 0.8f, 0.9f, 1.f};
    title->align = TextAlign::Center;
    ui_.addWidget(title);

    // Sound label
    auto* soundLabel = new UILabel();
    soundLabel->bounds = {340.f, 180.f, 200.f, 30.f};
    soundLabel->text = "SOUND";
    soundLabel->scale = 3.5f;
    soundLabel->color = {0.8f, 0.8f, 0.9f, 1.f};
    ui_.addWidget(soundLabel);

    // Sound toggle
    auto* sndBtn = new UIButton();
    sndBtn->bounds = {700.f, 170.f, 200.f, 60.f};
    sndBtn->label = soundEnabled_ ? "ON" : "OFF";
    sndBtn->textScale = 3.f;
    sndBtn->onClick = [this]() {
        soundEnabled_ = !soundEnabled_;
        PlayerData::get().soundEnabled = soundEnabled_;
        SaveSystem::get().save();
        aout << "Sound: " << (soundEnabled_ ? "ON" : "OFF") << std::endl;
        updateToggleColors();
    };
    soundToggle_ = sndBtn;
    ui_.addWidget(sndBtn);

    // Music label
    auto* musicLabel = new UILabel();
    musicLabel->bounds = {340.f, 280.f, 200.f, 30.f};
    musicLabel->text = "MUSIC";
    musicLabel->scale = 3.5f;
    musicLabel->color = {0.8f, 0.8f, 0.9f, 1.f};
    ui_.addWidget(musicLabel);

    // Music toggle
    auto* musBtn = new UIButton();
    musBtn->bounds = {700.f, 270.f, 200.f, 60.f};
    musBtn->label = musicEnabled_ ? "ON" : "OFF";
    musBtn->textScale = 3.f;
    musBtn->onClick = [this]() {
        musicEnabled_ = !musicEnabled_;
        PlayerData::get().musicEnabled = musicEnabled_;
        SaveSystem::get().save();
        aout << "Music: " << (musicEnabled_ ? "ON" : "OFF") << std::endl;
        updateToggleColors();
    };
    musicToggle_ = musBtn;
    ui_.addWidget(musBtn);

    updateToggleColors();
}

void SettingsScene::updateToggleColors() {
    if (soundToggle_) {
        soundToggle_->label = soundEnabled_ ? "ON" : "OFF";
        soundToggle_->normalColor = soundEnabled_
            ? Vec4{0.2f, 0.6f, 0.3f, 0.9f}
            : Vec4{0.5f, 0.3f, 0.3f, 0.85f};
    }
    if (musicToggle_) {
        musicToggle_->label = musicEnabled_ ? "ON" : "OFF";
        musicToggle_->normalColor = musicEnabled_
            ? Vec4{0.2f, 0.6f, 0.3f, 0.9f}
            : Vec4{0.5f, 0.3f, 0.3f, 0.85f};
    }
}

void SettingsScene::onUpdate(float dt) {}

void SettingsScene::onRender(float alpha, SpriteBatch& batch) {
    if (!whiteTexture_) return;
    auto& text = engine_.getTextRenderer();

    batch.begin();

    // Background
    batch.draw(*whiteTexture_, 0.f, 0.f, 1280.f, 720.f,
               0.f, 0.f, 1.f, 1.f,
               0.06f, 0.06f, 0.1f, 1.f);

    // Settings panel
    batch.draw(*whiteTexture_, 280.f, 100.f, 720.f, 400.f,
               0.f, 0.f, 1.f, 1.f,
               0.1f, 0.1f, 0.15f, 0.9f);

    // Panel border
    float bw = 2.f;
    batch.draw(*whiteTexture_, 280.f, 100.f, 720.f, bw,
               0.f, 0.f, 1.f, 1.f, 0.4f, 0.4f, 0.5f, 0.5f);
    batch.draw(*whiteTexture_, 280.f, 498.f, 720.f, bw,
               0.f, 0.f, 1.f, 1.f, 0.4f, 0.4f, 0.5f, 0.5f);

    // Version info
    text.drawText(batch, "JayGame v0.4.0", 640.f, 540.f,
                  2.f, {0.4f, 0.4f, 0.5f, 0.6f}, TextAlign::Center);

    ui_.render(batch, *whiteTexture_, text);

    batch.end();
}

void SettingsScene::onInput(const InputEvent& event) {
    ui_.onTouch(event);
}
