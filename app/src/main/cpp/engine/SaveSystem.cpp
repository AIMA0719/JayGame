#include "SaveSystem.h"
#include "AndroidOut.h"

#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <sstream>
#include <cstdint>
#include <cstring>

void SaveSystem::init(android_app* app) {
    app_ = app;
    aout << "SaveSystem initialized" << std::endl;
}

JNIEnv* SaveSystem::getJNIEnv() {
    JNIEnv* env = nullptr;
    app_->activity->vm->AttachCurrentThread(&env, nullptr);
    return env;
}

// Find class using the activity's ClassLoader (works from native threads)
static jclass findClassViaActivity(JNIEnv* env, jobject activity, const char* className) {
    jclass activityClass = env->GetObjectClass(activity);
    jmethodID getClassLoader = env->GetMethodID(activityClass, "getClassLoader",
        "()Ljava/lang/ClassLoader;");
    jobject classLoader = env->CallObjectMethod(activity, getClassLoader);
    env->DeleteLocalRef(activityClass);

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = env->GetMethodID(classLoaderClass, "loadClass",
        "(Ljava/lang/String;)Ljava/lang/Class;");
    env->DeleteLocalRef(classLoaderClass);

    jstring jClassName = env->NewStringUTF(className);
    auto cls = (jclass)env->CallObjectMethod(classLoader, loadClass, jClassName);
    env->DeleteLocalRef(jClassName);
    env->DeleteLocalRef(classLoader);

    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return nullptr;
    }
    return cls;
}

void SaveSystem::jniSave(const std::string& json) {
    JNIEnv* env = getJNIEnv();
    if (!env) {
        aout << "SaveSystem: Failed to get JNIEnv" << std::endl;
        return;
    }

    jobject activity = app_->activity->javaGameActivity;
    jclass cls = findClassViaActivity(env, activity, "com.example.jaygame.SaveBridge");
    if (!cls) {
        aout << "SaveSystem: SaveBridge class not found" << std::endl;
        return;
    }

    jmethodID mid = env->GetStaticMethodID(cls, "save",
        "(Landroid/content/Context;Ljava/lang/String;)V");
    if (!mid) {
        aout << "SaveSystem: save method not found" << std::endl;
        env->ExceptionClear();
        env->DeleteLocalRef(cls);
        return;
    }

    jstring jstr = env->NewStringUTF(json.c_str());
    env->CallStaticVoidMethod(cls, mid, activity, jstr);

    if (env->ExceptionCheck()) {
        aout << "SaveSystem: JNI save exception" << std::endl;
        env->ExceptionClear();
    }

    env->DeleteLocalRef(jstr);
    env->DeleteLocalRef(cls);
}

std::string SaveSystem::jniLoad() {
    JNIEnv* env = getJNIEnv();
    if (!env) return "";

    jobject activity = app_->activity->javaGameActivity;
    jclass cls = findClassViaActivity(env, activity, "com.example.jaygame.SaveBridge");
    if (!cls) return "";

    jmethodID mid = env->GetStaticMethodID(cls, "load",
        "(Landroid/content/Context;)Ljava/lang/String;");
    if (!mid) {
        env->ExceptionClear();
        env->DeleteLocalRef(cls);
        return "";
    }

    auto jResult = (jstring)env->CallStaticObjectMethod(cls, mid, activity);

    std::string result;
    if (jResult && !env->ExceptionCheck()) {
        const char* utf = env->GetStringUTFChars(jResult, nullptr);
        if (utf) {
            result = utf;
            env->ReleaseStringUTFChars(jResult, utf);
        }
        env->DeleteLocalRef(jResult);
    } else {
        env->ExceptionClear();
    }

    env->DeleteLocalRef(cls);
    return result;
}

// Simple FNV-1a hash for checksum
std::string SaveSystem::computeChecksum(const std::string& data) {
    uint32_t hash = 2166136261u;
    for (char c : data) {
        hash ^= static_cast<uint32_t>(c);
        hash *= 16777619u;
    }
    char buf[16];
    snprintf(buf, sizeof(buf), "%08x", hash);
    return buf;
}

bool SaveSystem::verifyChecksum(const std::string& json, const std::string& checksum) {
    // Find the data portion (everything before "checksum" key)
    auto pos = json.find("\"checksum\"");
    if (pos == std::string::npos) return false;

    std::string dataOnly = json.substr(0, pos);
    return computeChecksum(dataOnly) == checksum;
}

// ---- Simple JSON builder (no external dependency) ----

static void jsonEscapeAppend(std::ostringstream& ss, const std::string& s) {
    ss << '"';
    for (char c : s) {
        if (c == '"') ss << "\\\"";
        else if (c == '\\') ss << "\\\\";
        else ss << c;
    }
    ss << '"';
}

std::string SaveSystem::serializeToJson() {
    auto& pd = PlayerData::get();
    auto& ach = AchievementSystem::get();

    std::ostringstream ss;
    ss << "{\n";
    ss << "\"version\":" << pd.saveVersion << ",\n";

    // Player
    ss << "\"gold\":" << pd.gold << ",\n";
    ss << "\"diamonds\":" << pd.diamonds << ",\n";
    ss << "\"gas\":" << pd.gas << ",\n";
    ss << "\"trophies\":" << pd.trophies << ",\n";
    ss << "\"playerLevel\":" << pd.playerLevel << ",\n";
    ss << "\"totalXP\":" << pd.totalXP << ",\n";

    // Units
    ss << "\"units\":[";
    for (int i = 0; i < TOTAL_UNITS; i++) {
        if (i > 0) ss << ",";
        ss << "{\"o\":" << (pd.units[i].owned ? 1 : 0)
           << ",\"c\":" << pd.units[i].cards
           << ",\"l\":" << pd.units[i].level << "}";
    }
    ss << "],\n";

    // Deck
    ss << "\"deck\":[";
    for (int i = 0; i < 5; i++) {
        if (i > 0) ss << ",";
        ss << pd.deck[i];
    }
    ss << "],\n";

    // Stats
    ss << "\"totalWins\":" << pd.totalWins << ",\n";
    ss << "\"totalLosses\":" << pd.totalLosses << ",\n";
    ss << "\"totalKills\":" << pd.totalKills << ",\n";
    ss << "\"totalMerges\":" << pd.totalMerges << ",\n";
    ss << "\"totalGoldEarned\":" << pd.totalGoldEarned << ",\n";
    ss << "\"highestWave\":" << pd.highestWave << ",\n";
    ss << "\"maxUnitLevel\":" << pd.maxUnitLevel << ",\n";
    ss << "\"wonWithoutDamage\":" << (pd.wonWithoutDamage ? 1 : 0) << ",\n";
    ss << "\"wonWithSingleType\":" << (pd.wonWithSingleType ? 1 : 0) << ",\n";

    // Settings
    ss << "\"soundEnabled\":" << (pd.soundEnabled ? 1 : 0) << ",\n";
    ss << "\"musicEnabled\":" << (pd.musicEnabled ? 1 : 0) << ",\n";

    // Daily login
    ss << "\"lastLoginDate\":";
    jsonEscapeAppend(ss, pd.lastLoginDate);
    ss << ",\n";
    ss << "\"loginStreak\":" << pd.loginStreak << ",\n";
    ss << "\"lastClaimedDay\":" << pd.lastClaimedDay << ",\n";

    // Season pass
    ss << "\"seasonXP\":" << pd.seasonXP << ",\n";
    ss << "\"seasonClaimedTier\":" << pd.seasonClaimedTier << ",\n";

    // Family upgrades
    ss << "\"familyUpgrade\":[";
    for (int i = 0; i < 5; i++) {
        if (i > 0) ss << ",";
        ss << pd.familyUpgrade[i];
    }
    ss << "],\n";

    // Achievements
    ss << "\"achievements\":[";
    for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
        if (i > 0) ss << ",";
        ss << (ach.isUnlocked(i) ? 1 : 0);
    }
    ss << "],\n";

    // Stage data
    ss << "\"stageData\":{\"currentStageId\":" << pd.stageId << "},\n";
    ss << "\"difficulty\":" << pd.difficulty << ",\n";

    // Checksum (computed over everything above)
    std::string partialJson = ss.str();
    std::string checksum = computeChecksum(partialJson);
    ss << "\"checksum\":";
    jsonEscapeAppend(ss, checksum);
    ss << "\n}";

    return ss.str();
}

// ---- Simple JSON parser (minimal, for our known format) ----

static int jsonParseInt(const std::string& json, const std::string& key, int defaultVal) {
    std::string search = "\"" + key + "\":";
    auto pos = json.find(search);
    if (pos == std::string::npos) return defaultVal;
    pos += search.length();
    return std::atoi(json.c_str() + pos);
}

static std::string jsonParseString(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\":\"";
    auto pos = json.find(search);
    if (pos == std::string::npos) return "";
    pos += search.length();
    auto end = json.find('"', pos);
    if (end == std::string::npos) return "";
    return json.substr(pos, end - pos);
}

static bool jsonParseIntArray(const std::string& json, const std::string& key,
                               int* out, int maxCount) {
    std::string search = "\"" + key + "\":[";
    auto pos = json.find(search);
    if (pos == std::string::npos) return false;
    pos += search.length();

    int idx = 0;
    while (idx < maxCount && pos < json.length()) {
        if (json[pos] == ']') break;
        if (json[pos] == ',' || json[pos] == ' ') { pos++; continue; }
        out[idx++] = std::atoi(json.c_str() + pos);
        while (pos < json.length() && json[pos] != ',' && json[pos] != ']') pos++;
    }
    return true;
}

bool SaveSystem::deserializeFromJson(const std::string& json) {
    if (json.empty()) return false;

    // Verify checksum
    std::string checksum = jsonParseString(json, "checksum");
    if (!checksum.empty()) {
        auto pos = json.find("\"checksum\"");
        if (pos != std::string::npos) {
            std::string dataOnly = json.substr(0, pos);
            if (computeChecksum(dataOnly) != checksum) {
                aout << "SaveSystem: CHECKSUM MISMATCH - data corrupted!" << std::endl;
                return false;
            }
        }
    }

    auto& pd = PlayerData::get();
    auto& ach = AchievementSystem::get();

    int version = jsonParseInt(json, "version", 1);
    if (version != 1) {
        aout << "SaveSystem: Unknown save version " << version << std::endl;
        return false;
    }

    pd.gold = jsonParseInt(json, "gold", 500);
    pd.diamonds = jsonParseInt(json, "diamonds", 0);
    pd.gas = jsonParseInt(json, "gas", 0);
    pd.trophies = jsonParseInt(json, "trophies", 0);
    pd.playerLevel = jsonParseInt(json, "playerLevel", 1);
    pd.totalXP = jsonParseInt(json, "totalXP", 0);

    // Parse units array
    std::string unitsSearch = "\"units\":[";
    auto uPos = json.find(unitsSearch);
    if (uPos != std::string::npos) {
        uPos += unitsSearch.length();
        for (int i = 0; i < TOTAL_UNITS && uPos < json.length(); i++) {
            auto objStart = json.find('{', uPos);
            auto objEnd = json.find('}', objStart);
            if (objStart == std::string::npos || objEnd == std::string::npos) break;

            std::string obj = json.substr(objStart, objEnd - objStart + 1);
            pd.units[i].owned = (jsonParseInt(obj, "o", 0) != 0);
            pd.units[i].cards = jsonParseInt(obj, "c", 0);
            pd.units[i].level = jsonParseInt(obj, "l", 1);
            uPos = objEnd + 1;
        }
    }

    // Deck
    jsonParseIntArray(json, "deck", pd.deck, 5);

    // Stats
    pd.totalWins = jsonParseInt(json, "totalWins", 0);
    pd.totalLosses = jsonParseInt(json, "totalLosses", 0);
    pd.totalKills = jsonParseInt(json, "totalKills", 0);
    pd.totalMerges = jsonParseInt(json, "totalMerges", 0);
    pd.totalGoldEarned = jsonParseInt(json, "totalGoldEarned", 0);
    pd.highestWave = jsonParseInt(json, "highestWave", 0);
    pd.maxUnitLevel = jsonParseInt(json, "maxUnitLevel", 1);
    pd.wonWithoutDamage = (jsonParseInt(json, "wonWithoutDamage", 0) != 0);
    pd.wonWithSingleType = (jsonParseInt(json, "wonWithSingleType", 0) != 0);

    // Settings
    pd.soundEnabled = (jsonParseInt(json, "soundEnabled", 1) != 0);
    pd.musicEnabled = (jsonParseInt(json, "musicEnabled", 1) != 0);

    // Daily login
    pd.lastLoginDate = jsonParseString(json, "lastLoginDate");
    pd.loginStreak = jsonParseInt(json, "loginStreak", 0);
    pd.lastClaimedDay = jsonParseInt(json, "lastClaimedDay", 0);

    // Season pass
    pd.seasonXP = jsonParseInt(json, "seasonXP", 0);
    pd.seasonClaimedTier = jsonParseInt(json, "seasonClaimedTier", 0);

    // Family upgrades
    jsonParseIntArray(json, "familyUpgrade", pd.familyUpgrade, 5);

    // Stage data
    {
        std::string stageSearch = "\"stageData\":{";
        auto stagePos = json.find(stageSearch);
        if (stagePos != std::string::npos) {
            auto stageEnd = json.find('}', stagePos + stageSearch.length());
            if (stageEnd != std::string::npos) {
                std::string stageObj = json.substr(stagePos, stageEnd - stagePos + 1);
                pd.stageId = jsonParseInt(stageObj, "currentStageId", 0);
            }
        }
    }
    pd.difficulty = jsonParseInt(json, "difficulty", 0);

    // Calculate max waves based on stage
    {
        static const int stageWaves[] = {40, 40, 45, 45, 50, 60};
        pd.stageMaxWaves = (pd.stageId >= 0 && pd.stageId < 6) ? stageWaves[pd.stageId] : 40;
    }

    // Achievements
    int achArr[ACHIEVEMENT_COUNT] = {};
    if (jsonParseIntArray(json, "achievements", achArr, ACHIEVEMENT_COUNT)) {
        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            ach.setUnlocked(i, achArr[i] != 0);
        }
    }

    aout << "SaveSystem: Loaded save data (gold=" << pd.gold
         << " trophies=" << pd.trophies << ")" << std::endl;
    return true;
}

void SaveSystem::save() {
    std::string json = serializeToJson();
    jniSave(json);
    aout << "SaveSystem: Saved (" << json.length() << " bytes)" << std::endl;
}

bool SaveSystem::load() {
    std::string json = jniLoad();
    if (json.empty()) {
        aout << "SaveSystem: No save data found, using defaults" << std::endl;
        return false;
    }
    return deserializeFromJson(json);
}

void SaveSystem::resetToDefaults() {
    PlayerData::get().initDefaults();
    AchievementSystem::get().reset();
    save();
    aout << "SaveSystem: Reset to defaults" << std::endl;
}
