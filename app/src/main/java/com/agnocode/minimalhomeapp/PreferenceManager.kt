package com.agnocode.minimalhomeapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    companion object {
        val FAVORITE_APPS = stringSetPreferencesKey("favorite_apps")
        val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
        val BLOCKED_EXPIRY = stringPreferencesKey("blocked_expiry") // Format: packageName:timestamp
    }

    val favoriteAppsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_APPS] ?: emptySet()
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
}
