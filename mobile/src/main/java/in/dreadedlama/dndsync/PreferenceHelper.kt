package `in`.dreadedlama.dndsync

import android.content.Context
import androidx.preference.PreferenceManager
import `in`.dreadedlama.dndsync.shared.PreferenceKeys

class PreferencesHelper(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context);

    fun getValue(key: PreferenceKeys): Boolean {
        return prefs.getBoolean(key.key, key.defaultValue)
    }

    fun setValue(key: PreferenceKeys, value: Boolean) {
        prefs.edit().putBoolean(key.key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
