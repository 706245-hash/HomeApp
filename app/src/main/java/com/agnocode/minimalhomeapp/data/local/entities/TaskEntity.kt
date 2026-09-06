package com.agnocode.minimalhomeapp.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
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
    @PrimaryKey 
    @SerializedName("id")
    val id: String,
    
    @SerializedName("noteDate")
    val noteDate: String,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("isChecked")
    val isChecked: Boolean,
    
    @SerializedName("recurringDays")
    val recurringDays: String? = null,
    
    @SerializedName("order")
    val order: Int = 0 // To maintain UI order
)
