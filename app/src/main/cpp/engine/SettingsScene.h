#ifndef JAYGAME_SETTINGSSCENE_H
#define JAYGAME_SETTINGSSCENE_H

#include "Scene.h"
#include "UISystem.h"
#include <memory>

class GameEngine;
class TextureAsset;

class SettingsScene : public Scene {
public:
    explicit SettingsScene(GameEngine& engine);
    ~SettingsScene() override = default;

    void onEnter() override;
    void onExit() override;
    void onUpdate(float dt) override;
    void onRender(float alpha, SpriteBatch& batch) override;
    void onInput(const InputEvent& event) override;

private:
    GameEngine& engine_;

    UIContainer ui_;

    // Settings state
    bool soundEnabled_ = true;
    bool musicEnabled_ = true;

    // Toggle buttons
    UIButton* soundToggle_ = nullptr;
    UIButton* musicToggle_ = nullptr;

    std::shared_ptr<TextureAsset> whiteTexture_;

    void setupUI();
    void updateToggleColors();
};

#endif // JAYGAME_SETTINGSSCENE_H
