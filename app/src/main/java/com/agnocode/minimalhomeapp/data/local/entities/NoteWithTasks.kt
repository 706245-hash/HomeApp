package com.agnocode.minimalhomeapp.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithTasks(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "date",
        entityColumn = "noteDate"
    )
    val tasks: List<TaskEntity>
)
