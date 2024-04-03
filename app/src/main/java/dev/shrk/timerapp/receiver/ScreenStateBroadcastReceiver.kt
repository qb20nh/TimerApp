package dev.shrk.timerapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenStateBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("RESUME", "ON")
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("RESUME", "UNLOCK")
            }
        }
    }
}