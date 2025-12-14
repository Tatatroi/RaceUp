package com.raceup.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RaceFormActivity : AppCompatActivity() {

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

        // Submit Logic
        submitButton.setOnClickListener {
            submitRace()
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

        // 3. SEND TO FIRESTORE
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("races").document()

        val raceData = hashMapOf(
            "id" to docRef.id,
            "name" to name,
            "date" to date,
            "distance" to finalDistanceString, // Stores multiple distances now
            "website" to website,
            "isApproved" to false // IMPORTANT: Boolean (not String "false")
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