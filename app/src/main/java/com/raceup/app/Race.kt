package com.raceup.app
import com.google.firebase.firestore.PropertyName

data class Race(
    var id: String = "",
    var name: String = "",
    var date: String = "",
    var distance: String = "",
    var website: String = "",

    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false
)
