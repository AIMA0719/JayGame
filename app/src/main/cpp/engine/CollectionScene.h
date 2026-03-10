#ifndef JAYGAME_COLLECTIONSCENE_H
#define JAYGAME_COLLECTIONSCENE_H

#include "Scene.h"
#include "UISystem.h"
#include "MathTypes.h"
#include <memory>

class GameEngine;
class TextureAsset;

class CollectionScene : public Scene {
public:
    explicit CollectionScene(GameEngine& engine);
    ~CollectionScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

private:
    GameEngine& engine_;

    UIContainer ui_;
    UIButton* backButton_ = nullptr;

    // Which unit is selected for detail view (-1 = none)
    int selectedUnit_ = -1;

    float scrollOffset_ = 0.f;
    std::shared_ptr<TextureAsset> whiteTexture_;

    // Upgrade feedback
    float upgradeFlash_ = 0.f;
    Rect upgradeButtonRect_ = {0, 0, 0, 0};

    void setupUI();
    void renderUnitGrid(SpriteBatch& batch);
    void renderDetailPanel(SpriteBatch& batch);
    void tryUpgrade();
};

#endif // JAYGAME_COLLECTIONSCENE_H
