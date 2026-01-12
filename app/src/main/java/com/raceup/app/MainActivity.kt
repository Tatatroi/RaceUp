package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore
import com.raceup.app.firebase.FirebaseAuthManager
import android.widget.ImageView
class MainActivity : AppCompatActivity() {

    private lateinit var logoutButton: ImageButton
    private lateinit var welcomeTextView: TextView

    private lateinit var cardCalendar: CardView
    private lateinit var cardFavorites: CardView
    private lateinit var cardSuggest: CardView
    private lateinit var cardAdmin: CardView
    private lateinit var cardMap: CardView
    private lateinit var cardGroups: CardView
    private lateinit var cardJoinRace: CardView
    private lateinit var cardHistory: CardView


    private val authManager = FirebaseAuthManager()

    val ADMIN_EMAIL = "mihai@vaidos.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logoutButton: View = findViewById(R.id.logoutButton)
        welcomeTextView = findViewById(R.id.welcomeTextView)
        val userEmailText: TextView = findViewById(R.id.userEmailText)

        cardCalendar = findViewById(R.id.cardCalendar)
        cardFavorites = findViewById(R.id.cardFavorites)
        cardSuggest = findViewById(R.id.cardSuggest)
        cardAdmin = findViewById(R.id.cardAdmin)
        cardMap = findViewById(R.id.cardMap)
        cardJoinRace = findViewById(R.id.cardJoinRace)
        cardHistory = findViewById(R.id.cardHistory)
        cardGroups = findViewById(R.id.cardGroups)


        val currentUser = authManager.currentUser()

    val btnProfile = findViewById<View>(R.id.btnProfile)
    btnProfile.setOnClickListener {
        if (authManager.currentUser() != null) {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Please log in to view profile", Toast.LENGTH_SHORT).show()
        }
    }

    if (currentUser != null) {
        userEmailText.text = currentUser.email
        welcomeTextView.text = "Welcome, \n"

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")

                    if (!firstName.isNullOrEmpty()) {
                        welcomeTextView.text = "Welcome,\n$firstName $lastName"
                    }

                    val localPath = document.getString("localImagePath")
                    if (!localPath.isNullOrEmpty()) {
                        val imgProfileIcon = findViewById<ImageView>(R.id.imgProfileIcon)
                        try {
                            imgProfileIcon.setImageURI(android.net.Uri.parse(localPath))
                            imgProfileIcon.imageTintList = null
                            imgProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                            imgProfileIcon.setPadding(0,0,0,0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    } else {
        welcomeTextView.text = "Welcome,\nGuest"
    }

        logoutButton.setOnClickListener {
            authManager.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        cardMap.setOnClickListener {
            startActivity(Intent(this, MapExplorerActivity::class.java))
        }

        cardCalendar.setOnClickListener {
            startActivity(Intent(this, RaceListActivity::class.java))
        }

        if (currentUser == null) {
            cardSuggest.visibility = View.GONE
        } else {
            cardSuggest.setOnClickListener {
                startActivity(Intent(this, RaceFormActivity::class.java))
            }
        }

        if (currentUser == null) {
            cardFavorites.visibility = View.GONE
        } else {
            cardFavorites.visibility = View.VISIBLE
            cardFavorites.setOnClickListener {
                startActivity(Intent(this, FavoritesActivity::class.java))
            }
        }

        if (currentUser == null) {
            cardGroups.visibility = View.GONE
        } else {
            cardGroups.visibility = View.VISIBLE
            cardGroups.setOnClickListener {
                startActivity(Intent(this, GroupsActivity::class.java))
            }
        }

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

        cardJoinRace.setOnClickListener {
            val intent = Intent(this, RaceLobbyActivity::class.java)
            startActivity(intent)
        }

        cardHistory.setOnClickListener {
            val intent = Intent(this, RunHistoryActivity::class.java)
            startActivity(intent)
        }
    }
}