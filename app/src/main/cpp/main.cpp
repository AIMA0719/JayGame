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

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeUpgradeUnit(JNIEnv* /*env*/, jclass /*clazz*/, jint tileIndex) {
    BattleScene::upgradeRequestTileIndex.store(tileIndex, std::memory_order_release);
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeSwapUnits(JNIEnv* /*env*/, jclass /*clazz*/, jint fromTile, jint toTile) {
    BattleScene::swapRequestFrom.store(fromTile, std::memory_order_release);
    BattleScene::swapRequestTo.store(toTile, std::memory_order_release);
}

JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeRelocateUnit(JNIEnv* /*env*/, jclass /*clazz*/, jint tileIndex, jfloat normX, jfloat normY) {
    // Set X, Y first, then tile as ready flag
    BattleScene::relocateRequestX.store(static_cast<int>(normX * 10000.f), std::memory_order_release);
    BattleScene::relocateRequestY.store(static_cast<int>(normY * 10000.f), std::memory_order_release);
    BattleScene::relocateRequestTile.store(tileIndex, std::memory_order_release);
}

// --- Gamble: set SP to new value after Kotlin-side random calculation ---
JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeGamble(JNIEnv* /*env*/, jclass /*clazz*/, jfloat newSp) {
    BattleScene::gambleNewSp.store(static_cast<int>(newSp * 100.f), std::memory_order_release);
}

// --- Buy Unit: spawn specific unit by ID, deduct cost ---
JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeBuyUnit(JNIEnv* /*env*/, jclass /*clazz*/, jint unitDefId, jfloat cost) {
    BattleScene::buyUnitCost.store(static_cast<int>(cost * 100.f), std::memory_order_release);
    BattleScene::buyUnitId.store(unitDefId, std::memory_order_release);
}

// --- Battle Upgrade: apply permanent buff, deduct cost ---
JNIEXPORT void JNICALL
Java_com_example_jaygame_bridge_BattleBridge_nativeApplyBattleUpgrade(JNIEnv* /*env*/, jclass /*clazz*/, jint upgradeType, jint level, jfloat cost) {
    BattleScene::upgradeTypePending.store(upgradeType, std::memory_order_release);
    BattleScene::upgradeLevelPending.store(level, std::memory_order_release);
    BattleScene::upgradeCostPending.store(static_cast<int>(cost * 100.f), std::memory_order_release);
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
