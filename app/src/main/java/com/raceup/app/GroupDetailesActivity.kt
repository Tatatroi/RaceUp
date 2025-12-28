package com.raceup.app

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.Date

// Updated Data Class with ID
data class MemberStats(val userId: String, val name: String, val totalDistance: Double, val totalRuns: Int)

class GroupDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var recyclerLeaderboard: RecyclerView
    private lateinit var textGroupName: TextView
    private lateinit var btnExitGroup: Button

    private var currentGroupId: String = ""
    private var groupCreationTimestamp: Long = 0
    private var groupCreatorId: String = "" // Stores who owns the group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_details)

        currentGroupId = intent.getStringExtra("GROUP_ID") ?: ""

        textGroupName = findViewById(R.id.textGroupName)
        btnExitGroup = findViewById(R.id.btnExitGroup)
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnAddMember).setOnClickListener {
            showAddMemberDialog()
        }

        // Handle Leave/Delete Button
        btnExitGroup.setOnClickListener {
            if (currentUser?.uid == groupCreatorId) {
                confirmDeleteGroup()
            } else {
                confirmLeaveGroup()
            }
        }

        if (currentGroupId.isEmpty()) finish()
    }

    override fun onResume() {
        super.onResume()
        if (currentGroupId.isNotEmpty()) loadGroupData()
    }

    private fun loadGroupData() {
        db.collection("groups").document(currentGroupId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val groupName = document.getString("name") ?: "Group"
                    textGroupName.text = groupName

                    groupCreationTimestamp = document.getLong("createdAt") ?: 0L
                    groupCreatorId = document.getString("createdBy") ?: ""

                    // UPDATE UI: Am I the Admin?
                    updateAdminUI()

                    val members = document.get("members") as? List<String> ?: emptyList()
                    calculateLeaderboard(members)
                } else {
                    // Group was deleted while I was looking at it
                    Toast.makeText(this, "Group no longer exists", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun updateAdminUI() {
        if (currentUser?.uid == groupCreatorId) {
            btnExitGroup.text = "Delete Group"
            btnExitGroup.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
        } else {
            btnExitGroup.text = "Leave Group"
            btnExitGroup.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        }
    }

    // --- BUTTON ACTIONS ---

    private fun confirmDeleteGroup() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group?")
            .setMessage("This action cannot be undone. All members will lose this group data.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("groups").document(currentGroupId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Group Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group?")
            .setMessage("Are you sure you want to leave?")
            .setPositiveButton("Leave") { _, _ ->
                db.collection("groups").document(currentGroupId)
                    .update("members", FieldValue.arrayRemove(currentUser!!.uid))
                    .addOnSuccessListener {
                        Toast.makeText(this, "You left the group", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmKickMember(memberId: String, memberName: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove $memberName from the group?")
            .setPositiveButton("Remove") { _, _ ->
                // Delete the ID from the array in Firebase
                db.collection("groups").document(currentGroupId)
                    .update("members", FieldValue.arrayRemove(memberId))
                    .addOnSuccessListener {
                        Toast.makeText(this, "$memberName removed", Toast.LENGTH_SHORT).show()
                        loadGroupData() // Refresh the list immediately
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error removing member", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- LEADERBOARD LOGIC ---

    private fun calculateLeaderboard(memberIds: List<String>) {
        if (memberIds.isEmpty()) return

        val statsList = ArrayList<MemberStats>()
        var processedCount = 0
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val cal = Calendar.getInstance()
        cal.timeInMillis = if (groupCreationTimestamp > 0) groupCreationTimestamp else 0
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val groupMidnightTime = cal.timeInMillis

        for (userId in memberIds) {
            db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                val firstName = userDoc.getString("firstName") ?: "Unknown"
                val lastName = userDoc.getString("lastName") ?: ""
                val fullName = "$firstName $lastName"

                db.collection("users").document(userId).collection("runs")
                    .get()
                    .addOnSuccessListener { runsSnapshot ->
                        var totalDist = 0.0
                        var validRunCount = 0

                        for (run in runsSnapshot) {
                            try {
                                val rawDate = run.get("date")
                                var runTimeInMillis: Long = 0
                                var isDateValid = false

                                if (rawDate is String) {
                                    val dateObj = sdf.parse(rawDate)
                                    if (dateObj != null) runTimeInMillis = dateObj.time
                                    isDateValid = true
                                } else if (rawDate is Timestamp) {
                                    runTimeInMillis = rawDate.toDate().time
                                    isDateValid = true
                                } else if (rawDate is Date) {
                                    runTimeInMillis = rawDate.time
                                    isDateValid = true
                                } else {
                                    isDateValid = true
                                    runTimeInMillis = System.currentTimeMillis()
                                }

                                if (isDateValid && runTimeInMillis >= groupMidnightTime) {
                                    val distString = run.getString("distance") ?: "0"
                                    val cleanDistString = distString.split(",")[0]
                                    val distValue = cleanDistString.replace("km", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0

                                    totalDist += distValue
                                    validRunCount++
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        // PASS userId to the data object
                        statsList.add(MemberStats(userId, fullName, totalDist, validRunCount))
                        processedCount++

                        if (processedCount == memberIds.size) {
                            showLeaderboard(statsList)
                        }
                    }
            }.addOnFailureListener {
                processedCount++
                if (processedCount == memberIds.size) showLeaderboard(statsList)
            }
        }
    }



    private fun showLeaderboard(list: ArrayList<MemberStats>) {
        list.sortByDescending { it.totalDistance }

        // We pass a function here that runs when you LONG PRESS a user
        recyclerLeaderboard.adapter = LeaderboardAdapter(list) { selectedMember ->

            // 1. Check if I am the Creator
            if (currentUser?.uid == groupCreatorId) {

                // 2. Check if the person I clicked is NOT me (Can't kick myself)
                if (selectedMember.userId != currentUser.uid) {
                    confirmKickMember(selectedMember.userId, selectedMember.name)
                } else {
                    Toast.makeText(this, "You cannot kick yourself", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ... (Keep showAddMemberDialog and addMemberByEmail exactly as they were) ...
    private fun showAddMemberDialog() {
        val input = EditText(this)
        input.hint = "Enter friend's email"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("Add Member")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) addMemberByEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addMemberByEmail(email: String) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Email not found", Toast.LENGTH_LONG).show()
                } else {
                    val userIdToAdd = documents.documents[0].id
                    db.collection("groups").document(currentGroupId)
                        .update("members", FieldValue.arrayUnion(userIdToAdd))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Added!", Toast.LENGTH_SHORT).show()
                            loadGroupData()
                        }
                }
            }
    }

    // Updated Adapter to accept a "Long Click" function
    class LeaderboardAdapter(
        private val stats: List<MemberStats>,
        private val onMemberLongClick: (MemberStats) -> Unit // <--- NEW PARAMETER
    ) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rank: TextView = view.findViewById(R.id.textRank)
            val name: TextView = view.findViewById(R.id.textMemberName)
            val stats: TextView = view.findViewById(R.id.textMemberStats)
            val total: TextView = view.findViewById(R.id.textTotalDistance)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard_member, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = stats[position]
            holder.rank.text = "#${position + 1}"
            holder.name.text = item.name
            holder.stats.text = "Runs: ${item.totalRuns}"
            holder.total.text = String.format("%.1f km", item.totalDistance)

            // --- THE LOGIC FOR KICKING ---
            // When you hold down on a name...
            holder.itemView.setOnLongClickListener {
                onMemberLongClick(item) // Trigger the popup
                true // "True" means we consumed the click
            }
        }

        override fun getItemCount() = stats.size
    }
}