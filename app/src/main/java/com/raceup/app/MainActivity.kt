package com.raceup.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.raceup.app.firebase.FirebaseAuthManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val testMail = "test123@gmail.com"
        val testPassword = "abcd1234$"

        FirebaseAuthManager().registerUser(testMail, testPassword) { success, errorMessage ->
            if(success){
                Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Failed: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }

    }
}

