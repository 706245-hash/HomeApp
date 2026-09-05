package com.agnocode.minimalhomeapp.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey 
    @SerializedName("date")
    val date: String, // yyyy-MM-dd
    
    @SerializedName("content")
    val content: String
)
