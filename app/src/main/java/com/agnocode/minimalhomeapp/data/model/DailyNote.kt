package com.agnocode.minimalhomeapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DailyNote(
    @SerializedName("date")
    val date: String, // yyyy-MM-dd
    
    @SerializedName("content")
    val content: String = "",
    
    @SerializedName("tasks")
    val tasks: List<NoteTask> = emptyList()
)

@Keep
data class NoteTask(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("isChecked")
    val isChecked: Boolean = false
)
