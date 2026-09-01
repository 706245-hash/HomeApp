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

    var fontFamily = mutableStateOf("default")
    val universalSearchQuery = MutableStateFlow("")
    var isUniversalSearchActive = mutableStateOf(false)

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    val fontFamilyFlow: StateFlow<String> = repository.fontFamilyFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "default"
    )

    init {
        collectThemeSettings()
        viewModelScope.launch {
            repository.checkAndPerformMigration()
        }
    }

    private fun collectThemeSettings() {
        viewModelScope.launch {
            fontFamilyFlow.collect { fontFamily.value = it }
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            repository.saveFontFamily(family)
        }
    }

    fun triggerResetToHome() {
        _resetToHomeEvent.tryEmit(Unit)
    }
}
