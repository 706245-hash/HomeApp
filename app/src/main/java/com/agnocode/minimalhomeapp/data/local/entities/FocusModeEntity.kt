package com.agnocode.minimalhomeapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey val name: String,
    val startTime: Int? = null,
    val endTime: Int? = null
)
