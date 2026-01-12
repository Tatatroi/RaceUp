package com.raceup.app

import android.content.Intent
import android.os.Bundle
import android.view.View // Import this for View.VISIBLE / View.GONE
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var recyclerView: RecyclerView
    private lateinit var textNoGroups: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        recyclerView = findViewById(R.id.recyclerGroups)
        recyclerView.layoutManager = LinearLayoutManager(this)

        textNoGroups = findViewById(R.id.statusText)

        val btnCreateGroup = findViewById<Button>(R.id.btnCreateGroup)
        btnCreateGroup.setOnClickListener { showCreateGroupDialog() }

        loadMyGroups()
    }

    private fun loadMyGroups() {
        if (currentUser == null) return

        db.collection("groups")
            .whereArrayContains("members", currentUser.uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading groups", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val groupList = ArrayList<Group>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val group = doc.toObject(Group::class.java)
                        group.id = doc.id
                        groupList.add(group)
                    }
                }

                if (groupList.isEmpty()) {
                    textNoGroups.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    textNoGroups.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                recyclerView.adapter = GroupsAdapter(groupList) { selectedGroup ->
                    val intent = Intent(this, GroupDetailsActivity::class.java)
                    intent.putExtra("GROUP_ID", selectedGroup.id)
                    startActivity(intent)
                }
            }
    }

    private fun showCreateGroupDialog() {
        val input = EditText(this)
        input.hint = "Enter Group Name"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("New Group")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) createGroupInFirestore(groupName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createGroupInFirestore(groupName: String) {
        if (currentUser == null) return

        val timestamp = System.currentTimeMillis()

        val newGroupRef = db.collection("groups").document()
        val newGroup = Group(
            id = newGroupRef.id,
            name = groupName,
            createdBy = currentUser.uid,
            members = listOf(currentUser.uid),
            createdAt = timestamp
        )

        newGroupRef.set(newGroup)
            .addOnSuccessListener {
                Toast.makeText(this, "Group Created!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}