package com.raceup.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "RaceUp"
        val message = intent.getStringExtra("message") ?: "Upcoming Race!"
        val raceId = intent.getStringExtra("raceId") ?: ""

        // 1. Create the Notification Channel (Required for Android 8+)
        // IMPORTANCE_DEFAULT means it makes a sound but doesn't pop over everything intrusively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "race_reminders"
            val channelName = "Race Reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Define what happens when you tap the notification
        val tapIntent = Intent(context, RaceDetailsActivity::class.java).apply {
            putExtra("raceId", raceId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            raceId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build the actual Notification
        val builder = NotificationCompat.Builder(context, "race_reminders")
            .setSmallIcon(R.drawable.ic_calendar_today_24) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Removes notification when tapped

        // 4. Show it (Check permission first)
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}