package com.example.jaygame

import android.content.Context
import androidx.annotation.Keep

@Keep
class SaveBridge {
    companion object {
        private const val PREFS_NAME = "jaygame_save"
        private const val KEY_SAVE_DATA = "save_data"

        @JvmStatic
        @Keep
        fun save(context: Context, json: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_SAVE_DATA, json).apply()
            } catch (e: Exception) {
                android.util.Log.e("SaveBridge", "Failed to save: ${e.message}")
            }
        }

        @JvmStatic
        @Keep
        fun load(context: Context): String? {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getString(KEY_SAVE_DATA, null)
            } catch (e: Exception) {
                android.util.Log.e("SaveBridge", "Failed to load: ${e.message}")
                null
            }
        }
    }
}
