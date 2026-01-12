package com.raceup.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.raceup.app.firebase.FirebaseAuthManager
import com.raceup.app.firebase.FirebaseFirestoreManager
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var birthDateEditText: EditText
    private lateinit var genderSpinner: Spinner

    private lateinit var registerButton: Button
    private lateinit var loginRedirect: TextView
    private lateinit var profileImageView: ImageView

    private var selectedImageUri: Uri? = null

    // Image Picker Logic
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                profileImageView.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        emailEditText = findViewById(R.id.emailRegisterEditText)
        passwordEditText = findViewById(R.id.passwordRegisterEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)

        birthDateEditText = findViewById(R.id.birthDateEditText)
        birthDateEditText.isFocusable = false
        birthDateEditText.isClickable = true

        genderSpinner = findViewById(R.id.genderSpinner)

        registerButton = findViewById(R.id.registerButton)
        loginRedirect = findViewById(R.id.loginRedirect)
        profileImageView = findViewById(R.id.profileImageView)

        profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val genderOptions = arrayOf("Pick your gender", "Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val birthDate = birthDateEditText.text.toString().trim()
            val gender = genderSpinner.selectedItem.toString()

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (gender == "Pick your gender") {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match!"
                return@setOnClickListener
            }

            FirebaseAuthManager().registerUser(email, password) { success, errorMessage ->
                if (success) {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    if (currentUserId != null) {
                        var localImagePath = ""
                        if (selectedImageUri != null) {
                            localImagePath =
                                saveImageToInternalStorage(currentUserId, selectedImageUri!!)
                        }

                        FirebaseFirestoreManager().saveUserExtraData(
                            currentUserId,
                            firstName,
                            lastName,
                            birthDate,
                            gender,
                            localImagePath
                        )

                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT)
                            .show()

                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Error: User ID not found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        birthDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    birthDateEditText.setText(formattedDate)
                },
                year, month, day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun saveImageToInternalStorage(userId: String, uri: Uri): String {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            }

            val fileName = "profile_$userId.jpg"
            val file = File(this.filesDir, fileName)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            outputStream.flush()
            outputStream.close()

            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}