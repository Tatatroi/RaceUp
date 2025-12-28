package com.raceup.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

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

    // Map & Location Variables
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 1. Permission Launcher: Handles the user's response to "Allow Location?"
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission needed to find you", Toast.LENGTH_SHORT).show()
            }
        }

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

        // 2. Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fix ScrollView issue
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)
        mapContainer.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE ->
                    scrollView.requestDisallowInterceptTouchEvent(true)
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    scrollView.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Date Picker Logic
        raceDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    raceDateButton.text = selectedDate
                },
                year, month, day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        submitButton.setOnClickListener { submitRace() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 3. Click Listener: Add the pin ONLY when user clicks
        mMap.setOnMapClickListener { latLng ->
            mMap.clear() // Clear previous pins
            mMap.addMarker(MarkerOptions().position(latLng).title("Race Start"))
            selectedLocation = latLng // Save this for the database
        }

        // 4. Check Permissions & Enable Location
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted -> Turn it on
            enableUserLocation()
        } else {
            // Ask for permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableUserLocation() {
        // Double check permission before calling API (required by Android)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        // A. Show the Blue Dot
        mMap.isMyLocationEnabled = true

        // B. Get Last Known Location and Zoom there
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                // Move camera WITHOUT adding a marker
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
        }
    }

    private fun submitRace() {
        val name = raceNameEditText.text.toString().trim()
        val website = websiteEditText.text.toString().trim()
        val date = selectedDate ?: ""

        // Distances
        val distancesList = mutableListOf<String>()
        if (cb5k.isChecked) distancesList.add("5 km")
        if (cb10k.isChecked) distancesList.add("10 km")
        if (cbHalf.isChecked) distancesList.add("21.1 km")
        if (cbFull.isChecked) distancesList.add("42.2 km")
        if (customDistanceEditText.text.isNotEmpty()) distancesList.add(customDistanceEditText.text.toString())
        val finalDistanceString = distancesList.joinToString(", ")

        if (name.isEmpty() || date.isEmpty() || website.isEmpty()) {
            Toast.makeText(this, "Please fill name, date and website", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. Validation: Ensure user tapped the map
        if (selectedLocation == null) {
            Toast.makeText(this, "Please tap the map to mark the exact start location", Toast.LENGTH_SHORT).show()
            return
        }

        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val uniqueCode = (1..6)
            .map { allowedChars.random() }
            .joinToString("")

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("races").document()

        val raceData = hashMapOf(
            "id" to docRef.id,
            "name" to name,
            "date" to date,
            "distance" to finalDistanceString,
            "website" to website,
            "isApproved" to false,
            "latitude" to selectedLocation!!.latitude,
            "longitude" to selectedLocation!!.longitude,
            "raceCode" to uniqueCode
        )

        submitButton.isEnabled = false
        docRef.set(raceData)
            .addOnSuccessListener {
                Toast.makeText(this, "Race suggested!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                submitButton.isEnabled = true
            }
    }
}