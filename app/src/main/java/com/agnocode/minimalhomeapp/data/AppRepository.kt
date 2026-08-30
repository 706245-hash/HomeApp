package com.agnocode.minimalhomeapp.data

import android.content.Intent
import android.content.pm.PackageManager
import com.agnocode.minimalhomeapp.PreferenceManager
import com.agnocode.minimalhomeapp.data.model.AppItem
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
    val favoriteAppsFlow: Flow<Set<String>> = prefs.favoriteAppsFlow
    val blockedAppsFlow: Flow<Map<String, Long?>> = prefs.blockedAppsFlow

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
}
