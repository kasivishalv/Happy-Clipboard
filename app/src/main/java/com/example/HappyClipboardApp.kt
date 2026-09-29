package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.core.security.SecurityPreferences
import com.example.data.db.AppDatabase
import com.example.data.repository.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HappyClipboardApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { SecurityPreferences(this) }
    val repository by lazy {
        ClipboardRepository(
            database.clipboardDao(),
            database.collectionDao(),
            preferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("HappyClipboard", "APP_START")
        instance = this
        createNotificationChannel()

        applicationScope.launch(Dispatchers.IO) {
            try {
                repository.seedSampleDataIfEmpty()
                repository.cleanupExpired()
            } catch (e: Exception) {
                android.util.Log.e("HappyClipboard", "Error during repository startup seed/cleanup", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors clipboard changes and captures copied content offline"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "clipboard_monitor_channel"
        lateinit var instance: HappyClipboardApp
            private set
    }
}
