package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RaceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RaceAdapter
    private val raceList = mutableListOf<Race>()
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_race_list)

        recyclerView = findViewById(R.id.raceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = RaceAdapter(raceList) { race ->
            val intent = Intent(this, RaceDetailsActivity::class.java)
            intent.putExtra("raceId", race.id)
            startActivity(intent)
        }

        findViewById<Button>(R.id.addRaceButton).setOnClickListener {
            startActivity(Intent(this, RaceFormActivity::class.java))
        }

        loadRacesLive()
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    /**
     * LIVE DATA — updates automatically when new races are added
     */
    private fun loadRacesLive() {
        listenerRegistration = db.collection("races")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                raceList.clear()
                for (doc in snapshot.documents) {
                    val race = doc.toObject(Race::class.java)
                    race?.id = doc.id
                    if (race != null) raceList.add(race)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
