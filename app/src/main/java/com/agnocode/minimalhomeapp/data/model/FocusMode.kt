package com.agnocode.minimalhomeapp.data.model

data class FocusMode(
    val name: String,
    val allowedPackages: Set<String>,
    val startTime: Int? = null, // Minutes from midnight
    val endTime: Int? = null    // Minutes from midnight
)
