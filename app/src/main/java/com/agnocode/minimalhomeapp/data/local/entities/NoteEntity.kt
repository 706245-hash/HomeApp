package com.agnocode.minimalhomeapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val content: String
)
