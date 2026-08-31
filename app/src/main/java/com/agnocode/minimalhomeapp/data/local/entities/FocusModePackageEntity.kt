package com.agnocode.minimalhomeapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "focus_mode_packages",
    primaryKeys = ["modeName", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = FocusModeEntity::class,
            parentColumns = ["name"],
            childColumns = ["modeName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("modeName")]
)
data class FocusModePackageEntity(
    val modeName: String,
    val packageName: String
)
