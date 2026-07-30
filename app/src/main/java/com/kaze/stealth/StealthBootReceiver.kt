package com.kaze.stealth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StealthBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Core.start(context.applicationContext)
        }
    }
}
