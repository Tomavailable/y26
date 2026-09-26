package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReviewReminderManager.isReminderEnabled(context)) {
                val (hour, minute) = ReviewReminderManager.getReminderTime(context)
                ReviewReminderManager.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
