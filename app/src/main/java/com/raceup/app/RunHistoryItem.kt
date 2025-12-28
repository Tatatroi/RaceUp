package com.raceup.app

data class RunHistoryItem(
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val avgPace: String = "",
    val date: java.util.Date? = null
)