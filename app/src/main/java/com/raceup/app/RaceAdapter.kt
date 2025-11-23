package com.raceup.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RaceAdapter(
    private val races: List<Race>,
    private val onRaceClick: (Race) -> Unit
) : RecyclerView.Adapter<RaceAdapter.RaceViewHolder>() {

    inner class RaceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.raceNameText)
        val dateText: TextView = view.findViewById(R.id.raceDateText)
        val distanceText: TextView = view.findViewById(R.id.raceDistanceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_race, parent, false)
        return RaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val race = races[position]

        holder.nameText.text = race.name
        holder.dateText.text = race.date
        holder.distanceText.text = race.distance

        holder.view.setOnClickListener {
            onRaceClick(race)  // Call the callback instead of directly starting activity
        }
    }

    override fun getItemCount(): Int = races.size
}
