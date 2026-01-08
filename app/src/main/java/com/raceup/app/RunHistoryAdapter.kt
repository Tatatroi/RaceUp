package com.raceup.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class RunHistoryAdapter(
    private val runList: List<RunHistoryItem>
) : RecyclerView.Adapter<RunHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRaceName: TextView = itemView.findViewById(R.id.tvRaceName)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvPace: TextView = itemView.findViewById(R.id.tvPace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val run = runList[position]

        holder.tvRaceName.text = if (run.raceName.isNotEmpty()) run.raceName else "Virtual Run"

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = if (run.date != null) dateFormat.format(run.date) else ""

        val km = run.distanceMeters / 1000.0
        holder.tvDistance.text = String.format(Locale.US, "%.2f km", km)

        val hours = run.durationSeconds / 3600
        val minutes = (run.durationSeconds % 3600) / 60
        val seconds = run.durationSeconds % 60
        if (hours > 0) {
            holder.tvTime.text = String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            holder.tvTime.text = String.format("%02d:%02d", minutes, seconds)
        }

        holder.tvPace.text = "${run.avgPace} /km"
    }

    override fun getItemCount() = runList.size
}