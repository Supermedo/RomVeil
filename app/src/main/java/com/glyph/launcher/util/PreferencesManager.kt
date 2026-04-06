package com.glyph.launcher.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glyph_prefs")

/**
 * A folder-to-platform mapping. User assigns a folder to a specific console.
 */
@Serializable
data class FolderMapping(
    val uri: String,
    val platformTag: String,
    val folderName: String
)

/**
 * Manages persistent user preferences using Jetpack DataStore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val KEY_SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val KEY_ROMS_FOLDER_URI = stringPreferencesKey("roms_folder_uri")
        val KEY_LAST_PLATFORM_FILTER = stringPreferencesKey("last_platform_filter")
        val KEY_FOLDER_MAPPINGS = stringPreferencesKey("folder_mappings")
        val KEY_THEGAMESDB_API_KEY = stringPreferencesKey("thegamesdb_api_key")
        val KEY_RAWG_API_KEY = stringPreferencesKey("rawg_api_key")
        val KEY_MOBYGAMES_API_KEY = stringPreferencesKey("mobygames_api_key")
        val KEY_SS_DEV_ID = stringPreferencesKey("ss_devid")
        val KEY_SS_DEV_PASS = stringPreferencesKey("ss_devpass")
        val KEY_SS_USER = stringPreferencesKey("ss_user")
        val KEY_SS_PASS = stringPreferencesKey("ss_pass")
        val KEY_DEFAULT_SCRAPER = stringPreferencesKey("default_scraper")

        fun emulatorKeyFor(platformTag: String) =
            stringPreferencesKey("emulator_$platformTag")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETUP_COMPLETE] ?: false
    }

    val romsFolderUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ROMS_FOLDER_URI]
    }

    val lastPlatformFilter: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_PLATFORM_FILTER]
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETUP_COMPLETE] = complete
        }
    }

    suspend fun setRomsFolderUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ROMS_FOLDER_URI] = uri
        }
    }

    suspend fun setLastPlatformFilter(platformTag: String?) {
        context.dataStore.edit { prefs ->
            if (platformTag != null) {
                prefs[KEY_LAST_PLATFORM_FILTER] = platformTag
            } else {
                prefs.remove(KEY_LAST_PLATFORM_FILTER)
            }
        }
    }

    fun getEmulatorPreference(platformTag: String): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[emulatorKeyFor(platformTag)]
        }

    suspend fun setEmulatorPreference(platformTag: String, packageName: String) {
        context.dataStore.edit { prefs ->
            prefs[emulatorKeyFor(platformTag)] = packageName
        }
    }

    // ── Folder Mappings ──────────────────────────────────────────────────────

    val folderMappings: Flow<List<FolderMapping>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FOLDER_MAPPINGS] ?: return@map emptyList()
        try {
            json.decodeFromString<List<FolderMapping>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFolderMappingsOnce(): List<FolderMapping> = folderMappings.first()

    suspend fun addFolderMapping(mapping: FolderMapping) {
        context.dataStore.edit { prefs ->
            val current = try {
                val raw = prefs[KEY_FOLDER_MAPPINGS] ?: "[]"
                json.decodeFromString<List<FolderMapping>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            // Don't add duplicate URIs
            current.removeAll { it.uri == mapping.uri }
            current.add(mapping)
            prefs[KEY_FOLDER_MAPPINGS] = json.encodeToString(current)
        }
    }

    suspend fun removeFolderMapping(uri: String) {
        context.dataStore.edit { prefs ->
            val current = try {
                val raw = prefs[KEY_FOLDER_MAPPINGS] ?: "[]"
                json.decodeFromString<List<FolderMapping>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            current.removeAll { it.uri == uri }
            prefs[KEY_FOLDER_MAPPINGS] = json.encodeToString(current)
        }
    }

    suspend fun clearFolderMappings() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_FOLDER_MAPPINGS)
        }
    }

    // ── Scraper API Keys ─────────────────────────────────────────────────────

    val theGamesDbApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEGAMESDB_API_KEY] ?: ""
    }

    suspend fun setTheGamesDbApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEGAMESDB_API_KEY] = key
        }
    }

    val rawgApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RAWG_API_KEY] ?: ""
    }

    suspend fun setRawgApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RAWG_API_KEY] = key
        }
    }

    val mobyGamesApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MOBYGAMES_API_KEY] ?: ""
    }

    suspend fun setMobyGamesApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MOBYGAMES_API_KEY] = key
        }
    }

    val ssDevId: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_SS_DEV_ID] ?: "" }
    suspend fun setSsDevId(valStr: String) { context.dataStore.edit { it[KEY_SS_DEV_ID] = valStr } }

    val ssDevPass: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_SS_DEV_PASS] ?: "" }
    suspend fun setSsDevPass(valStr: String) { context.dataStore.edit { it[KEY_SS_DEV_PASS] = valStr } }

    val ssUser: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_SS_USER] ?: "" }
    suspend fun setSsUser(valStr: String) { context.dataStore.edit { it[KEY_SS_USER] = valStr } }

    val ssPass: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_SS_PASS] ?: "" }
    suspend fun setSsPass(valStr: String) { context.dataStore.edit { it[KEY_SS_PASS] = valStr } }

    /** Preferred scraper: "thegamesdb", "rawg", "mobygames", "screenscraper", or "default". */
    private val validScrapers = listOf("thegamesdb", "rawg", "mobygames", "screenscraper", "default")

    val defaultScraper: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SCRAPER]?.takeIf { it in validScrapers } ?: "thegamesdb"
    }

    suspend fun setDefaultScraper(scraper: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SCRAPER] = scraper.takeIf { it in validScrapers } ?: "thegamesdb"
        }
    }
}
