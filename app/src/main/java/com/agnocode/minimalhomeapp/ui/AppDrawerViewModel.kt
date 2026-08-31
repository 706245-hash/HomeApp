package com.agnocode.minimalhomeapp.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.AppItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val repository: AppRepository,
    private val pm: PackageManager
) : ViewModel() {

    private val _apps = mutableStateListOf<AppItem>()
    val apps: List<AppItem> = _apps

    var isRefreshing = mutableStateOf(false)
        private set

    val favoritePackages = mutableStateListOf<String>()
    
    var iconPackPackage = mutableStateOf<String?>(null)
    var showIcons = mutableStateOf(false)

    val availableIconPacks = mutableStateListOf<AppItem>()
    var searchQuery = mutableStateOf("")

    val favoriteAppsFlow: StateFlow<Set<String>?> = repository.favoriteAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val iconPackPackageFlow: StateFlow<String?> = repository.iconPackPackageFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val showIconsFlow: StateFlow<Boolean> = repository.showIconsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    // Bridge with FocusMode (We might need a way to filter based on active mode)
    // For now, we'll keep it simple and just listen to blocked apps
    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    init {
        refreshApps()
        collectFavorites()
        collectThemeSettings()
        refreshIconPacks()
    }

    fun refreshApps() {
        viewModelScope.launch {
            isRefreshing.value = true
            // Removed artificial delay for better UX
            val newList = repository.getInstalledApps()
            _apps.clear()
            _apps.addAll(newList)
            isRefreshing.value = false
        }
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            val packs = repository.getAvailableIconPacks()
            availableIconPacks.clear()
            availableIconPacks.addAll(packs)
        }
    }

    private fun collectFavorites() {
        viewModelScope.launch {
            favoriteAppsFlow.collect { list ->
                if (list == null) {
                    if (favoritePackages.isEmpty()) {
                        setSmartDefaults()
                    }
                } else {
                    if (favoritePackages.isEmpty() && list.isNotEmpty()) {
                        favoritePackages.addAll(list)
                    }
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

    private fun collectThemeSettings() {
        viewModelScope.launch {
            iconPackPackageFlow.collect { iconPackPackage.value = it }
        }
        viewModelScope.launch {
            showIconsFlow.collect { showIcons.value = it }
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

    private fun saveFavorites() {
        viewModelScope.launch {
            repository.saveFavorites(favoritePackages.toSet())
        }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch {
            repository.saveIconPackPackage(packageName)
        }
    }

    fun setShowIcons(show: Boolean) {
        viewModelScope.launch {
            repository.saveShowIcons(show)
        }
    }

    // Logic to filter visible apps, optionally by Focus Mode
    fun getVisibleApps(allowedPackages: Set<String>? = null): List<AppItem> {
        val filteredByMode = if (allowedPackages != null) {
            _apps.filter { it.packageName in allowedPackages }
        } else {
            _apps
        }
        
        val blocked = blockedAppsFlow.value.keys
        val filteredByBlock = filteredByMode.filter { it.packageName !in blocked }
        
        return if (searchQuery.value.isEmpty()) {
            filteredByBlock
        } else {
            filteredByBlock.filter { it.label.contains(searchQuery.value, ignoreCase = true) }
        }
    }
}
