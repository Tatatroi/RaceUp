package com.raceup.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RaceFormActivity : AppCompatActivity() {

    private lateinit var raceNameEditText: EditText
    private lateinit var raceDateButton: Button
    private lateinit var distanceSpinner: Spinner
    private lateinit var customDistanceEditText: EditText
    private lateinit var websiteEditText: EditText
    private lateinit var submitButton: Button

    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_form)

        raceNameEditText = findViewById(R.id.raceNameEditText)
        raceDateButton = findViewById(R.id.raceDateButton)
        distanceSpinner = findViewById(R.id.distanceSpinner)
        customDistanceEditText = findViewById(R.id.customDistanceEditText)
        websiteEditText = findViewById(R.id.websiteEditText)
        submitButton = findViewById(R.id.submitButton)

        // Distance options
        val distances = listOf("Marathon (42km)", "Half Marathon (21km)", "10km", "Custom")
        distanceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, distances)

        // Show custom field only if "Custom" selected
        distanceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                customDistanceEditText.visibility =
                    if (distances[position] == "Custom") android.view.View.VISIBLE else android.view.View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Pick date
        raceDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = "$day/${month + 1}/$year"
                    raceDateButton.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Submit form
        submitButton.setOnClickListener {
            val name = raceNameEditText.text.toString()
            val date = selectedDate ?: ""
            val distance = if (distanceSpinner.selectedItem == "Custom")
                customDistanceEditText.text.toString()
            else
                distanceSpinner.selectedItem.toString()
            val website = websiteEditText.text.toString()

            if (name.isEmpty() || date.isEmpty() || distance.isEmpty() || website.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("races").document()
            val raceData = hashMapOf(
                "id" to docRef.id,
                "name" to name,
                "date" to date,
                "distance" to distance,
                "website" to website
            )

            docRef.set(raceData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Race saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
