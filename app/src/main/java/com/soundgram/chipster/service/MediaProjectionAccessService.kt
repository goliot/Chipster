package com.soundgram.chipster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.soundgram.chipster.R


class MediaProjectionAccessService : Service() {

    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "my_channel_id"
            val channel =
                NotificationChannel(channelId, "My Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("AR 서비스 가동중 ")
//                .setContentText("AR")
                .setSmallIcon(R.drawable.ic_map)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}