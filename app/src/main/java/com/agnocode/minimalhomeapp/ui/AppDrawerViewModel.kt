package com.agnocode.minimalhomeapp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import coil.ImageLoader
import coil.request.ImageRequest
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val repository: AppRepository,
    private val pm: PackageManager,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    var isRefreshing = mutableStateOf(false)
        private set

    val favoritePackages = mutableStateListOf<String>()
    val blockedApps = mutableStateMapOf<String, Long?>()
    
    var iconPackPackage = mutableStateOf<String?>(null)
    var showIcons = mutableStateOf(false)

    private val iconCache = mutableMapOf<String, android.graphics.drawable.Drawable>()

    val availableIconPacks = mutableStateListOf<AppItem>()
    val searchQuery = MutableStateFlow("")
    private val _usageStats = MutableStateFlow<Map<String, Long>>(emptyMap())
    val usageStats: StateFlow<Map<String, Long>> = _usageStats.asStateFlow()
    var usageAwarenessMode = mutableStateOf("none")

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

    val usageAwarenessModeFlow: StateFlow<String> = repository.usageAwarenessModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "none"
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
        collectUsageSettings()
        refreshIconPacks()
        startUsageMonitor()
    }

    private fun collectUsageSettings() {
        viewModelScope.launch {
            usageAwarenessModeFlow.collect { mode ->
                usageAwarenessMode.value = mode
                if (mode != "none") refreshUsageStats()
            }
        }
    }

    private fun startUsageMonitor() {
        viewModelScope.launch {
            while (true) {
                if (usageAwarenessMode.value != "none") {
                    refreshUsageStats()
                }
                delay(30000) // Refresh usage every 30 seconds
            }
        }
    }

    fun refreshUsageStats() {
        val stats = repository.getUsageStats()
        _usageStats.value = stats
    }

    fun setUsageAwarenessMode(mode: String) {
        viewModelScope.launch {
            repository.setUsageAwarenessMode(mode)
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            isRefreshing.value = true
            val newList = repository.getInstalledApps()
            _apps.value = newList
            isRefreshing.value = false
            prewarmIconCache(newList)
        }
    }

    private fun prewarmIconCache(apps: List<AppItem>) {
        if (!showIcons.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val toCache = (favoritePackages.toList() + apps.take(20).map { it.packageName })
                .distinct()
            
            toCache.forEach { pkg ->
                if (!iconCache.containsKey(pkg)) {
                    try {
                        val icon = pm.getApplicationIcon(pkg)
                        iconCache[pkg] = icon
                        // Also tell Coil to cache the bitmap for smoother UI transitions
                        val request = ImageRequest.Builder(context)
                            .data(icon)
                            .size(100)
                            .build()
                        imageLoader.enqueue(request)
                    } catch (e: Exception) {
                        // Skip
                    }
                }
            }
        }
    }

    fun getIcon(packageName: String): android.graphics.drawable.Drawable? {
        return iconCache[packageName]
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
