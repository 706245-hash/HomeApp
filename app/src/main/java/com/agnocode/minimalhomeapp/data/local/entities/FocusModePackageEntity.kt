package com.agnocode.minimalhomeapp.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.gson.annotations.SerializedName

@Keep
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
    @SerializedName("modeName")
    val modeName: String,
    
    @SerializedName("packageName")
    val packageName: String
)
