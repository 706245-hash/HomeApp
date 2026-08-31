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
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.FocusMode
import com.agnocode.minimalhomeapp.data.model.NoteTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val pm: PackageManager
) : ViewModel() {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _apps = mutableStateListOf<AppItem>()
    val apps: List<AppItem> = _apps

    var isRefreshing = mutableStateOf(false)
        private set

    val favoritePackages = mutableStateListOf<String>()
    val blockedApps = mutableStateMapOf<String, Long?>()
    
    val focusModes = mutableStateListOf<FocusMode>()
    var activeFocusModeName = mutableStateOf<String?>(null)

    var fontFamily = mutableStateOf("default")
    var iconPackPackage = mutableStateOf<String?>(null)
    var showIcons = mutableStateOf(false)

    val availableIconPacks = mutableStateListOf<AppItem>()

    // Quick Note State
    private val allDailyNotes = mutableStateMapOf<String, DailyNote>()
    var selectedNoteDate = mutableStateOf(dateFmt.format(Date()))
    var currentNoteText = mutableStateOf("")
    val currentTasks = mutableStateListOf<NoteTask>()

    var searchQuery = mutableStateOf("")
    
    var universalSearchQuery = mutableStateOf("")
    var isUniversalSearchActive = mutableStateOf(false)

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    val favoriteAppsFlow: StateFlow<Set<String>?> = repository.favoriteAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    val focusModesFlow: StateFlow<List<FocusMode>> = repository.focusModesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeFocusModeFlow: StateFlow<String?> = repository.activeFocusModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val fontFamilyFlow: StateFlow<String> = repository.fontFamilyFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "default"
    )

    val iconPackPackageFlow: StateFlow<String?> = repository.iconPackPackageFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val showIconsFlow: StateFlow<Boolean> = repository.showIconsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    private val favoritesInitializedFlow: StateFlow<Boolean> = repository.favoritesInitializedFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val dailyNotesFlow: StateFlow<Map<String, DailyNote>> = repository.dailyNotesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    init {
        refreshApps()
        collectFavorites()
        collectBlockedApps()
        collectFocusModes()
        collectThemeSettings()
        collectDailyNotes()
        refreshIconPacks()
        startExpiryPruning()
        startScheduleMonitor()
        startDailyRefresh()
    }

    fun refreshApps() {
        viewModelScope.launch {
            isRefreshing.value = true
            // Artificial delay to make the refresh feel more substantial
            delay(1000L)
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

    private fun collectBlockedApps() {
        viewModelScope.launch {
            blockedAppsFlow.collect { map ->
                if (blockedApps.isEmpty()) {
                    blockedApps.putAll(map)
                }
            }
        }
    }

    private fun collectFocusModes() {
        viewModelScope.launch {
            focusModesFlow.collect { list ->
                focusModes.clear()
                focusModes.addAll(list)
            }
        }
        viewModelScope.launch {
            activeFocusModeFlow.collect { name ->
                activeFocusModeName.value = name
            }
        }
    }

    private fun collectThemeSettings() {
        viewModelScope.launch {
            fontFamilyFlow.collect { fontFamily.value = it }
        }
        viewModelScope.launch {
            iconPackPackageFlow.collect { iconPackPackage.value = it }
        }
        viewModelScope.launch {
            showIconsFlow.collect { showIcons.value = it }
        }
    }

    private fun collectDailyNotes() {
        viewModelScope.launch {
            dailyNotesFlow.collect { map ->
                allDailyNotes.clear()
                allDailyNotes.putAll(map)
                loadSelectedNote()
            }
        }
    }

    private fun loadSelectedNote() {
        val note = allDailyNotes[selectedNoteDate.value] ?: DailyNote(selectedNoteDate.value)
        currentNoteText.value = note.content
        currentTasks.clear()
        currentTasks.addAll(note.tasks)
    }

    fun selectNoteDate(date: String) {
        saveCurrentNote()
        selectedNoteDate.value = date
        loadSelectedNote()
    }

    fun updateNoteText(text: String) {
        currentNoteText.value = text
        saveCurrentNote()
    }

    fun addTask() {
        val newTask = NoteTask(UUID.randomUUID().toString(), "")
        currentTasks.add(0, newTask) // Add to top
        saveCurrentNote()
    }

    fun updateTaskText(id: String, text: String) {
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            currentTasks[index] = currentTasks[index].copy(text = text)
            saveCurrentNote()
        }
    }

    fun toggleTask(id: String, checked: Boolean) {
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            currentTasks[index] = currentTasks[index].copy(isChecked = checked)
            saveCurrentNote()
        }
    }

    fun deleteTask(id: String) {
        currentTasks.removeAll { it.id == id }
        saveCurrentNote()
    }

    private fun saveCurrentNote() {
        val note = DailyNote(selectedNoteDate.value, currentNoteText.value, currentTasks.toList())
        allDailyNotes[selectedNoteDate.value] = note
        viewModelScope.launch {
            repository.saveDailyNotes(allDailyNotes.toMap())
        }
    }

    fun getAvailableDates(): List<String> {
        val today = dateFmt.format(Date())
        return (allDailyNotes.keys + today)
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinct()
            .sortedDescending()
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

    private fun startScheduleMonitor() {
        viewModelScope.launch {
            while (true) {
                val calendar = java.util.Calendar.getInstance()
                val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
                
                val scheduledMode = focusModes.find { mode ->
                    val start = mode.startTime
                    val end = mode.endTime
                    if (start != null && end != null) {
                        if (start < end) {
                            currentMinutes in start until end
                        } else {
                            // Overnight schedule (e.g., 22:00 to 06:00)
                            currentMinutes >= start || currentMinutes < end
                        }
                    } else false
                }

                if (scheduledMode != null) {
                    if (activeFocusModeName.value != scheduledMode.name) {
                        toggleFocusMode(scheduledMode.name)
                    }
                } else {
                    // If the current active mode was a scheduled one that has now ended, deactivate it
                    val currentMode = focusModes.find { it.name == activeFocusModeName.value }
                    if (currentMode?.startTime != null && currentMode.endTime != null) {
                        toggleFocusMode(null)
                    }
                }
                
                // Check every 30 seconds
                delay(30000L)
            }
        }
    }

    private fun startDailyRefresh() {
        viewModelScope.launch {
            while (true) {
                val today = dateFmt.format(Date())
                if (selectedNoteDate.value != today && !allDailyNotes.containsKey(selectedNoteDate.value)) {
                    // If we're on a date that doesn't exist anymore (shouldn't happen with history)
                    // or if it's a new day and we were on 'today', switch to the new 'today'.
                    selectedNoteDate.value = today
                    loadSelectedNote()
                }
                delay(60000L) // Check every minute
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

    fun addFocusMode(name: String, allowedPackages: Set<String>, startTime: Int? = null, endTime: Int? = null) {
        val newMode = FocusMode(name, allowedPackages, startTime, endTime)
        val newList = focusModes.filter { it.name != name } + newMode
        saveFocusModes(newList)
    }

    fun deleteFocusMode(name: String) {
        if (activeFocusModeName.value == name) {
            toggleFocusMode(null)
        }
        val newList = focusModes.filter { it.name != name }
        saveFocusModes(newList)
    }

    fun toggleFocusMode(name: String?) {
        viewModelScope.launch {
            repository.setActiveFocusMode(name)
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            repository.saveFontFamily(family)
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

    private fun saveFocusModes(modes: List<FocusMode>) {
        viewModelScope.launch {
            repository.saveFocusModes(modes)
        }
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
        val activeMode = focusModes.find { it.name == activeFocusModeName.value }
        val filteredByMode = if (activeMode != null) {
            _apps.filter { it.packageName in activeMode.allowedPackages }
        } else {
            _apps
        }
        
        val filteredByBlock = filteredByMode.filter { it.packageName !in blockedApps }
        
        return if (searchQuery.value.isEmpty()) {
            filteredByBlock
        } else {
            filteredByBlock.filter { it.label.contains(searchQuery.value, ignoreCase = true) }
        }
    }

    fun getFavorites(): List<AppItem> {
        val activeMode = focusModes.find { it.name == activeFocusModeName.value }
        val filteredByMode = if (activeMode != null) {
            _apps.filter { it.packageName in activeMode.allowedPackages }
        } else {
            _apps
        }
        
        return filteredByMode.filter { it.packageName in favoritePackages && it.packageName !in blockedApps }
    }

    fun getUniversalSearchResults(): List<AppItem> {
        val query = universalSearchQuery.value
        if (query.isEmpty()) return emptyList()
        
        // Use all apps, but still respect blocked apps
        return _apps.filter { 
            it.packageName !in blockedApps && 
            it.label.contains(query, ignoreCase = true)
        }.take(5) // Limit to 5 results for speed/minimalism
    }

    suspend fun getAvailableIconPacks(): List<AppItem> {
        return repository.getAvailableIconPacks()
    }

    fun triggerResetToHome() {
        _resetToHomeEvent.tryEmit(Unit)
    }
}
