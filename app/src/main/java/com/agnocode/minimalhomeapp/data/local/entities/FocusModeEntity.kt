package com.agnocode.minimalhomeapp.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey 
    @SerializedName("name")
    val name: String,
    
    @SerializedName("startTime")
    val startTime: Int? = null,
    
    @SerializedName("endTime")
    val endTime: Int? = null
)
