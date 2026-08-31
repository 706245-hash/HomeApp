package com.agnocode.minimalhomeapp.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.FocusMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class FocusModeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val focusModes = mutableStateListOf<FocusMode>()
    var activeFocusModeName = mutableStateOf<String?>(null)
    val blockedApps = mutableStateMapOf<String, Long?>()

    val focusModesFlow: StateFlow<List<FocusMode>> = repository.focusModesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeFocusModeFlow: StateFlow<String?> = repository.activeFocusModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    init {
        collectFocusModes()
        collectBlockedApps()
        startExpiryPruning()
        // No more schedule monitor polling!
        viewModelScope.launch {
            repository.updateFocusSchedule()
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

    fun blockApp(packageName: String, expiry: Long?) {
        blockedApps[packageName] = expiry
        saveBlockedApps()
    }

    fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        saveBlockedApps()
    }

    fun addFocusMode(name: String, allowedPackages: Set<String>, startTime: Int? = null, endTime: Int? = null, oldName: String? = null) {
        val newMode = FocusMode(name, allowedPackages, startTime, endTime)
        viewModelScope.launch {
            if (oldName != null && oldName != name) {
                repository.deleteFocusMode(oldName)
                if (activeFocusModeName.value == oldName) {
                    repository.setActiveFocusMode(name)
                }
            }
            repository.saveFocusMode(newMode)
        }
    }

    fun deleteFocusMode(name: String) {
        if (activeFocusModeName.value == name) {
            toggleFocusMode(null)
        }
        viewModelScope.launch {
            repository.deleteFocusMode(name)
        }
    }

    fun toggleFocusMode(name: String?) {
        viewModelScope.launch {
            repository.setActiveFocusMode(name)
        }
    }

    private fun saveBlockedApps() {
        viewModelScope.launch {
            repository.saveBlockedApps(blockedApps.toMap())
        }
    }
}
