package com.mimo.companion.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

/** Alarms are lost on reboot / app update / clock or time-zone change: re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        thread(name = "mimo-rearm") { try { ReminderAlarm.reschedule(app) } finally { pending.finish() } }
    }
}
