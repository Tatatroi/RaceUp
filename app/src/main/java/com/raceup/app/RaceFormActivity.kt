package com.raceup.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class RaceFormActivity : AppCompatActivity(), OnMapReadyCallback {

    // UI Elements
    private lateinit var raceNameEditText: EditText
    private lateinit var websiteEditText: EditText
    private lateinit var raceDateButton: Button
    private lateinit var submitButton: Button

    // Checkboxes
    private lateinit var cb5k: CheckBox
    private lateinit var cb10k: CheckBox
    private lateinit var cbHalf: CheckBox
    private lateinit var cbFull: CheckBox
    private lateinit var customDistanceEditText: EditText

    private var selectedDate: String? = null

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_form)

        // Initialize Views
        raceNameEditText = findViewById(R.id.raceNameEditText)
        websiteEditText = findViewById(R.id.websiteEditText)
        raceDateButton = findViewById(R.id.raceDateButton)
        submitButton = findViewById(R.id.submitButton)

        cb5k = findViewById(R.id.cb5k)
        cb10k = findViewById(R.id.cb10k)
        cbHalf = findViewById(R.id.cbHalf)
        cbFull = findViewById(R.id.cbFull)
        customDistanceEditText = findViewById(R.id.customDistanceEditText)

        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)

        mapContainer.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> {
                    // User is touching the map -> Disable page scrolling
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    // User let go -> Re-enable page scrolling
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Return false so the event still goes to the Map
        }

        // Date Picker Logic
        raceDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    // Formatting date nicely
                    selectedDate = "$day/${month + 1}/$year"
                    raceDateButton.text = "Date: $selectedDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Submit Logic
        submitButton.setOnClickListener {
            submitRace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Default View (Example: Center of Europe or User Location)
        val defaultLocation = LatLng(51.1657, 10.4515) // Germany center
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))

        mMap.setOnMapClickListener { latLng ->
            // Clear old marker and add new one
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Race Start"))
            selectedLocation = latLng
        }
    }

    private fun submitRace() {
        val name = raceNameEditText.text.toString().trim()
        val website = websiteEditText.text.toString().trim()
        val date = selectedDate ?: ""

        // 1. COLLECT DISTANCES
        val distancesList = mutableListOf<String>()
        if (cb5k.isChecked) distancesList.add("5 km")
        if (cb10k.isChecked) distancesList.add("10 km")
        if (cbHalf.isChecked) distancesList.add("21.1 km")
        if (cbFull.isChecked) distancesList.add("42.2 km")

        val customText = customDistanceEditText.text.toString().trim()
        if (customText.isNotEmpty()) {
            distancesList.add(customText)
        }

        // Join them into one string (e.g., "10 km, 21.1 km, 27km Trail")
        val finalDistanceString = distancesList.joinToString(", ")

        // 2. VALIDATION
        if (name.isEmpty() || date.isEmpty() || website.isEmpty()) {
            Toast.makeText(this, "Please fill name, date and website", Toast.LENGTH_SHORT).show()
            return
        }
        if (finalDistanceString.isEmpty()) {
            Toast.makeText(this, "Please select at least one distance", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedLocation == null) {
            Toast.makeText(this, "Please tap the map to select a start location", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. SEND TO FIRESTORE
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("races").document()

        val raceData = hashMapOf(
            "id" to docRef.id,
            "name" to name,
            "date" to date,
            "distance" to finalDistanceString, // Stores multiple distances now
            "website" to website,
            "isApproved" to false, // IMPORTANT: Boolean (not String "false")
            "latitude" to selectedLocation!!.latitude,
            "longitude" to selectedLocation!!.longitude
        )

        submitButton.isEnabled = false // Prevent double clicking

        docRef.set(raceData)
            .addOnSuccessListener {
                Toast.makeText(this, "Race suggested! Waiting for admin approval.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                submitButton.isEnabled = true
            }
    }
}