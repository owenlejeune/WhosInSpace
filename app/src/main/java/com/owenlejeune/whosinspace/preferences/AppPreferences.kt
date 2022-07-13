package com.owenlejeune.whosinspace.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    companion object {
        private val PREF_FILE = "whos_in_space_shared_preferences"

        private val USE_MONET_COLORS_KEY = "use_monet_colors"
        private val TEST_JSON_KEY = "test_json"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    var useMonetColors: Boolean
        get() = preferences.getBoolean(USE_MONET_COLORS_KEY, false)
        set(value) { preferences.put(USE_MONET_COLORS_KEY, value) }

    val useTestJson = false
    var testJson: String
        get() = preferences.getString(TEST_JSON_KEY, "") ?: ""
        set(value) { preferences.put(TEST_JSON_KEY, value) }

    private fun SharedPreferences.put(key: String, value: Any?) {
        edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putFloat(key, value.toFloat())
                is String -> putString(key, value)
                else -> throw UnsupportedTypeError()
            }
            apply()
        }
    }

    class UnsupportedTypeError: Exception()

}