package com.iocl.knowledgehub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IOCLApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val mainChannel = NotificationChannel(
                "iocl_notifications",
                "IOCL Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New magazine and newspaper notifications"
                enableLights(true)
                enableVibration(true)
            }

            val silentChannel = NotificationChannel(
                "iocl_silent",
                "Background Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent background sync notifications"
            }

            manager.createNotificationChannel(mainChannel)
            manager.createNotificationChannel(silentChannel)
        }
    }
}
