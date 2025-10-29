package com.raceup.app.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.util.Log



class FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // create new user
    fun registerUser(email: String, password: String,onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
            if (task.isSuccessful){
                Log.d("FirebaseAuth", "User created: ${auth.currentUser?.email}")
                onResult(true, null)
            }
                else{
                Log.e("FirebaseAuth", "Failed: ${task.exception?.message}")
                onResult(false, task.exception?.message)
            }
    }
    }
}
