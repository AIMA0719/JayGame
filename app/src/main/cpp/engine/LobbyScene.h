#ifndef JAYGAME_LOBBYSCENE_H
#define JAYGAME_LOBBYSCENE_H

#include "Scene.h"
#include "UISystem.h"
#include <memory>

class GameEngine;
class TextureAsset;

class LobbyScene : public Scene {
public:
    explicit LobbyScene(GameEngine& engine);
    ~LobbyScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

private:
    GameEngine& engine_;

    // UI
    UIContainer ui_;
    UIButton* battleButton_ = nullptr;
    UIButton* deckButton_ = nullptr;
    UIButton* collectionButton_ = nullptr;
    UIButton* settingsButton_ = nullptr;
    UILabel* titleLabel_ = nullptr;
    UILabel* trophyLabel_ = nullptr;
    UILabel* goldLabel_ = nullptr;
    UILabel* diamondLabel_ = nullptr;
    UILabel* rankLabel_ = nullptr;
    UILabel* seasonLabel_ = nullptr;

    // Textures
    std::shared_ptr<TextureAsset> whiteTexture_;

    // Daily login popup
    bool showDailyPopup_ = false;
    int dailyRewardDay_ = -1;
    float dailyPopupTimer_ = 0.f;

    void setupUI();
    void onBattlePressed();
    void onDeckPressed();
    void onCollectionPressed();
    void onSettingsPressed();
    void checkDailyLogin();
    void renderDailyPopup(SpriteBatch& batch);
    void renderAchievementPopup(SpriteBatch& batch);
};

#endif // JAYGAME_LOBBYSCENE_H
