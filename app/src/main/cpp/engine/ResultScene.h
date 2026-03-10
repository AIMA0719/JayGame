#ifndef JAYGAME_RESULTSCENE_H
#define JAYGAME_RESULTSCENE_H

#include "Scene.h"
#include "UISystem.h"
#include <memory>
#include <string>

class GameEngine;
class TextureAsset;

class ResultScene : public Scene {
public:
    struct BattleResult {
        bool victory = false;
        int waveReached = 0;
        int goldEarned = 0;
        int trophyChange = 0;
        int killCount = 0;
        int mergeCount = 0;
        bool perfectWin = false;     // no HP lost
        bool monoTypeWin = false;    // only 1 unit type used
    };

    ResultScene(GameEngine& engine, const BattleResult& result);
    ~ResultScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

private:
    GameEngine& engine_;
    BattleResult result_;

    UIContainer ui_;
    UIButton* continueButton_ = nullptr;

    std::shared_ptr<TextureAsset> whiteTexture_;
    float animTimer_ = 0.f;
    int cardsEarned_ = 0;

    void setupUI();
    void onContinuePressed();
};

#endif // JAYGAME_RESULTSCENE_H
