package com.agnocode.minimalhomeapp.data

import android.content.Intent
import android.content.pm.PackageManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.Calendar
import com.agnocode.minimalhomeapp.PreferenceManager
import com.agnocode.minimalhomeapp.data.local.AppDatabase
import com.agnocode.minimalhomeapp.data.local.dao.FocusModeDao
import com.agnocode.minimalhomeapp.data.local.dao.NoteDao
import com.agnocode.minimalhomeapp.data.FocusModeScheduler
import com.agnocode.minimalhomeapp.data.local.entities.*
import com.agnocode.minimalhomeapp.data.model.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val pm: PackageManager,
    private val prefs: PreferenceManager,
    private val db: AppDatabase,
    private val noteDao: NoteDao,
    private val focusModeDao: FocusModeDao,
    private val scheduler: FocusModeScheduler
) {
    fun getUsageStats(): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        
        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        return stats.associate { it.packageName to it.totalTimeInForeground }
    }
    val favoriteAppsFlow: Flow<Set<String>?> = prefs.favoriteAppsFlow
    val blockedAppsFlow: Flow<Map<String, Long?>> = prefs.blockedAppsFlow
    val activeFocusModeFlow: Flow<String?> = prefs.activeFocusModeFlow
    val iconPackPackageFlow: Flow<String?> = prefs.iconPackPackageFlow
    val showIconsFlow: Flow<Boolean> = prefs.showIconsFlow
    val favoritesInitializedFlow: Flow<Boolean> = prefs.favoritesInitializedFlow
    val dndSyncEnabledFlow: Flow<Boolean> = prefs.dndSyncEnabledFlow
    val protectedPackagesFlow: Flow<Set<String>> = prefs.protectedPackagesFlow
    val biometricFocusLockFlow: Flow<Boolean> = prefs.biometricFocusLockFlow
    val showFavoritesFlow: Flow<Boolean> = prefs.showFavoritesFlow
    val ghostPackagesFlow: Flow<Set<String>> = prefs.ghostPackagesFlow
    val monochromeIconsFlow: Flow<Boolean> = prefs.monochromeIconsFlow
    val accentColorFlow: Flow<String> = prefs.accentColorFlow
    val usageAwarenessModeFlow: Flow<String> = prefs.usageAwarenessModeFlow
    val autoSyncEnabledFlow: Flow<Boolean> = prefs.autoSyncEnabledFlow
    val autoSyncUriFlow: Flow<String?> = prefs.autoSyncUriFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyNotesFlow: Flow<Map<String, DailyNote>> = noteDao.getAllNotesWithTasks().map { notesWithTasks ->
        notesWithTasks.associate { noteWithTasks ->
            noteWithTasks.note.date to DailyNote(
                date = noteWithTasks.note.date,
                content = noteWithTasks.note.content,
                tasks = noteWithTasks.tasks.sortedBy { it.order }.map { NoteTask(it.id, it.text, it.isChecked) }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val focusModesFlow: Flow<List<FocusMode>> = focusModeDao.getAllFocusModes().flatMapLatest { modes ->
        if (modes.isEmpty()) return@flatMapLatest flowOf(emptyList<FocusMode>())
        
        val modeFlows = modes.map { modeEntity ->
            focusModeDao.getPackagesForMode(modeEntity.name).map { pkgs ->
                FocusMode(
                    name = modeEntity.name,
                    allowedPackages = pkgs.toSet(),
                    startTime = modeEntity.startTime,
                    endTime = modeEntity.endTime
                )
            }
        }
        combine(modeFlows) { it.toList() }
    }

    suspend fun checkAndPerformMigration() = withContext(Dispatchers.IO) {
        val lastMaintenance = prefs.lastMaintenanceTimeFlow.first()
        val now = System.currentTimeMillis()
        if (now - lastMaintenance > 7 * 24 * 60 * 60 * 1000L) {
            performDatabaseMaintenance()
            prefs.saveLastMaintenanceTime(now)
        }
        
        if (prefs.dataMigratedFlow.first()) return@withContext

        val existingNotes = prefs.dailyNotesFlow.first()
        val existingFocusModes = prefs.focusModesFlow.first()
        
        if (existingNotes.isNotEmpty()) {
            existingNotes.forEach { (_, note) ->
                saveDailyNote(note)
            }
        }
        
        if (existingFocusModes.isNotEmpty()) {
            existingFocusModes.forEach { mode ->
                saveFocusMode(mode)
            }
        }
        
        prefs.setDataMigrated()
    }

    suspend fun saveDailyNote(note: DailyNote) = withContext(Dispatchers.IO) {
        val entity = NoteEntity(note.date, note.content)
        val tasks = note.tasks.mapIndexed { index, task ->
            TaskEntity(task.id, note.date, task.text, task.isChecked, index)
        }
        noteDao.saveNoteWithTasks(entity, tasks)
    }

    private fun performDatabaseMaintenance() {
        try {
            db.openHelper.writableDatabase.execSQL("VACUUM")
        } catch (e: Exception) {
            Log.e("AppRepository", "Database maintenance failed", e)
        }
    }

    suspend fun updateFocusSchedule() = withContext(Dispatchers.IO) {
        scheduler.scheduleNext()
    }

    suspend fun saveFocusMode(mode: FocusMode) = withContext(Dispatchers.IO) {
        val entity = FocusModeEntity(mode.name, mode.startTime, mode.endTime)
        focusModeDao.saveFocusMode(entity, mode.allowedPackages)
        scheduler.scheduleNext()
    }

    suspend fun deleteFocusMode(name: String) = withContext(Dispatchers.IO) {
        focusModeDao.deleteFocusMode(name)
        scheduler.scheduleNext()
    }

    suspend fun toggleActiveModeIfMatches(name: String?) = withContext(Dispatchers.IO) {
        val current = prefs.activeFocusModeFlow.first()
        if (current == name) {
            prefs.setActiveFocusMode(null)
        }
    }

    suspend fun deleteNote(date: String) = withContext(Dispatchers.IO) {
        // Not implemented in DAO yet, but we should add it if needed
    }

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
        // Now handled per-mode or migrated
    }

    suspend fun setActiveFocusMode(name: String?) {
        prefs.setActiveFocusMode(name)
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

    suspend fun setDndSyncEnabled(enabled: Boolean) {
        prefs.setDndSyncEnabled(enabled)
    }

    suspend fun saveProtectedPackages(packages: Set<String>) {
        prefs.saveProtectedPackages(packages)
    }

    suspend fun setBiometricFocusLock(enabled: Boolean) {
        prefs.setBiometricFocusLock(enabled)
    }

    suspend fun setShowFavorites(show: Boolean) {
        prefs.setShowFavorites(show)
    }

    suspend fun saveGhostPackages(packages: Set<String>) {
        prefs.saveGhostPackages(packages)
    }

    suspend fun setMonochromeIcons(enabled: Boolean) {
        prefs.setMonochromeIcons(enabled)
    }

    suspend fun setAccentColor(color: String) {
        prefs.setAccentColor(color)
    }

    suspend fun setUsageAwarenessMode(mode: String) {
        prefs.setUsageAwarenessMode(mode)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.setAutoSyncEnabled(enabled)
    }

    suspend fun setAutoSyncUri(uri: String?) {
        prefs.setAutoSyncUri(uri)
    }

    suspend fun saveDailyNotes(notes: Map<String, DailyNote>) {
        // Now handled via saveDailyNote individually, but for backward compat/migration:
        notes.values.forEach { saveDailyNote(it) }
    }

    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupModel(
            notes = noteDao.getAllNotesRaw(),
            tasks = noteDao.getAllTasksRaw(),
            focusModes = focusModeDao.getAllFocusModesRaw(),
            focusModePackages = focusModeDao.getAllPackagesRaw().map { 
                FocusModePackageBackup(it.modeName, it.packageName) 
            },
            preferences = prefs.getAllPreferencesForBackup()
        )
        Gson().toJson(backup)
    }

    suspend fun restoreFromBackupJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = Gson().fromJson(json, BackupModel::class.java)
            if (backup == null || backup.version < 1) return@withContext false
            
            // Start restoration
            db.runInTransaction {
                // We use runBlocking here because we are inside a Room transaction callback which is synchronous
                kotlinx.coroutines.runBlocking {
                    // Clear current data
                    noteDao.deleteAllNotes()
                    noteDao.deleteAllTasks()
                    focusModeDao.deleteAllFocusModes()
                    focusModeDao.deleteAllPackages()
                    
                    // Insert backup data
                    backup.notes.forEach { noteDao.insertNote(it) }
                    backup.tasks.forEach { noteDao.insertTask(it) }
                    backup.focusModes.forEach { focusModeDao.insertFocusMode(it) }
                    backup.focusModePackages.forEach { 
                        focusModeDao.insertPackage(FocusModePackageEntity(it.modeName, it.packageName))
                    }
                    
                    // Restore preferences
                    prefs.restorePreferencesFromBackup(backup.preferences)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "Restore failed", e)
            false
        }
    }

    suspend fun searchNotes(query: String): List<NoteEntity> = withContext(Dispatchers.IO) {
        noteDao.searchNotes(query)
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
