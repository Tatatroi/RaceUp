package com.raceup.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.cardview.widget.CardView

class RaceDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var raceId: String? = null

    // UI Elements
    private lateinit var nameText: TextView
    private lateinit var dateText: TextView
    private lateinit var distanceText: TextView
    private lateinit var websiteText: TextView
    private lateinit var btnFavorite: FloatingActionButton
    private lateinit var adminPanel: LinearLayout
    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button

    private lateinit var mapCard: CardView
    private var raceLat: Double = 0.0
    private var raceLng: Double = 0.0
    private var mapReady: GoogleMap? = null

    // ADMIN CONFIGURATION
    private val ADMIN_EMAIL = "mihai@vaidos.com" // <--- CHANGE THIS!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_details)

        // 1. Initialize Views
        nameText = findViewById(R.id.detailRaceName)
        dateText = findViewById(R.id.detailRaceDate)
        distanceText = findViewById(R.id.detailRaceDistance)
        websiteText = findViewById(R.id.detailRaceWebsite)
        btnFavorite = findViewById(R.id.btnFavorite)
        adminPanel = findViewById(R.id.adminPanel)
        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)
        mapCard = findViewById(R.id.mapCard)

        // 2. Get Race ID
        raceId = intent.getStringExtra("raceId")
        if (raceId == null) {
            Toast.makeText(this, "Error: No race loaded", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.detailsMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 3. Load Data
        loadRaceDetails()

        // 4. Setup User vs Admin vs Guest Logic
        setupPermissions()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapReady = googleMap
        // If data loaded first, update the map now
        updateMapLocation()
    }

    private fun setupPermissions() {
        val user = auth.currentUser

        // CASE A: Guest (Not logged in)
        if (user == null) {
            btnFavorite.visibility = View.GONE
            adminPanel.visibility = View.GONE
        }
        // CASE B: Admin
        else if (user.email == ADMIN_EMAIL) {
            btnFavorite.visibility = View.VISIBLE // Admins can also favorite
            adminPanel.visibility = View.VISIBLE  // Show Approve/Reject buttons

            // Wire up Admin Buttons
            btnApprove.setOnClickListener { approveRace() }
            btnReject.setOnClickListener { rejectRace() }
            btnFavorite.setOnClickListener { toggleFavorite() }
        }
        // CASE C: Normal User
        else {
            btnFavorite.visibility = View.VISIBLE
            adminPanel.visibility = View.GONE // Hide Admin tools
            btnFavorite.setOnClickListener { toggleFavorite() }
        }
    }

    private fun loadRaceDetails() {
        db.collection("races").document(raceId!!)
            .get()
            .addOnSuccessListener { doc ->
                val race = doc.toObject(Race::class.java)

                if (race != null) {
                    // 1. Set text fields
                    nameText.text = race.name
                    dateText.text = race.date
                    distanceText.text = race.distance
                    websiteText.text = race.website
                    raceLat = race.latitude
                    raceLng = race.longitude

                    updateMapLocation()

                    // 2. ADMIN LOGIC: Check status to show/hide buttons
                    val user = auth.currentUser
                    if (user?.email == ADMIN_EMAIL) {
                        // We always show the panel for admins now
                        adminPanel.visibility = View.VISIBLE

                        if (race.isApproved) {
                            // CASE: Race is LIVE.
                            // Hide "Approve" (it's already done). Show "Delete".
                            btnApprove.visibility = View.GONE
                            btnReject.visibility = View.VISIBLE
                            btnReject.text = "Delete Race"

                            // Important: Update the click listener to warn the user
                            btnReject.setOnClickListener {
                                // Optional: You could add an Alert Dialog here to confirm
                                deleteRace()
                            }
                        } else {
                            // CASE: Race is PENDING.
                            // Show both buttons.
                            btnApprove.visibility = View.VISIBLE
                            btnReject.visibility = View.VISIBLE
                            btnReject.text = "Reject Request"

                            btnApprove.setOnClickListener { approveRace() }
                            btnReject.setOnClickListener { deleteRace() }
                        }
                    } else {
                        // Normal user
                        adminPanel.visibility = View.GONE
                    }
                }
            }
    }

    private fun updateMapLocation() {
        if (mapReady != null && raceLat != 0.0 && raceLng != 0.0) {
            mapCard.visibility = View.VISIBLE

            val location = LatLng(raceLat, raceLng)
            mapReady?.clear()
            mapReady?.addMarker(MarkerOptions().position(location).title("Race Start"))
            mapReady?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))

            // Enable basic controls
            mapReady?.uiSettings?.isZoomControlsEnabled = true
        } else {
            // Hide map if no location data exists (e.g. old races)
            mapCard.visibility = View.GONE
        }
    }

    private fun deleteRace() {
        db.collection("races").document(raceId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Race Deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun toggleFavorite() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid).collection("favorites").document(raceId!!)

        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                ref.delete()
                Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                val data = hashMapOf("raceId" to raceId, "timestamp" to System.currentTimeMillis())
                ref.set(data)
                Toast.makeText(this, "Added to Favorites!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun approveRace() {
        db.collection("races").document(raceId!!)
            .update("isApproved", true)
            .addOnSuccessListener {
                Toast.makeText(this, "Race Approved & Live!", Toast.LENGTH_SHORT).show()
                finish() // Close screen
            }
    }

    private fun rejectRace() {
        // In a real app, you might want a "Are you sure?" dialog here
        db.collection("races").document(raceId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Race Deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}