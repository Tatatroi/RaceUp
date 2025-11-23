package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.raceup.app.firebase.FirebaseAuthManager

class MainActivity : AppCompatActivity() {

    private lateinit var logoutButton: Button
    private lateinit var welcomeTextView: TextView
    private lateinit var manageRacesButton: Button
    private val authManager = FirebaseAuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoutButton = findViewById(R.id.logoutButton)
        manageRacesButton = findViewById(R.id.manageRacesButton)
        welcomeTextView = findViewById(R.id.welcomeTextView)

        val currentUser = authManager.currentUser()
        val displayName = currentUser?.displayName ?: currentUser?.email ?: "User"
        welcomeTextView.text = "Welcome, $displayName!"

        logoutButton.setOnClickListener {
            authManager.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        manageRacesButton.setOnClickListener {
            startActivity(Intent(this, RaceListActivity::class.java))
        }

        findViewById<ImageButton>(R.id.imageButton5).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = authManager.currentUser()
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
