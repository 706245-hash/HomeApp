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

    private val _resetToHomeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetToHomeEvent = _resetToHomeEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.checkAndPerformMigration()
        }
    }

    fun triggerResetToHome() {
        _resetToHomeEvent.tryEmit(Unit)
    }
}
