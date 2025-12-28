package com.raceup.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class RunTrackerActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvTimer: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvPace: TextView
    private lateinit var btnAction: Button

    // Data
    private var raceId: String? = null
    private var currentSeconds = 0L
    private var currentDistanceMeters = 0.0
    private var raceName: String = "Unknown Race"

    // State Flag
    private var isRunStarted = false

    // Broadcast Receiver to get data from background Service
    private val runUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                currentSeconds = intent.getLongExtra("SECONDS", 0)
                currentDistanceMeters = intent.getDoubleExtra("DISTANCE", 0.0)
                updateUI(currentSeconds, currentDistanceMeters)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_tracker)

        raceId = intent.getStringExtra("RACE_ID")

        if (raceId != null) {
            FirebaseFirestore.getInstance().collection("races").document(raceId!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        raceName = doc.getString("name") ?: "Virtual Race"
                        findViewById<TextView>(R.id.tvRaceTitle).text = raceName
                    }
                }
        }

        tvTimer = findViewById(R.id.tvTimer)
        tvDistance = findViewById(R.id.tvDistance)
        tvPace = findViewById(R.id.tvPace)

        // Make sure to match the ID in your XML (I changed it to btnAction)
        btnAction = findViewById(R.id.btnAction)

        // 1. CLICK LISTENER - Handles both Start and Stop
        btnAction.setOnClickListener {
            if (!isRunStarted) {
                // User wants to START
                checkPermissionsAndStart()
            } else {
                // User wants to FINISH
                stopRunAndSave()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Android 13+ needs Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        } else {
            // Permissions exist -> Start immediately
            startService()
        }
    }

    private fun startService() {
        // 1. Register Receiver
        val filter = IntentFilter("RACE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(runUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(runUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        }

        // 2. Start Service
        val intent = Intent(this, RunService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // 3. Update UI to "Running Mode"
        isRunStarted = true
        btnAction.text = "FINISH RACE"
        btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F")) // Red
    }

    private fun updateUI(seconds: Long, distanceMeters: Double) {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, secs)

        val km = distanceMeters / 1000.0
        tvDistance.text = String.format(Locale.US, "%.2f km", km)

        if (km > 0.05) {
            val totalMinutes = seconds / 60.0
            val pace = totalMinutes / km
            val paceMin = pace.toInt()
            val paceSec = ((pace - paceMin) * 60).toInt()
            tvPace.text = String.format("%02d:%02d", paceMin, paceSec)
        } else {
            tvPace.text = "--:--"
        }
    }

    private fun stopRunAndSave() {
        // 1. Stop the Service
        val intent = Intent(this, RunService::class.java)
        stopService(intent)

        try {
            unregisterReceiver(runUpdateReceiver)
        } catch (e: Exception) {
            // Ignore
        }

        btnAction.isEnabled = false
        btnAction.text = "SAVING..."

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: You are not logged in!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()

        val runData = hashMapOf(
            "raceId" to (raceId ?: "unknown"),
            "raceName" to raceName,
            "userId" to uid,
            "date" to java.util.Date(),
            "durationSeconds" to currentSeconds,
            "distanceMeters" to currentDistanceMeters,
            "avgPace" to tvPace.text.toString()
        )

        db.collection("users").document(uid).collection("runs")
            .add(runData)
            .addOnSuccessListener {
                Toast.makeText(this, "Run Saved Successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                // THIS IS THE IMPORTANT CHANGE: Show the actual error message
                Toast.makeText(this, "Save Failed: ${e.message}", Toast.LENGTH_LONG).show()

                // Re-enable button so you can try again if it was just a network blip
                btnAction.isEnabled = true
                btnAction.text = "RETRY SAVE"
            }
    }

    // Handle Permission Result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startService()
            } else {
                Toast.makeText(this, "Permissions needed to track run.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // If the user closes the app without finishing, we stop the service to avoid battery drain
        // (Or you can keep it running if you want background tracking to persist after close)
        if(isRunStarted) {
            val intent = Intent(this, RunService::class.java)
            stopService(intent)
        }

        try {
            unregisterReceiver(runUpdateReceiver)
        } catch (_: Exception) { }
    }
}