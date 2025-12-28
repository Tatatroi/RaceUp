package com.raceup.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

// 2. The Adapter Class
class RunHistoryAdapter(
    private val runList: List<RunHistoryItem>
) : RecyclerView.Adapter<RunHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These IDs must match your item_run.xml
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvPace: TextView = itemView.findViewById(R.id.tvPace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // Make sure to inflate 'item_run', NOT 'item_race'
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_run, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val run = runList[position]

        // 1. Format Date (e.g., "Oct 24, 2023 • 14:30")
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        holder.tvDate.text = if (run.date != null) dateFormat.format(run.date) else "Unknown Date"

        // 2. Format Distance (Meters -> KM)
        val km = run.distanceMeters / 1000.0
        holder.tvDistance.text = String.format(Locale.US, "%.2f km", km)

        // 3. Format Duration (Seconds -> HH:MM:SS)
        val hours = run.durationSeconds / 3600
        val minutes = (run.durationSeconds % 3600) / 60
        val seconds = run.durationSeconds % 60
        holder.tvTime.text = String.format("Time: %02d:%02d:%02d", hours, minutes, seconds)

        // 4. Pace
        holder.tvPace.text = "${run.avgPace} /km"
    }

    override fun getItemCount() = runList.size
}