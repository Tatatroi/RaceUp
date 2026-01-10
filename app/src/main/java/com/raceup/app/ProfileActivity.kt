package com.raceup.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private lateinit var imgProfile: ImageView
    private lateinit var textName: TextView
    private lateinit var textDetails: TextView
    private lateinit var recyclerHistory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imgProfile = findViewById(R.id.imgProfile)
        textName = findViewById(R.id.textProfileName)
        textDetails = findViewById(R.id.textProfileDetails)
        recyclerHistory = findViewById(R.id.recyclerProfileHistory)
        recyclerHistory.layoutManager = LinearLayoutManager(this)

        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)

        // 1. Click Image to Change it
        findViewById<androidx.cardview.widget.CardView>(R.id.cardProfileImage).setOnClickListener {
            pickImageFromGallery()
        }

        // 2. Click Edit Button (Edit Name/Birthdate)
        btnEditProfile.setOnClickListener {
            // You can implement a dialog here to edit text fields
            Toast.makeText(this, "Edit Name feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        loadUserData()
        loadRunHistory()
    }

    private fun loadUserData() {
        if (currentUser == null) return

        db.collection("users").document(currentUser.uid)
            .addSnapshotListener { document, e ->
                if (e != null || document == null) return@addSnapshotListener

                // 1. Name
                val first = document.getString("firstName") ?: ""
                val last = document.getString("lastName") ?: ""
                textName.text = "$first $last"

                // 2. Age (Calculated from birthDate string "12/11/2025")
                val birthDateStr = document.getString("birthDate") ?: ""
                val age = calculateAge(birthDateStr)
                val email = document.getString("email") ?: currentUser.email

                textDetails.text = "Age: $age  |  $email"

                // 3. Image (Load from URI string if available)
                val imagePath = document.getString("localImagePath")
                if (!imagePath.isNullOrEmpty()) {
                    try {
                        imgProfile.setImageURI(Uri.parse(imagePath))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun loadRunHistory() {
        if (currentUser == null) return

        db.collection("users").document(currentUser.uid).collection("runs")
            .get()
            .addOnSuccessListener { documents ->
                val runList = ArrayList<RunHistoryItem>()
                for (doc in documents) {
                    runList.add(RunHistoryItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown Race",
                        distance = doc.getString("distance") ?: "0 km",
                        date = doc.getString("date") ?: ""
                    ))
                }

                // Set Adapter
                recyclerHistory.adapter = ProfileRacesAdapter(runList) { selectedRun ->
                    // ON CLICK: Open Race Details
                    // Assuming you have a RaceDetailsActivity or want to show a Dialog
                    showRunDetailsDialog(selectedRun)
                }
            }
    }

    // Helper to calculate age from DD/MM/YYYY
    private fun calculateAge(dateStr: String): String {
        if (dateStr.isEmpty()) return "--"
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dateStr)
            val today = Calendar.getInstance()
            val dob = Calendar.getInstance()
            if (birthDate != null) dob.time = birthDate

            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age.toString()
        } catch (e: Exception) {
            "--"
        }
    }

    // --- IMAGE PICKER LOGIC ---
    private val PICK_IMAGE_CODE = 1001

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 1. Permission to read this URI in the future
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // 2. Save the URI string to Firestore
                if (currentUser != null) {
                    db.collection("users").document(currentUser.uid)
                        .update("localImagePath", uri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile Image Updated", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    // Simple Dialog to show details when a race is clicked
    private fun showRunDetailsDialog(run: RunHistoryItem) {
        AlertDialog.Builder(this)
            .setTitle(run.name)
            .setMessage("Date: ${run.date}\nDistance: ${run.distance}")
            .setPositiveButton("Close", null)
            .show()
    }
}