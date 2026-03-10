#ifndef JAYGAME_DECKEDITSCENE_H
#define JAYGAME_DECKEDITSCENE_H

#include "Scene.h"
#include "UISystem.h"
#include <memory>

class GameEngine;
class TextureAsset;

class DeckEditScene : public Scene {
public:
    explicit DeckEditScene(GameEngine& engine);
    ~DeckEditScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

private:
    GameEngine& engine_;

    UIContainer ui_;
    UIButton* backButton_ = nullptr;

    // Current deck (5 slots)
    int deck_[5] = {0, 1, 2, 3, 4};

    // Inventory: all owned units (for now, all 10 base units)
    static constexpr int INVENTORY_SIZE = 10;

    // Scroll
    float scrollOffset_ = 0.f;

    // Selected unit for drag
    int selectedInventoryIdx_ = -1;
    int selectedDeckSlot_ = -1;

    std::shared_ptr<TextureAsset> whiteTexture_;

    void setupUI();
    void renderDeckSlots(SpriteBatch& batch);
    void renderInventory(SpriteBatch& batch);
};

#endif // JAYGAME_DECKEDITSCENE_H
