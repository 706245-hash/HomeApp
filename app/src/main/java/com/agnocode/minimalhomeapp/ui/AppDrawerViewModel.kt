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
import com.agnocode.minimalhomeapp.util.isFuzzyMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val repository: AppRepository,
    private val pm: PackageManager
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    var isRefreshing = mutableStateOf(false)
        private set

    val favoritePackages = mutableStateListOf<String>()
    val blockedApps = mutableStateMapOf<String, Long?>()
    
    var iconPackPackage = mutableStateOf<String?>(null)
    var showIcons = mutableStateOf(false)

    val availableIconPacks = mutableStateListOf<AppItem>()
    val searchQuery = MutableStateFlow("")

    val favoriteAppsFlow: StateFlow<Set<String>?> = repository.favoriteAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val iconPackPackageFlow: StateFlow<String?> = repository.iconPackPackageFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val showIconsFlow: StateFlow<Boolean> = repository.showIconsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeAllowedPackagesFlow: StateFlow<Set<String>?> = repository.activeFocusModeFlow
        .flatMapLatest { name ->
            if (name == null) flowOf(null)
            else repository.focusModesFlow.map { modes ->
                modes.find { it.name == name }?.allowedPackages
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val visibleAppsFlow: StateFlow<List<AppItem>> = combine(
        _apps,
        searchQuery,
        blockedAppsFlow,
        activeAllowedPackagesFlow
    ) { allApps, query, blocked, allowed ->
        val filteredByMode = if (allowed != null) {
            allApps.filter { it.packageName in allowed }
        } else {
            allApps
        }
        
        val filteredByBlock = filteredByMode.filter { it.packageName !in blocked.keys }
        
        if (query.isEmpty()) {
            filteredByBlock
        } else {
            filteredByBlock.filter { it.label.isFuzzyMatch(query) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesFlow: StateFlow<List<AppItem>> = combine(
        _apps,
        favoriteAppsFlow,
        blockedAppsFlow,
        activeAllowedPackagesFlow
    ) { allApps, favorites, blocked, allowed ->
        if (favorites == null) return@combine emptyList<AppItem>()
        val filteredByMode = if (allowed != null) {
            allApps.filter { it.packageName in allowed }
        } else {
            allApps
        }
        filteredByMode.filter { it.packageName in favorites && it.packageName !in blocked.keys }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshApps()
        collectFavorites()
        collectThemeSettings()
        collectBlockedApps()
        refreshIconPacks()
    }

    fun refreshApps() {
        viewModelScope.launch {
            isRefreshing.value = true
            val newList = repository.getInstalledApps()
            _apps.value = newList
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

    private fun collectBlockedApps() {
        viewModelScope.launch {
            blockedAppsFlow.collect { map ->
                blockedApps.clear()
                blockedApps.putAll(map)
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
}
