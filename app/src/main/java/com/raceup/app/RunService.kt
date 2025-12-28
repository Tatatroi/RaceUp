package com.raceup.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class RunService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null

    // Timer & Stats
    private var secondsElapsed = 0L
    private var totalDistanceMeters = 0.0
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            broadcastUpdate() // Send data to Activity
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // FIX FOR ANDROID 14: Must specify the service type if you declared it in Manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, buildNotification())
        }

        setupLocationTracking()
    }

    private fun setupLocationTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                for (location in res.locations) {
                    if (lastLocation != null) {
                        totalDistanceMeters += lastLocation!!.distanceTo(location)
                    }
                    lastLocation = location

                    // Optional: Broadcast immediately on location change for smoother distance
                    broadcastUpdate()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("RunService", "Location Permission missing: ${e.message}")
        }

        // FIX: Start timer OUTSIDE the try block so it runs even if GPS is slow to start
        // (But usually, you want to ensure permissions are granted before starting service)
        handler.post(timerRunnable)
    }

    private fun broadcastUpdate() {
        val intent = Intent("RACE_UPDATE")
        intent.putExtra("SECONDS", secondsElapsed)
        intent.putExtra("DISTANCE", totalDistanceMeters)
        // Required for newer Android versions to ensure specific package receives it
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "run_channel",
                "Race Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "run_channel")
            .setContentTitle("RaceUp is tracking your run")
            .setContentText("Keep going!")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true) // Prevents user from swiping it away accidentally
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {
            // Ignore
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}