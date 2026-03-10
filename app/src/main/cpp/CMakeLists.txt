# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.22.1)

project("jaygame")

# Creates your game shared library. The name must be the same as the
# one used for loading in your Kotlin/Java or AndroidManifest.txt files.
add_library(jaygame SHARED
    main.cpp
    AndroidOut.cpp
    TextureAsset.cpp
    Utility.cpp
    engine/GraphicsContext.cpp
    engine/GameEngine.cpp
    engine/InputSystem.cpp
    engine/SceneManager.cpp
    engine/SpriteBatch.cpp
    engine/TestScene.cpp
    engine/Unit.cpp
    engine/Wave.cpp
    engine/Enemy.cpp
    engine/Projectile.cpp
    engine/BattleScene.cpp
    engine/Buff.cpp
    engine/Ability.cpp
    engine/MergeSystem.cpp
    engine/SpriteAtlas.cpp
    engine/TextRenderer.cpp
    engine/UISystem.cpp
    engine/LobbyScene.cpp
    engine/DeckEditScene.cpp
    engine/CollectionScene.cpp
    engine/ResultScene.cpp
    engine/SettingsScene.cpp
    engine/Achievement.cpp
    engine/SaveSystem.cpp )

# Include engine headers
target_include_directories(jaygame PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Searches for a package provided by the game activity dependency
find_package(game-activity REQUIRED CONFIG)
find_package(games-frame-pacing REQUIRED CONFIG)

# Forces the linker to keep the JNI entry point for GameActivity
set(CMAKE_SHARED_LINKER_FLAGS
        "${CMAKE_SHARED_LINKER_FLAGS} -u Java_com_google_androidgamesdk_GameActivity_initializeNativeCode")

# Configure libraries CMake uses to link your target library.
target_link_libraries(jaygame
    # The game activity
    game-activity::game-activity_static

    # Swappy frame pacing
    games-frame-pacing::swappy_static

    # EGL and other dependent libraries required for drawing
    # and interacting with Android system
    EGL
    GLESv3
    jnigraphics
    android
    log)
