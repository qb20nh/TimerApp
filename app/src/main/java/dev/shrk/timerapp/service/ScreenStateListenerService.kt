package dev.shrk.timerapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.shrk.timerapp.MainActivity
import dev.shrk.timerapp.R
import dev.shrk.timerapp.receiver.ScreenStateBroadcastReceiver
import dev.shrk.timerapp.values.Notifications

class ScreenStateListenerService: Service() {
    private lateinit var receiver: ScreenStateBroadcastReceiver

    companion object {
        var isServiceRunning = false
        private const val NOTIFICATION_ID = Notifications.screenStateListenerServiceNotification
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            // Initialize and start your foreground service
            startForeground(NOTIFICATION_ID, createNotification(this))

            receiver = ScreenStateBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(receiver, filter)

            isServiceRunning = true
        }
        // Handle any additional commands
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(context: Context): Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "screenStateListenerServiceNotification"
        val channelName = "Screen State Monitoring"

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Monitors and records the times when the device's screen is unlocked or turned on, to enhance app functionality."
                // Customize the channel's initial settings
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Build the notification
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Screen State Monitoring Active")
            .setContentText("Recording screen unlock and turn on times. Tap for more info or to disable.")
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle())
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your notification icon
            // Add additional settings as needed
            .build()
    }

}