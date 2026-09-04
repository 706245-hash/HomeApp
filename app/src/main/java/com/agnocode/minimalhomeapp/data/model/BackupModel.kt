package com.agnocode.minimalhomeapp.data.model

import com.agnocode.minimalhomeapp.data.local.entities.FocusModeEntity
import com.agnocode.minimalhomeapp.data.local.entities.NoteEntity
import com.agnocode.minimalhomeapp.data.local.entities.TaskEntity

data class BackupModel(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<NoteEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val focusModes: List<FocusModeEntity> = emptyList(),
    val focusModePackages: List<FocusModePackageBackup> = emptyList(),
    val preferences: AppPreferencesBackup = AppPreferencesBackup()
)

data class FocusModePackageBackup(
    val modeName: String,
    val packageName: String
)

data class AppPreferencesBackup(
    val favoriteApps: Set<String> = emptySet(),
    val protectedPackages: Set<String> = emptySet(),
    val blockedApps: Map<String, Long?> = emptyMap(),
    val iconPackPackage: String? = null,
    val showIcons: Boolean = false,
    val dndSyncEnabled: Boolean = false,
    val biometricFocusLock: Boolean = false,
    val showFavorites: Boolean = true,
    val ghostPackages: Set<String> = emptySet(),
    val monochromeIcons: Boolean = false,
    val accentColor: String = "white"
)
