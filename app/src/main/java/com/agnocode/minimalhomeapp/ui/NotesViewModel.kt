package com.agnocode.minimalhomeapp.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agnocode.minimalhomeapp.data.AppRepository
import com.agnocode.minimalhomeapp.data.model.DailyNote
import com.agnocode.minimalhomeapp.data.model.NoteTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // allDailyNotes stores the last known state from the database
    private val allDailyNotes = mutableStateMapOf<String, DailyNote>()
    
    // UI state
    var selectedNoteDate = mutableStateOf(dateFmt.format(Date()))
    var currentNoteText = mutableStateOf("")
    val currentTasks = mutableStateListOf<NoteTask>()

    private var saveJob: Job? = null
    private var initialLoadDone = false

    init {
        collectDailyNotes()
    }

    private fun collectDailyNotes() {
        viewModelScope.launch {
            repository.dailyNotesFlow.collect { map ->
                allDailyNotes.clear()
                allDailyNotes.putAll(map)
                
                // Direct collection ensures we don't get a premature "empty" emission.
                // We load the selected note into the UI only once at startup.
                if (!initialLoadDone) {
                    loadSelectedNote()
                    initialLoadDone = true
                }
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
        // Immediate save of previous note before switching
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            saveCurrentNoteImmediate()
        }
        
        selectedNoteDate.value = date
        loadSelectedNote()
    }

    fun updateNoteText(text: String) {
        if (currentNoteText.value == text) return
        currentNoteText.value = text
        triggerDebouncedSave()
    }

    fun addTask() {
        val newTask = NoteTask(UUID.randomUUID().toString(), "")
        currentTasks.add(0, newTask) // Add to top
        triggerDebouncedSave() // Debounce even for adds to avoid rapid DB writes if user adds many
    }

    fun updateTaskText(id: String, text: String) {
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            if (currentTasks[index].text == text) return
            currentTasks[index] = currentTasks[index].copy(text = text)
            triggerDebouncedSave()
        }
    }

    fun toggleTask(id: String, checked: Boolean) {
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            currentTasks[index] = currentTasks[index].copy(isChecked = checked)
            triggerDebouncedSave() // Use debounce for consistency, but 500ms is fast enough
        }
    }

    fun deleteTask(id: String) {
        currentTasks.removeAll { it.id == id }
        triggerDebouncedSave()
    }

    private fun triggerDebouncedSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            saveCurrentNoteInternal()
        }
    }

    private fun saveCurrentNoteImmediate() {
        viewModelScope.launch {
            saveCurrentNoteInternal()
        }
    }

    private suspend fun saveCurrentNoteInternal() {
        val note = DailyNote(
            date = selectedNoteDate.value,
            content = currentNoteText.value,
            tasks = currentTasks.toList()
        )
        // Update local cache to match what we are sending to DB
        allDailyNotes[selectedNoteDate.value] = note
        repository.saveDailyNote(note)
    }

    fun getAvailableDates(): List<String> {
        val today = dateFmt.format(Date())
        return (allDailyNotes.keys + today)
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinct()
            .sortedDescending()
    }
}
