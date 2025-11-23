package com.raceup.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RaceDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var nameText: TextView
    private lateinit var dateText: TextView
    private lateinit var distanceText: TextView
    private lateinit var websiteText: TextView
    private lateinit var favoriteButton: Button

    private var raceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_details)

        nameText = findViewById(R.id.detailRaceName)
        dateText = findViewById(R.id.detailRaceDate)
        distanceText = findViewById(R.id.detailRaceDistance)
        websiteText = findViewById(R.id.detailRaceWebsite)
        favoriteButton = findViewById(R.id.addFavoriteButton)

        raceId = intent.getStringExtra("raceId")

        if (raceId == null) {
            Toast.makeText(this, "Error: No race ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadRaceDetails()
        favoriteButton.setOnClickListener { addToFavorites() }
    }

    private fun loadRaceDetails() {
        db.collection("races").document(raceId!!)
            .get()
            .addOnSuccessListener { doc ->
                val race = doc.toObject(Race::class.java)
                if (race != null) {
                    nameText.text = race.name
                    dateText.text = race.date
                    distanceText.text = race.distance
                    websiteText.text = race.website
                }
            }
    }

    private fun addToFavorites() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("favorites")
            .document(raceId!!)
            .set(mapOf("favorite" to true))
            .addOnSuccessListener {
                Toast.makeText(this, "Added to favorites!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
