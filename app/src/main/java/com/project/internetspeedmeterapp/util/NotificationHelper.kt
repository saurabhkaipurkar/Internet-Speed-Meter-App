package com.project.internetspeedmeterapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.project.internetspeedmeterapp.R

object NotificationHelper {
    private const val CHANNEL_ID = "speed_meter_channel"

    fun createNotification(context: Context, speed: String): Notification {
        createNotificationChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Internet Speed Meter")
            .setContentText("Monitoring Speed: \n$speed")
            .setSmallIcon(R.drawable.network_check)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Speed Meter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for monitoring speed"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
