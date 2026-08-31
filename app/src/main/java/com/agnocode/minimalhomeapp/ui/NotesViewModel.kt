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

    private val allDailyNotes = mutableStateMapOf<String, DailyNote>()
    var selectedNoteDate = mutableStateOf(dateFmt.format(Date()))
    var currentNoteText = mutableStateOf("")
    val currentTasks = mutableStateListOf<NoteTask>()

    val dailyNotesFlow: StateFlow<Map<String, DailyNote>> = repository.dailyNotesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    init {
        collectDailyNotes()
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
            repository.saveDailyNote(note)
        }
    }

    fun getAvailableDates(): List<String> {
        val today = dateFmt.format(Date())
        return (allDailyNotes.keys + today)
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinct()
            .sortedDescending()
    }
}
