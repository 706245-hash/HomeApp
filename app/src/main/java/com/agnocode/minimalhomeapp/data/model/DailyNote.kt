package com.agnocode.minimalhomeapp.data.model

data class DailyNote(
    val date: String, // yyyy-MM-dd
    val content: String = "",
    val tasks: List<NoteTask> = emptyList()
)

data class NoteTask(
    val id: String,
    val text: String,
    val isChecked: Boolean = false
)
