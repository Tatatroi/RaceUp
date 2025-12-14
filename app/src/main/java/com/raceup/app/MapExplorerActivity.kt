package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapExplorerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_explorer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.explorerMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Optional: Center map on a default location (e.g. Europe)
        val defaultLoc = LatLng(50.0, 10.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 4f))

        // Load all races
        loadRacesOnMap()
    }

    private fun loadRacesOnMap() {
        db.collection("races")
            .whereEqualTo("isApproved", true) // Only show approved races
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val race = document.toObject(Race::class.java)
                    race.id = document.id

                    // Only add pin if it has valid coordinates
                    if (race.latitude != 0.0 && race.longitude != 0.0) {
                        val position = LatLng(race.latitude, race.longitude)

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(race.name)
                                .snippet(race.date) // Shows date when clicked
                        )
                        // Store the Race ID in the marker so we know which one was clicked
                        marker?.tag = race.id
                    }
                }

                // Handle clicking the "Info Window" (the bubble that pops up)
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