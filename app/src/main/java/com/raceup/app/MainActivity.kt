package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.raceup.app.firebase.FirebaseAuthManager

class MainActivity : AppCompatActivity() {

    private lateinit var logoutButton: ImageButton
    private lateinit var welcomeTextView: TextView

    // Dashboard Cards
    private lateinit var cardCalendar: CardView
    private lateinit var cardFavorites: CardView
    private lateinit var cardSuggest: CardView
    private lateinit var cardAdmin: CardView

    private val authManager = FirebaseAuthManager()

    // Replace this with your actual Admin email
    val ADMIN_EMAIL = "mihai@vaidos.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Views
        logoutButton = findViewById(R.id.logoutButton)
        welcomeTextView = findViewById(R.id.welcomeTextView)

        cardCalendar = findViewById(R.id.cardCalendar)
        cardFavorites = findViewById(R.id.cardFavorites)
        cardSuggest = findViewById(R.id.cardSuggest)
        cardAdmin = findViewById(R.id.cardAdmin)

        // 2. Setup User Info
        val currentUser = authManager.currentUser()
        val displayName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Guest"
        welcomeTextView.text = "Welcome,\n$displayName"

        // 3. LOGOUT LOGIC
        logoutButton.setOnClickListener {
            authManager.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 4. CLICK LISTENERS

        // Calendar (Open for everyone)
        cardCalendar.setOnClickListener {
            startActivity(Intent(this, RaceListActivity::class.java))
        }

        // Suggest Race (Hidden for Guests?)
        // Let's allow guests to click but maybe redirect to login or just block it
        if (currentUser == null) {
            cardSuggest.visibility = View.GONE
        } else {
            cardSuggest.setOnClickListener {
                startActivity(Intent(this, RaceFormActivity::class.java))
            }
        }

        // Favorites (Hidden for Guests)
        if (currentUser == null) {
            cardFavorites.visibility = View.GONE
        } else {
            cardFavorites.visibility = View.VISIBLE
            cardFavorites.setOnClickListener {
                startActivity(Intent(this, FavoritesActivity::class.java))
            }
        }

        // 5. ADMIN LOGIC
        if (currentUser?.email == ADMIN_EMAIL) {
            cardAdmin.visibility = View.VISIBLE
            cardAdmin.setOnClickListener {
                val intent = Intent(this, RaceListActivity::class.java)
                intent.putExtra("SHOW_PENDING", true)
                startActivity(intent)
            }
        } else {
            cardAdmin.visibility = View.GONE
        }
    }
}