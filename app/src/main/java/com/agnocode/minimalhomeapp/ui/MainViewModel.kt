package com.agnocode.minimalhomeapp.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.worker.DailyBackupWorker
import com.agnocode.minimalhomeapp.util.SearchCommandEngine
import com.agnocode.minimalhomeapp.util.SmartAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    val universalSearchQuery = MutableStateFlow("")
    var isUniversalSearchActive = mutableStateOf(false)
    var selectedWidget = mutableStateOf("none")
    var showFavorites = mutableStateOf(true)
    var autoSyncEnabled = mutableStateOf(false)
    var autoSyncUri = mutableStateOf<String?>(null)

    val smartAction: StateFlow<SmartAction?> = universalSearchQuery
        .map { SearchCommandEngine.parse(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    val selectedWidgetFlow: StateFlow<String> = repository.selectedWidgetFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "none"
    )

    val showFavoritesFlow: StateFlow<Boolean> = repository.showFavoritesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val autoSyncEnabledFlow: StateFlow<Boolean> = repository.autoSyncEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val autoSyncUriFlow: StateFlow<String?> = repository.autoSyncUriFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    init {
        collectSettings()
        viewModelScope.launch {
            repository.checkAndPerformMigration()
        }
    }

    private fun collectSettings() {
        viewModelScope.launch {
            selectedWidgetFlow.collect { selectedWidget.value = it }
        }
        viewModelScope.launch {
            showFavoritesFlow.collect { showFavorites.value = it }
        }
        viewModelScope.launch {
            autoSyncEnabledFlow.collect { 
                autoSyncEnabled.value = it
                if (it) scheduleAutoSync() else cancelAutoSync()
            }
        }
        viewModelScope.launch {
            autoSyncUriFlow.collect { autoSyncUri.value = it }
        }
    }

    fun setSelectedWidget(widget: String) {
        viewModelScope.launch {
            repository.setSelectedWidget(widget)
        }
    }

    fun setShowFavorites(show: Boolean) {
        viewModelScope.launch {
            repository.setShowFavorites(show)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }

    fun setAutoSyncUri(uri: String?) {
        viewModelScope.launch {
            repository.setAutoSyncUri(uri)
        }
    }

    private fun scheduleAutoSync() {
        val workRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelAutoSync() {
        WorkManager.getInstance(context).cancelUniqueWork("daily_backup")
    }

    fun triggerResetToHome() {
        _resetToHomeEvent.tryEmit(Unit)
    }

    suspend fun exportBackup(): String {
        return repository.generateBackupJson()
    }

    suspend fun importBackup(json: String): Boolean {
        return repository.restoreFromBackupJson(json)
    }
}
