package com.agnocode.minimalhomeapp.ui

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.FocusMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class FocusModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    val focusModes = mutableStateListOf<FocusMode>()
    var activeFocusModeName = mutableStateOf<String?>(null)
    val blockedApps = mutableStateMapOf<String, Long?>()
    var dndSyncEnabled = mutableStateOf(false)
    val protectedPackages = mutableStateSetOf<String>()
    var biometricFocusLock = mutableStateOf(false)

    val focusModesFlow: StateFlow<List<FocusMode>> = repository.focusModesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeFocusModeFlow: StateFlow<String?> = repository.activeFocusModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val blockedAppsFlow: StateFlow<Map<String, Long?>> = repository.blockedAppsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    val dndSyncEnabledFlow: StateFlow<Boolean> = repository.dndSyncEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val protectedPackagesFlow: StateFlow<Set<String>> = repository.protectedPackagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )

    val biometricFocusLockFlow: StateFlow<Boolean> = repository.biometricFocusLockFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    init {
        collectFocusModes()
        collectBlockedApps()
        collectSettings()
        collectSecurity()
        startExpiryPruning()
        // No more schedule monitor polling!
        viewModelScope.launch {
            repository.updateFocusSchedule()
        }
    }

    private fun collectSettings() {
        viewModelScope.launch {
            dndSyncEnabledFlow.collect { enabled ->
                dndSyncEnabled.value = enabled
                // Sync DND state immediately when preference changes
                toggleSystemDND(activeFocusModeName.value != null)
            }
        }
    }

    private fun collectSecurity() {
        viewModelScope.launch {
            protectedPackagesFlow.collect { set ->
                protectedPackages.clear()
                protectedPackages.addAll(set)
            }
        }
        viewModelScope.launch {
            biometricFocusLockFlow.collect { enabled ->
                biometricFocusLock.value = enabled
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
                toggleSystemDND(name != null)
            }
        }
    }

    fun setDndSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDndSyncEnabled(enabled)
        }
    }

    fun toggleProtectedPackage(packageName: String) {
        val newSet = protectedPackages.toMutableSet()
        if (newSet.contains(packageName)) {
            newSet.remove(packageName)
        } else {
            newSet.add(packageName)
        }
        viewModelScope.launch {
            repository.saveProtectedPackages(newSet)
        }
    }

    fun setBiometricFocusLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricFocusLock(enabled)
        }
    }

    fun hasDndPermission(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun toggleSystemDND(isActive: Boolean) {
        if (!dndSyncEnabled.value) return
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            try {
                if (isActive) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                } else {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            } catch (e: Exception) {
                // Could happen on some devices/versions
            }
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
