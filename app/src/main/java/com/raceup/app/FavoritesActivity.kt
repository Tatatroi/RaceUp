package com.raceup.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent

class FavoritesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RaceAdapter
    private val favoritesList = mutableListOf<Race>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recyclerView = findViewById(R.id.favoritesRecyclerView)
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

                if (raceIds.isEmpty()) return@addOnSuccessListener

                // Step 2: query the races collection using those IDs
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
                    }
            }
    }
}
