package com.raceup.app.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.auth.FirebaseUser

class FirebaseFirestoreManager {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    fun saveUserExtraData(userId: String, firstName: String, lastName: String, birthDate: String, gender: String, localImagePath: String) {

        val userMap = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "birthDate" to birthDate,
            "gender" to gender,
            "email" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
            "localImagePath" to localImagePath
        )

        db.collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                // Log: Date salvate cu succes
            }
            .addOnFailureListener { e ->
                // Log: Eroare la salvare
            }
    }
}
