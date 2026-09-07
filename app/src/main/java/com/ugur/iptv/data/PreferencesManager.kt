package com.ugur.iptv.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ugur_iptv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER = "remember_login"
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_LAST_CHANNEL = "last_channel_id"
        private const val KEY_ASPECT_RATIO = "aspect_ratio_mode"
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value.trim()).apply()

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value.trim()).apply()

    var isRemembered: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER, value).apply()

    var lastChannelId: Int
        get() = prefs.getInt(KEY_LAST_CHANNEL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_CHANNEL, value).apply()

    var aspectRatioMode: Int
        get() = prefs.getInt(KEY_ASPECT_RATIO, 0) // 0: Fit, 1: Fill, 2: 16:9, 3: 4:3
        set(value) = prefs.edit().putInt(KEY_ASPECT_RATIO, value).apply()

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun isFavorite(streamId: Int): Boolean {
        return getFavorites().contains(streamId.toString())
    }

    fun toggleFavorite(streamId: Int): Boolean {
        val currentFavs = getFavorites().toMutableSet()
        val idStr = streamId.toString()
        val newState = if (currentFavs.contains(idStr)) {
            currentFavs.remove(idStr)
            false
        } else {
            currentFavs.add(idStr)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavs).apply()
        return newState
    }

    fun hasSavedCredentials(): Boolean {
        return isRemembered && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }
}
