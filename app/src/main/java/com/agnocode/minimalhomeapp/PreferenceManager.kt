package com.agnocode.minimalhomeapp

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.FocusMode
import com.agnocode.minimalhomeapp.data.model.NoteTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    companion object {
        val FAVORITE_APPS = stringSetPreferencesKey("favorite_apps")
        val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
        val BLOCKED_EXPIRY = stringPreferencesKey("blocked_expiry") // Format: packageName:timestamp
        val FOCUS_MODES = stringSetPreferencesKey("focus_modes") // Format: name|pkg1,pkg2
        val ACTIVE_FOCUS_MODE = stringPreferencesKey("active_focus_mode")
        val ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
        val SHOW_ICONS = booleanPreferencesKey("show_icons")
        val DAILY_NOTES = stringPreferencesKey("daily_notes") 
        val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
        val DATA_MIGRATED = booleanPreferencesKey("data_migrated_to_room")
        val DND_SYNC_ENABLED = booleanPreferencesKey("dnd_sync_enabled")
        val PROTECTED_PACKAGES = stringSetPreferencesKey("protected_packages")
        val BIOMETRIC_FOCUS_LOCK = booleanPreferencesKey("biometric_focus_lock")
        val SELECTED_WIDGET = stringPreferencesKey("selected_widget")
        val SHOW_FAVORITES = booleanPreferencesKey("show_favorites")
        val LAST_MAINTENANCE_TIME = longPreferencesKey("last_maintenance_time")
    }

    private fun encode(s: String): String = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
    private fun decode(s: String): String = try { String(Base64.decode(s, Base64.NO_WRAP)) } catch(e: Exception) { s }

    val favoriteAppsFlow: Flow<Set<String>?> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_APPS]
    }

    val blockedAppsFlow: Flow<Map<String, Long?>> = context.dataStore.data.map { preferences ->
        val expiryData = preferences[BLOCKED_EXPIRY] ?: ""
        val blockedSet = preferences[BLOCKED_APPS] ?: emptySet()
        
        val expiryMap = expiryData.split(";").filter { it.isNotEmpty() }.associate {
            val parts = it.split("|")
            parts[0] to parts[1].toLongOrNull()
        }
        
        blockedSet.associateWith { expiryMap[it] }
    }

    val focusModesFlow: Flow<List<FocusMode>> = context.dataStore.data.map { preferences ->
        val modesSet = preferences[FOCUS_MODES] ?: emptySet()
        modesSet.map { entry ->
            val parts = entry.split("|")
            val name = parts[0]
            val pkgs = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(",").toSet() else emptySet()
            val startTime = if (parts.size > 2) parts[2].toIntOrNull() else null
            val endTime = if (parts.size > 3) parts[3].toIntOrNull() else null
            FocusMode(name, pkgs, startTime, endTime)
        }
    }

    val activeFocusModeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_FOCUS_MODE]
    }

    val iconPackPackageFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ICON_PACK_PACKAGE]
    }

    val showIconsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_ICONS] ?: false
    }

    val favoritesInitializedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_INITIALIZED] ?: false
    }

    val dataMigratedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DATA_MIGRATED] ?: false
    }

    val dndSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DND_SYNC_ENABLED] ?: false
    }

    val protectedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PROTECTED_PACKAGES] ?: emptySet()
    }

    val biometricFocusLockFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOCUS_LOCK] ?: false
    }

    val selectedWidgetFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_WIDGET] ?: "none"
    }

    val showFavoritesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_FAVORITES] ?: true
    }

    val lastMaintenanceTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_MAINTENANCE_TIME] ?: 0L
    }

    val dailyNotesFlow: Flow<Map<String, DailyNote>> = context.dataStore.data.map { preferences ->
        val raw = preferences[DAILY_NOTES] ?: ""
        if (raw.isEmpty()) emptyMap() else {
            try {
                // New robust format: date[FIELD]contentB64[FIELD]task1Id::task1TextB64::task1Checked[TASK]task2... [NOTE] nextDate...
                raw.split("[NOTE]").filter { it.isNotEmpty() }.associate { entry ->
                    val parts = entry.split("[FIELD]")
                    val date = parts[0]
                    if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        return@associate "" to DailyNote("") 
                    }
                    val content = parts.getOrNull(1)?.let { decode(it) } ?: ""
                    val tasks = parts.getOrNull(2)?.split("[TASK]")?.filter { it.isNotEmpty() }?.map { t ->
                        val tParts = t.split("::")
                        NoteTask(tParts[0], tParts.getOrNull(1)?.let { decode(it) } ?: "", tParts.getOrNull(2) == "true")
                    } ?: emptyList()
                    date to DailyNote(date, content, tasks)
                }.filter { it.key.isNotEmpty() }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun saveFavorites(favorites: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_APPS] = favorites
        }
    }

    suspend fun saveBlockedApps(blocked: Map<String, Long?>) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_APPS] = blocked.keys
            val expiryString = blocked.map { "${it.key}|${it.value ?: "null"}" }.joinToString(";")
            preferences[BLOCKED_EXPIRY] = expiryString
        }
    }

    suspend fun saveFocusModes(modes: List<FocusMode>) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_MODES] = modes.map { 
                "${it.name}|${it.allowedPackages.joinToString(",")}|${it.startTime ?: ""}|${it.endTime ?: ""}"
            }.toSet()
        }
    }

    suspend fun setActiveFocusMode(name: String?) {
        context.dataStore.edit { preferences ->
            if (name == null) {
                preferences.remove(ACTIVE_FOCUS_MODE)
            } else {
                preferences[ACTIVE_FOCUS_MODE] = name
            }
        }
    }

    suspend fun saveIconPackPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName == null) {
                preferences.remove(ICON_PACK_PACKAGE)
            } else {
                preferences[ICON_PACK_PACKAGE] = packageName
            }
        }
    }

    suspend fun saveShowIcons(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ICONS] = show
        }
    }

    suspend fun setFavoritesInitialized() {
        context.dataStore.edit { preferences ->
            preferences[FAVORITES_INITIALIZED] = true
        }
    }

    suspend fun setDataMigrated() {
        context.dataStore.edit { preferences ->
            preferences[DATA_MIGRATED] = true
        }
    }

    suspend fun setDndSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DND_SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveProtectedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PROTECTED_PACKAGES] = packages
        }
    }

    suspend fun setBiometricFocusLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOCUS_LOCK] = enabled
        }
    }

    suspend fun setSelectedWidget(widget: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_WIDGET] = widget
        }
    }

    suspend fun setShowFavorites(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_FAVORITES] = show
        }
    }

    suspend fun saveLastMaintenanceTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_MAINTENANCE_TIME] = time
        }
    }

    suspend fun saveDailyNotes(notes: Map<String, DailyNote>) {
        context.dataStore.edit { preferences ->
            val data = notes.values.joinToString("[NOTE]") { note ->
                val taskData = note.tasks.joinToString("[TASK]") { "${it.id}::${encode(it.text)}::${it.isChecked}" }
                "${note.date}[FIELD]${encode(note.content)}[FIELD]$taskData"
            }
            preferences[DAILY_NOTES] = data
        }
    }

    suspend fun clearLegacyData() {
        context.dataStore.edit { preferences ->
            preferences.remove(DAILY_NOTES)
            preferences.remove(FOCUS_MODES)
            preferences.remove(BLOCKED_EXPIRY) // Will be handled by Room or cleaned up
        }
    }
}
