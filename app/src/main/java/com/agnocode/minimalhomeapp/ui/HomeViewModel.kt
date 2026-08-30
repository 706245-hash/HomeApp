package com.agnocode.minimalhomeapp.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.AppItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val pm: PackageManager
) : ViewModel() {

    private val _apps = mutableStateListOf<AppItem>()
    val apps: List<AppItem> = _apps

    val favoritePackages = mutableStateListOf<String>()
    val blockedApps = mutableStateMapOf<String, Long?>()

    var searchQuery = mutableStateOf("")

    val favoriteAppsFlow: StateFlow<Set<String>> = repository.favoriteAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )

    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    init {
        refreshApps()
        collectFavorites()
        collectBlockedApps()
        startExpiryPruning()
    }

    fun refreshApps() {
        viewModelScope.launch {
            val newList = repository.getInstalledApps()
            _apps.clear()
            _apps.addAll(newList)
        }
    }

    private fun collectFavorites() {
        viewModelScope.launch {
            favoriteAppsFlow.collect { list ->
                if (favoritePackages.isEmpty() && list.isNotEmpty()) {
                    favoritePackages.addAll(list)
                } else if (list.isEmpty() && favoritePackages.isEmpty()) {
                    setSmartDefaults()
                }
            }
        }
    }

    private fun setSmartDefaults() {
        val defaults = mutableSetOf<String>()
        pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName?.let { defaults.add(it) }
        pm.resolveActivity(Intent(Intent.ACTION_VIEW, "http://".toUri()), 0)?.activityInfo?.packageName?.let { defaults.add(it) }
        pm.resolveActivity(Intent(Intent.ACTION_SENDTO, "smsto:".toUri()), 0)?.activityInfo?.packageName?.let { defaults.add(it) }
        
        if (defaults.isEmpty()) {
            defaults.addAll(listOf("com.android.chrome", "com.google.android.apps.messaging"))
        }
        favoritePackages.addAll(defaults)
        saveFavorites()
    }

    private fun collectBlockedApps() {
        viewModelScope.launch {
            blockedAppsFlow.collect { map ->
                if (blockedApps.isEmpty()) {
                    blockedApps.putAll(map)
                }
            }
        }
    }

    private fun startExpiryPruning() {
        viewModelScope.launch {
            while (true) {
                val nextExpiry = blockedApps.values.filterNotNull().minOrNull()
                if (nextExpiry != null) {
                    val delayMillis = nextExpiry - System.currentTimeMillis()
                    if (delayMillis > 0) {
                        delay(delayMillis.milliseconds)
                    }
                    val now = System.currentTimeMillis()
                    val expired = blockedApps.filter { it.value != null && it.value!! <= now }.keys
                    if (expired.isNotEmpty()) {
                        expired.forEach { blockedApps.remove(it) }
                        saveBlockedApps()
                    }
                } else {
                    delay(5000) // Poll every 5 seconds if no timers active
                }
            }
        }
    }

    fun toggleFavorite(packageName: String) {
        if (favoritePackages.contains(packageName)) {
            favoritePackages.remove(packageName)
        } else {
            favoritePackages.add(packageName)
        }
        saveFavorites()
    }

    fun removeFavorite(packageName: String) {
        favoritePackages.remove(packageName)
        saveFavorites()
    }

    fun blockApp(packageName: String, expiry: Long?) {
        blockedApps[packageName] = expiry
        favoritePackages.remove(packageName)
        saveBlockedApps()
        saveFavorites()
    }

    fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        saveBlockedApps()
    }

    private fun saveFavorites() {
        viewModelScope.launch {
            repository.saveFavorites(favoritePackages.toSet())
        }
    }

    private fun saveBlockedApps() {
        viewModelScope.launch {
            repository.saveBlockedApps(blockedApps.toMap())
        }
    }
    
    fun getVisibleApps(): List<AppItem> {
        val filteredByBlock = _apps.filter { it.packageName !in blockedApps }
        return if (searchQuery.value.isEmpty()) {
            filteredByBlock
        } else {
            filteredByBlock.filter { it.label.contains(searchQuery.value, ignoreCase = true) }
        }
    }

    fun getFavorites(): List<AppItem> {
        return _apps.filter { it.packageName in favoritePackages && it.packageName !in blockedApps }
    }
}
