package com.agnocode.minimalhomeapp.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val universalSearchQuery = MutableStateFlow("")
    var isUniversalSearchActive = mutableStateOf(false)
    var selectedWidget = mutableStateOf("none")
    var showFavorites = mutableStateOf(true)

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    val selectedWidgetFlow: StateFlow<String> = repository.selectedWidgetFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "none"
    )

    val showFavoritesFlow: StateFlow<Boolean> = repository.showFavoritesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
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

    fun triggerResetToHome() {
        _resetToHomeEvent.tryEmit(Unit)
    }
}
