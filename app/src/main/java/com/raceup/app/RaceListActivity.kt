package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RaceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RaceAdapter
    private lateinit var searchEditText: EditText
    private lateinit var btnSort: ImageButton
    private lateinit var emptyView: LinearLayout

    // We need TWO lists:
    // 1. The full data from DB
    // 2. The filtered data currently shown
    private val allRacesList = mutableListOf<Race>()
    private val displayList = mutableListOf<Race>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_list)

        // Init Views
        recyclerView = findViewById(R.id.raceRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        btnSort = findViewById(R.id.btnSort)
        emptyView = findViewById(R.id.emptyView)
        val addRaceButton = findViewById<View>(R.id.addRaceButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Adapter uses displayList (the filtered one)
        adapter = RaceAdapter(displayList) { race ->
            val intent = Intent(this, RaceDetailsActivity::class.java)
            intent.putExtra("raceId", race.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Guest Mode Logic
        if (auth.currentUser == null) {
            addRaceButton.visibility = View.GONE
        } else {
            addRaceButton.visibility = View.VISIBLE
            addRaceButton.setOnClickListener {
                startActivity(Intent(this, RaceFormActivity::class.java))
            }
        }

        // Admin Review Logic
        val showPending = intent.getBooleanExtra("SHOW_PENDING", false)
        if (showPending) {
            findViewById<TextView>(R.id.tvTitle).text = "Pending Approvals"
            // Hide search/sort in admin review to keep it simple, or keep them if you want
            searchEditText.visibility = View.GONE
            btnSort.visibility = View.GONE
        }

        // Load Data
        loadRacesLive(showPending)

        // SEARCH LOGIC
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // SORT LOGIC
        btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun loadRacesLive(showPending: Boolean) {
        val targetStatus = !showPending

        listenerRegistration = db.collection("races")
            .whereEqualTo("isApproved", targetStatus)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    allRacesList.clear()
                    for (doc in snapshot.documents) {
                        val race = doc.toObject(Race::class.java)
                        race?.id = doc.id
                        if (race != null) allRacesList.add(race)
                    }
                    // Initially, display everything
                    displayList.clear()
                    displayList.addAll(allRacesList)
                    adapter.notifyDataSetChanged()

                    updateEmptyView()
                }
            }
    }

    private fun filter(text: String) {
        displayList.clear()
        if (text.isEmpty()) {
            displayList.addAll(allRacesList)
        } else {
            val query = text.lowercase()
            for (race in allRacesList) {
                if (race.name.lowercase().contains(query) || race.distance.lowercase().contains(query)) {
                    displayList.add(race)
                }
            }
        }
        adapter.notifyDataSetChanged()

        updateEmptyView()
    }

    private fun showSortDialog() {
        val options = arrayOf("Name (A-Z)", "Date (Newest First)", "Date (Oldest First)")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sort By")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> displayList.sortBy { it.name }
                1 -> displayList.sortByDescending { parseDate(it.date) }
                2 -> displayList.sortBy { parseDate(it.date) }
            }
            adapter.notifyDataSetChanged()
        }
        builder.show()
    }

    // Helper to parse your date string "dd/MM/yyyy" for sorting
    private fun parseDate(dateString: String): Long {
        return try {
            val parts = dateString.split("/")
            // format: YYYYMMDD for easy number comparison
            val year = parts[2].toInt()
            val month = parts[1].toInt()
            val day = parts[0].toInt()

            // Return a comparable number (timestamp or just huge int)
            (year * 10000 + month * 100 + day).toLong()
        } catch (e: Exception) {
            0L // If date is weird, put it at the end/start
        }
    }

    private fun updateEmptyView() {
        if (displayList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}