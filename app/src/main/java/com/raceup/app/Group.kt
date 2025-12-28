package com.raceup.app

data class Group(
    var id: String = "",
    var name: String = "",
    var createdBy: String = "",
    var members: List<String> = emptyList(),
    // NEW FIELD: Stores time in milliseconds (Long)
    var createdAt: Long = System.currentTimeMillis()
)