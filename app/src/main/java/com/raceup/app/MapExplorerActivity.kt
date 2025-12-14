package com.raceup.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
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

class MapExplorerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Permission Launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission needed to show your position", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_explorer)

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.explorerMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 1. Load the races (Red Pins)
        loadRacesOnMap()

        // 2. Check Permission & Zoom to User (Blue Dot)
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        // Turn on the Blue Dot
        mMap.isMyLocationEnabled = true

        // Get last known location and zoom there
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f)) // Zoom level 10 is good for "City/Region" view
            }
        }
    }

    private fun loadRacesOnMap() {
        db.collection("races")
            .whereEqualTo("isApproved", true)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val race = document.toObject(Race::class.java)
                    race.id = document.id

                    if (race.latitude != 0.0 && race.longitude != 0.0) {
                        val position = LatLng(race.latitude, race.longitude)
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(race.name)
                                .snippet(race.date)
                        )
                        marker?.tag = race.id
                    }
                }

                mMap.setOnInfoWindowClickListener { marker ->
                    val raceId = marker.tag as? String
                    if (raceId != null) {
                        val intent = Intent(this, RaceDetailsActivity::class.java)
                        intent.putExtra("raceId", raceId)
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not load races", Toast.LENGTH_SHORT).show()
            }
    }
}