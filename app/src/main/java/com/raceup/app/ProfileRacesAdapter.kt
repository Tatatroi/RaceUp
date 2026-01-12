package com.raceup.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class RunHistoryItemProfile(
    val id: String,
    val name: String,
    val distance: String,
    val date: String
)

class ProfileRacesAdapter(
    private val runs: List<RunHistoryItemProfile>,
    private val onRunClicked: (RunHistoryItemProfile) -> Unit
) : RecyclerView.Adapter<ProfileRacesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itemRaceName)
        val date: TextView = view.findViewById(R.id.itemRaceDate)
        val dist: TextView = view.findViewById(R.id.itemRaceDist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_race, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val run = runs[position]
        holder.name.text = run.name
        holder.date.text = run.date
        holder.dist.text = run.distance

        // Handle Click
        holder.itemView.setOnClickListener { onRunClicked(run) }
    }

    override fun getItemCount() = runs.size
}