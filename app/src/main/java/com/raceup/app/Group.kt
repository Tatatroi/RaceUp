package com.raceup.app

data class Group(
    var id: String = "",
    var name: String = "",
    var createdBy: String = "",
    var members: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
)