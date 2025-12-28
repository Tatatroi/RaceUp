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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RaceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RaceAdapter
    private lateinit var searchEditText: EditText
    private lateinit var btnSort: ImageButton
    private lateinit var emptyView: LinearLayout
    private lateinit var filterChipGroup: ChipGroup // <--- NEW

    private val allRacesList = mutableListOf<Race>()
    private val displayList = mutableListOf<Race>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    // Tracks the active filter
    private var selectedDistanceFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_list)

        // Init Views
        recyclerView = findViewById(R.id.raceRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        btnSort = findViewById(R.id.btnSort)
        emptyView = findViewById(R.id.emptyView)
        filterChipGroup = findViewById(R.id.filterChipGroup) // <--- NEW
        val addRaceButton = findViewById<View>(R.id.addRaceButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RaceAdapter(displayList) { race ->
            val intent = Intent(this, RaceDetailsActivity::class.java)
            intent.putExtra("raceId", race.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Setup Buttons
        if (auth.currentUser == null) addRaceButton.visibility = View.GONE
        else addRaceButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RaceFormActivity::class.java
                )
            )
        }

        val showPending = intent.getBooleanExtra("SHOW_PENDING", false)
        if (showPending) {
            findViewById<TextView>(R.id.tvTitle).text = "Pending Approvals"
            searchEditText.visibility = View.GONE
            btnSort.visibility = View.GONE
            // Hide filters in admin mode
            findViewById<View>(R.id.chipScrollView).visibility = View.GONE
        }

        loadRacesLive(showPending)

        // --- SEARCH LISTENER ---
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            } // Changed to call applyFilters

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // --- SORT LISTENER ---
        btnSort.setOnClickListener { showSortDialog() }

        // --- NEW: CHIP LISTENER ---
        filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                // Nothing selected
                selectedDistanceFilter = null
            } else {
                // Something selected, figure out which one
                when (checkedId) {
                    R.id.chip5k -> selectedDistanceFilter = "5" // Will match "5 km"
                    R.id.chip10k -> selectedDistanceFilter = "10"
                    R.id.chip21k -> selectedDistanceFilter = "21" // Matches 21.1 km
                    R.id.chip42k -> selectedDistanceFilter = "42" // Matches 42.2 km
                }
            }
            // Re-run the filter with new chip setting
            applyFilters()
        }
    }

    // --- THE MAIN FILTER FUNCTION ---
    private fun applyFilters() {
        val searchText = searchEditText.text.toString().lowercase().trim()

        displayList.clear()

        for (race in allRacesList) {
            // 1. Check Text
            val matchesText = if (searchText.isEmpty()) true else {
                race.name.lowercase().contains(searchText) ||
                        race.distance.lowercase().contains(searchText)
            }

            // 2. Check Chip
            // The race distance string looks like "5 km, 10 km, 21.1 km"
            // We check if it contains our filter key (e.g., "10")
            val matchesChip = if (selectedDistanceFilter == null) true else {
                race.distance.contains(selectedDistanceFilter!!)
            }

            // Add ONLY if both are true
            if (matchesText && matchesChip) {
                displayList.add(race)
            }
        }

        adapter.notifyDataSetChanged()
        updateEmptyView()
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
                    applyFilters() // Apply filters immediately after loading
                }
            }
    }

    // ... (Keep your existing showSortDialog, parseDate, updateEmptyView, onDestroy) ...
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

    private fun parseDate(dateString: String): Long {
        return try {
            val parts = dateString.split("/")
            val year = parts[2].toInt()
            val month = parts[1].toInt()
            val day = parts[0].toInt()
            (year * 10000 + month * 100 + day).toLong()
        } catch (e: Exception) {
            0L
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