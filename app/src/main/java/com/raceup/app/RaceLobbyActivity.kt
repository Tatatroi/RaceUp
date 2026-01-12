package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class RaceLobbyActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_lobby)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val codeEditText = findViewById<EditText>(R.id.codeEditText)
        val btnStartRun = findViewById<Button>(R.id.btnStartRun)

        btnBack.setOnClickListener {
            finish()
        }

        btnStartRun.setOnClickListener {
            val enteredCode = codeEditText.text.toString().trim()

            if (enteredCode.isEmpty()) {
                Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (enteredCode.length < 6) {
                Toast.makeText(this, "Code must be 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyCodeAndStart(enteredCode)
        }
    }

    private fun verifyCodeAndStart(code: String) {
        val btnStartRun = findViewById<Button>(R.id.btnStartRun)
        btnStartRun.isEnabled = false

        db.collection("races")
            .whereEqualTo("raceCode", code)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val raceDocument = documents.documents[0]
                    val raceId = raceDocument.id
                    val raceName = raceDocument.getString("name") ?: "Unknown Race"

                    Toast.makeText(this, "Joining $raceName...", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, RunTrackerActivity::class.java)
                    intent.putExtra("RACE_ID", raceId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid Race Code. Please try again.", Toast.LENGTH_SHORT)
                        .show()
                    btnStartRun.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnStartRun.isEnabled = true
            }
    }
}