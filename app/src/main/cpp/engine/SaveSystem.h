#ifndef JAYGAME_SAVESYSTEM_H
#define JAYGAME_SAVESYSTEM_H

#include "PlayerData.h"
#include "Achievement.h"
#include <string>
#include <jni.h>

struct android_app;

class SaveSystem {
public:
    static SaveSystem& get() {
        static SaveSystem instance;
        return instance;
    }

    // Must be called once with the app pointer (for JNI access)
    void init(android_app* app);

    // Save current PlayerData to SharedPreferences
    void save();

    // Load PlayerData from SharedPreferences. Returns false if no save or corrupt.
    bool load();

    // Reset to defaults
    void resetToDefaults();

private:
    SaveSystem() = default;

    android_app* app_ = nullptr;

    // Serialization
    std::string serializeToJson();
    bool deserializeFromJson(const std::string& json);

    // Simple checksum for integrity
    std::string computeChecksum(const std::string& data);
    bool verifyChecksum(const std::string& json, const std::string& checksum);

    // JNI bridge
    void jniSave(const std::string& json);
    std::string jniLoad();

    // Helper
    JNIEnv* getJNIEnv();
};

#endif // JAYGAME_SAVESYSTEM_H
