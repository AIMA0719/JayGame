#include <jni.h>

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <game-activity/GameActivity.h>

#include "AndroidOut.h"
#include "engine/GameEngine.h"
#include "engine/BattleScene.h"

extern "C" {

void handle_cmd(android_app *pApp, int32_t cmd) {
    auto* engine = static_cast<GameEngine*>(pApp->userData);
    switch (cmd) {
        case APP_CMD_INIT_WINDOW:
            aout << "APP_CMD_INIT_WINDOW" << std::endl;
            if (engine) {
                engine->onWindowInit();
            }
            break;
        case APP_CMD_TERM_WINDOW:
            aout << "APP_CMD_TERM_WINDOW" << std::endl;
            if (engine) {
                engine->onWindowTerm();
            }
            break;
        default:
            break;
    }
}

bool motion_event_filter_func(const GameActivityMotionEvent *motionEvent) {
    auto sourceClass = motionEvent->source & AINPUT_SOURCE_CLASS_MASK;
    return (sourceClass == AINPUT_SOURCE_CLASS_POINTER ||
            sourceClass == AINPUT_SOURCE_CLASS_JOYSTICK);
}

// --- JNI Bridge: Compose → C++ ---
// Called from Kotlin BattleBridge.nativeSummon() on UI thread.
// Sets atomic flag; BattleScene::onUpdate() picks it up on game thread.
JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeSummon(JNIEnv* /*env*/, jclass /*clazz*/) {
    BattleScene::summonRequested.store(true, std::memory_order_release);
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeClickTile(JNIEnv* /*env*/, jclass /*clazz*/, jint tileIndex) {
    BattleScene::clickedTileIndex.store(tileIndex, std::memory_order_release);
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeMergeUnit(JNIEnv* /*env*/, jclass /*clazz*/, jint tileIndex) {
    BattleScene::mergeRequestUnitId.store(tileIndex, std::memory_order_release);
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeSellUnit(JNIEnv* /*env*/, jclass /*clazz*/, jint tileIndex) {
    BattleScene::sellRequestTileIndex.store(tileIndex, std::memory_order_release);
}

void android_main(struct android_app *pApp) {
    aout << "Welcome to JayGame" << std::endl;

    pApp->onAppCmd = handle_cmd;
    android_app_set_motion_event_filter(pApp, motion_event_filter_func);

    // Create engine (set as userData so handle_cmd can access it)
    GameEngine engine(pApp);
    pApp->userData = &engine;

    // Push battle scene directly (menu screens are in Compose)
    engine.getSceneManager().push(
        std::make_unique<BattleScene>(engine));

    // Run the game loop (blocks until destroy requested)
    engine.run();

    pApp->userData = nullptr;
    aout << "android_main exiting" << std::endl;
}

} // extern "C"
