package com.agnocode.minimalhomeapp.data.model

import androidx.annotation.Keep
import com.agnocode.minimalhomeapp.data.local.entities.FocusModeEntity
import com.agnocode.minimalhomeapp.data.local.entities.NoteEntity
import com.agnocode.minimalhomeapp.data.local.entities.TaskEntity
import com.google.gson.annotations.SerializedName

@Keep
data class BackupModel(
    @SerializedName("version")
    val version: Int = 1,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("notes")
    val notes: List<NoteEntity> = emptyList(),
    
    @SerializedName("tasks")
    val tasks: List<TaskEntity> = emptyList(),
    
    @SerializedName("focusModes")
    val focusModes: List<FocusModeEntity> = emptyList(),
    
    @SerializedName("focusModePackages")
    val focusModePackages: List<FocusModePackageBackup> = emptyList(),
    
    @SerializedName("preferences")
    val preferences: AppPreferencesBackup = AppPreferencesBackup()
)

@Keep
data class FocusModePackageBackup(
    @SerializedName("modeName")
    val modeName: String,
    
    @SerializedName("packageName")
    val packageName: String
)

@Keep
data class AppPreferencesBackup(
    @SerializedName("favoriteApps")
    val favoriteApps: Set<String> = emptySet(),
    
    @SerializedName("protectedPackages")
    val protectedPackages: Set<String> = emptySet(),
    
    @SerializedName("blockedApps")
    val blockedApps: Map<String, Long?> = emptyMap(),
    
    @SerializedName("iconPackPackage")
    val iconPackPackage: String? = null,
    
    @SerializedName("showIcons")
    val showIcons: Boolean = false,
    
    @SerializedName("dndSyncEnabled")
    val dndSyncEnabled: Boolean = false,
    
    @SerializedName("biometricFocusLock")
    val biometricFocusLock: Boolean = false,
    
    @SerializedName("showFavorites")
    val showFavorites: Boolean = true,
    
    @SerializedName("ghostPackages")
    val ghostPackages: Set<String> = emptySet(),
    
    @SerializedName("monochromeIcons")
    val monochromeIcons: Boolean = false,
    
    @SerializedName("accentColor")
    val accentColor: String = "white"
)
