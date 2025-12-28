package com.raceup.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.raceup.app.BuildConfig

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

    private lateinit var weatherTempText: TextView
    private lateinit var weatherDescText: TextView
    private lateinit var runScoreText: TextView
    private lateinit var scoreCard: CardView

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
        weatherTempText = findViewById(R.id.weatherTempText)
        weatherDescText = findViewById(R.id.weatherDescText)
        runScoreText = findViewById(R.id.runScoreText)
        scoreCard = findViewById(R.id.scoreCard)

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

                    // NEW: Fetch Weather if we have coordinates
                    if (raceLat != 0.0 && raceLng != 0.0) {
                        fetchWeather(raceLat, raceLng)
                    } else {
                        // Handle case with no location data
                        weatherDescText.text = "No location data"
                        runScoreText.text = "N/A"
                    }

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
                // CASE: Already a favorite -> Remove it
                ref.delete()
                Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                // Optional: You could cancel alarms here if you wanted to implement cancel logic later
            } else {
                // CASE: Not a favorite -> Add it
                val data = hashMapOf("raceId" to raceId, "timestamp" to System.currentTimeMillis())
                ref.set(data)
                Toast.makeText(this, "Added to Favorites!", Toast.LENGTH_SHORT).show()

                // --- NEW CODE: SCHEDULE NOTIFICATIONS ---
                // We create a temporary Race object using the text currently on screen
                val currentRace = Race(
                    id = raceId!!,
                    name = nameText.text.toString(),
                    date = dateText.text.toString()
                )

                scheduleNotifications(currentRace)
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        // Use a Coroutine to fetch data in the background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = WeatherNetwork.api.getCurrentWeather(lat, lon, "metric", BuildConfig.WEATHER_API_KEY)

                // Switch back to Main Thread to update UI
                withContext(Dispatchers.Main) {
                    updateWeatherUI(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    weatherDescText.text = "Weather unavailable"
                }
            }
        }
    }

    private fun updateWeatherUI(weather: WeatherResponse) {
        val temp = weather.main.temp
        val condition = weather.weather.firstOrNull()?.main ?: "Clear"

        weatherTempText.text = "${temp.toInt()}°C"
        weatherDescText.text = condition

        // --- RUN SCORE LOGIC ---
        var score = "MEDIUM"
        var color = Color.parseColor("#FF9800") // Orange

        // 1. Temperature Check
        val isTempGood = temp in 8.0..20.0
        val isTempOk = temp in 5.0..25.0

        // 2. Condition Check
        val isRaining = condition.contains("Rain", ignoreCase = true)
        val isSnowing = condition.contains("Snow", ignoreCase = true)

        if (isTempGood && !isRaining && !isSnowing) {
            score = "EXCELLENT"
            color = Color.parseColor("#4CAF50") // Green
        } else if (isTempOk && !isSnowing) {
            score = "GOOD"
            color = Color.parseColor("#8BC34A") // Light Green
        } else if (temp > 28.0 || temp < 0.0 || isSnowing) {
            score = "POOR"
            color = Color.parseColor("#F44336") // Red
        } else {
            score = "MEDIUM" // Default
        }

        runScoreText.text = "RUN LEVEL: $score"
        runScoreText.setTextColor(Color.WHITE)
        scoreCard.setCardBackgroundColor(color)
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

    private fun scheduleNotifications(race: Race) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Parse the Race Date
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val date = sdf.parse(race.date) ?: return

        val calendar = Calendar.getInstance()
        calendar.time = date

        // --- NOTIFICATION 1: Race Day (8:00 AM) ---
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        // Schedule if date hasn't passed
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            scheduleSingleNotification(
                alarmManager,
                calendar.timeInMillis,
                "Good Luck Today! 🏃",
                "Today is the big day: ${race.name}. You got this!",
                race.id,
                1 // Unique request code
            )
        }

        // --- NOTIFICATION 2: 3 Days Before (10:00 AM) ---
        calendar.add(Calendar.DAY_OF_YEAR, -3) // Subtract 3 days
        calendar.set(Calendar.HOUR_OF_DAY, 10) // Set to 10 AM

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            scheduleSingleNotification(
                alarmManager,
                calendar.timeInMillis,
                "3 Days to Go! ⏳",
                "Your race ${race.name} is coming up in 3 days.",
                race.id,
                2 // Different request code
            )
        }

        val testTime = System.currentTimeMillis() + 10000 // 10 seconds
        scheduleSingleNotification(alarmManager, testTime, "Test Title", "Test Message", race.id, 99)

        Toast.makeText(this, "Reminders set for 3 days before and race day.", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleSingleNotification(
        alarmManager: AlarmManager,
        timeInMillis: Long,
        title: String,
        message: String,
        raceId: String,
        requestCode: Int
    ) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("raceId", raceId)
        }

        // Unique ID is essential so different reminders don't overwrite each other
        val uniqueId = raceId.hashCode() + requestCode

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "setExact" means the system executes it at that precise time
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    // Fallback for no permission: inexact timing (good enough for reminders)
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // If app lacks permission, just use standard set (might be a few minutes off, which is fine)
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }
}