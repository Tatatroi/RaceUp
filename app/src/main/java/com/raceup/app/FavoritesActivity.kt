package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout // 1. Add this variable
    private lateinit var adapter: RaceAdapter
    private val favoritesList = mutableListOf<Race>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyView = findViewById(R.id.emptyView) // 2. Initialize it

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RaceAdapter(favoritesList) { race ->
            val intent = Intent(this, RaceDetailsActivity::class.java)
            intent.putExtra("raceId", race.id)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return

        // Step 1: get list of race IDs in user's favorites
        db.collection("users")
            .document(uid)
            .collection("favorites")
            .get()
            .addOnSuccessListener { favDocs ->
                val raceIds = favDocs.documents.map { it.id }

                // CASE: No favorites found in database
                if (raceIds.isEmpty()) {
                    favoritesList.clear()
                    adapter.notifyDataSetChanged()
                    showEmptyState(true) // Show the "No Favorites" view
                    return@addOnSuccessListener
                }

                // Step 2: query the races collection using those IDs
                // Note: Firestore 'whereIn' supports a maximum of 10 IDs at once.
                db.collection("races")
                    .whereIn(FieldPath.documentId(), raceIds)
                    .get()
                    .addOnSuccessListener { raceDocs ->
                        favoritesList.clear()
                        for (doc in raceDocs.documents) {
                            val race = doc.toObject(Race::class.java)
                            race?.id = doc.id
                            if (race != null) favoritesList.add(race)
                        }
                        adapter.notifyDataSetChanged()

                        // Check if list is empty (e.g., if favorited races were deleted)
                        showEmptyState(favoritesList.isEmpty())
                    }
                    .addOnFailureListener {
                        showEmptyState(true)
                    }
            }
            .addOnFailureListener {
                showEmptyState(true)
            }
    }

    // 3. Helper function to toggle views
    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}