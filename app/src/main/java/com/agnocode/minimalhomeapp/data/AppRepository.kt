package com.agnocode.minimalhomeapp.data

import android.content.Intent
import android.content.pm.PackageManager
import com.agnocode.minimalhomeapp.PreferenceManager
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.FocusMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val pm: PackageManager,
    private val prefs: PreferenceManager
) {
    val favoriteAppsFlow: Flow<Set<String>?> = prefs.favoriteAppsFlow
    val blockedAppsFlow: Flow<Map<String, Long?>> = prefs.blockedAppsFlow
    val focusModesFlow: Flow<List<FocusMode>> = prefs.focusModesFlow
    val activeFocusModeFlow: Flow<String?> = prefs.activeFocusModeFlow
    val fontFamilyFlow: Flow<String> = prefs.fontFamilyFlow
    val iconPackPackageFlow: Flow<String?> = prefs.iconPackPackageFlow
    val showIconsFlow: Flow<Boolean> = prefs.showIconsFlow
    val favoritesInitializedFlow: Flow<Boolean> = prefs.favoritesInitializedFlow
    val dailyNotesFlow: Flow<Map<String, DailyNote>> = prefs.dailyNotesFlow

    suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        pm.queryIntentActivities(launcherIntent, 0)
            .map {
                AppItem(
                    label = it.loadLabel(pm).toString().ifEmpty { it.activityInfo.packageName },
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun saveFavorites(favorites: Set<String>) {
        prefs.saveFavorites(favorites)
    }

    suspend fun saveBlockedApps(blocked: Map<String, Long?>) {
        prefs.saveBlockedApps(blocked)
    }

    suspend fun saveFocusModes(modes: List<FocusMode>) {
        prefs.saveFocusModes(modes)
    }

    suspend fun setActiveFocusMode(name: String?) {
        prefs.setActiveFocusMode(name)
    }

    suspend fun saveFontFamily(fontFamily: String) {
        prefs.saveFontFamily(fontFamily)
    }

    suspend fun saveIconPackPackage(packageName: String?) {
        prefs.saveIconPackPackage(packageName)
    }

    suspend fun saveShowIcons(show: Boolean) {
        prefs.saveShowIcons(show)
    }

    suspend fun setFavoritesInitialized() {
        prefs.setFavoritesInitialized()
    }

    suspend fun saveDailyNotes(notes: Map<String, DailyNote>) {
        prefs.saveDailyNotes(notes)
    }

    suspend fun getAvailableIconPacks(): List<AppItem> = withContext(Dispatchers.Default) {
        val intent = Intent("com.novalauncher.THEME")
        pm.queryIntentActivities(intent, 0).map {
            AppItem(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }
    }
}
