package com.agnocode.minimalhomeapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["date"],
            childColumns = ["noteDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteDate")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val noteDate: String,
    val text: String,
    val isChecked: Boolean,
    val order: Int = 0 // To maintain UI order
)
