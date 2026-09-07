package com.ugur.iptv.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ugur_iptv_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val TAG = "PreferencesManager"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER = "remember_login"
        private const val KEY_FAVORITES = "favorite_channel_ids"
        private const val KEY_LAST_CHANNEL = "last_channel_id"
        private const val KEY_ASPECT_RATIO = "aspect_ratio_mode"

        private const val BACKUP_FILE_INTERNAL = "ugurtv_auth_backup.json"
        private const val BACKUP_FILE_EXTERNAL = "ugurtv_auth_persistent.json"
    }

    private data class AuthBackup(
        @SerializedName("server_url") val serverUrl: String,
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String,
        @SerializedName("favorites") val favorites: Set<String>,
        @SerializedName("remember") val remember: Boolean
    )

    init {
        // Auto-restore credentials if SharedPreferences were cleared during update
        autoRestoreIfEmpty()
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SERVER_URL, value.trim()).commit()
            syncToBackup()
        }

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USERNAME, value.trim()).commit()
            syncToBackup()
        }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_PASSWORD, value.trim()).commit()
            syncToBackup()
        }

    var isRemembered: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REMEMBER, value).commit()
            syncToBackup()
        }

    var lastChannelId: Int
        get() = prefs.getInt(KEY_LAST_CHANNEL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_CHANNEL, value).apply()

    var aspectRatioMode: Int
        get() = prefs.getInt(KEY_ASPECT_RATIO, 0)
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
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavs).commit()
        syncToBackup()
        return newState
    }

    fun saveCredentials(server: String, user: String, pass: String, remember: Boolean) {
        prefs.edit()
            .putString(KEY_SERVER_URL, server.trim())
            .putString(KEY_USERNAME, user.trim())
            .putString(KEY_PASSWORD, pass.trim())
            .putBoolean(KEY_REMEMBER, remember)
            .commit()

        syncToBackup()
    }

    fun hasSavedCredentials(): Boolean {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            autoRestoreIfEmpty()
        }
        return isRemembered && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .commit()

        deleteBackupFiles()
    }

    /**
     * Writes an exact persistent backup of the credentials & favorites to disk
     * to survive app updates and cache wipes.
     */
    private fun syncToBackup() {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) return

        try {
            val backup = AuthBackup(
                serverUrl = serverUrl,
                username = username,
                password = password,
                favorites = getFavorites(),
                remember = isRemembered
            )
            val json = gson.toJson(backup)

            // 1. Internal filesDir backup
            val internalFile = File(context.filesDir, BACKUP_FILE_INTERNAL)
            internalFile.writeText(json)

            // 2. External app filesDir backup (survives APK upgrades)
            context.getExternalFilesDir(null)?.let { extDir ->
                val extFile = File(extDir, BACKUP_FILE_EXTERNAL)
                extFile.writeText(json)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync credentials to persistent backup", e)
        }
    }

    /**
     * If SharedPreferences is ever empty, restores from the persistent backup file.
     */
    private fun autoRestoreIfEmpty() {
        val currentServer = prefs.getString(KEY_SERVER_URL, "") ?: ""
        val currentUser = prefs.getString(KEY_USERNAME, "") ?: ""
        val currentPass = prefs.getString(KEY_PASSWORD, "") ?: ""

        if (currentServer.isNotBlank() && currentUser.isNotBlank() && currentPass.isNotBlank()) {
            return
        }

        // Try reading internal backup file
        var backupJson: String? = null
        val internalFile = File(context.filesDir, BACKUP_FILE_INTERNAL)
        if (internalFile.exists()) {
            backupJson = try { internalFile.readText() } catch (e: Exception) { null }
        }

        // Try external backup file if internal wasn't found
        if (backupJson.isNullOrBlank()) {
            context.getExternalFilesDir(null)?.let { extDir ->
                val extFile = File(extDir, BACKUP_FILE_EXTERNAL)
                if (extFile.exists()) {
                    backupJson = try { extFile.readText() } catch (e: Exception) { null }
                }
            }
        }

        if (!backupJson.isNullOrBlank()) {
            try {
                val backup = gson.fromJson(backupJson, AuthBackup::class.java)
                if (backup != null && backup.serverUrl.isNotBlank() && backup.username.isNotBlank()) {
                    prefs.edit()
                        .putString(KEY_SERVER_URL, backup.serverUrl)
                        .putString(KEY_USERNAME, backup.username)
                        .putString(KEY_PASSWORD, backup.password)
                        .putBoolean(KEY_REMEMBER, backup.remember)
                        .putStringSet(KEY_FAVORITES, backup.favorites)
                        .commit()
                    Log.i(TAG, "Credentials successfully restored from persistent backup.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse backup JSON", e)
            }
        }
    }

    private fun deleteBackupFiles() {
        try {
            File(context.filesDir, BACKUP_FILE_INTERNAL).delete()
            context.getExternalFilesDir(null)?.let {
                File(it, BACKUP_FILE_EXTERNAL).delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting backup files", e)
        }
    }
}
