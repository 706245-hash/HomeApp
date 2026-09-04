package com.agnocode.minimalhomeapp.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.local.entities.NoteEntity
import com.agnocode.minimalhomeapp.data.worker.DailyBackupWorker
import com.agnocode.minimalhomeapp.util.SearchCommandEngine
import com.agnocode.minimalhomeapp.util.SmartAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {

    val universalSearchQuery = MutableStateFlow("")
    var isUniversalSearchActive = mutableStateOf(false)
    var showFavorites = mutableStateOf(true)
    var autoSyncEnabled = mutableStateOf(false)
    var autoSyncUri = mutableStateOf<String?>(null)
    var accentColor = mutableStateOf("white")
    var monochromeIcons = mutableStateOf(true)

    val smartAction: StateFlow<SmartAction?> = universalSearchQuery
        .map { SearchCommandEngine.parse(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val noteSearchResults: StateFlow<List<NoteEntity>> = universalSearchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            if (query.length < 3) flowOf(emptyList())
            else flow { emit(repository.searchNotes(query)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    val showFavoritesFlow: StateFlow<Boolean> = repository.showFavoritesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val autoSyncEnabledFlow: StateFlow<Boolean> = repository.autoSyncEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val autoSyncUriFlow: StateFlow<String?> = repository.autoSyncUriFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val accentColorFlow: StateFlow<String> = repository.accentColorFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "white"
    )

    val monochromeIconsFlow: StateFlow<Boolean> = repository.monochromeIconsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    init {
        collectSettings()
        viewModelScope.launch {
            repository.checkAndPerformMigration()
        }
    }

    private fun collectSettings() {
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
        viewModelScope.launch {
            accentColorFlow.collect { accentColor.value = it }
        }
        viewModelScope.launch {
            monochromeIconsFlow.collect { monochromeIcons.value = it }
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

    fun setAccentColor(color: String) {
        viewModelScope.launch {
            repository.setAccentColor(color)
        }
    }

    fun setMonochromeIcons(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonochromeIcons(enabled)
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
