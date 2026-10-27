package com.mimo.companion.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mimo.companion.Graph
import com.mimo.companion.MainActivity
import com.mimo.companion.R
import com.mimo.companion.domain.reminders.Delivery
import com.mimo.companion.domain.reminders.Reminder
import kotlin.concurrent.thread

/** Fires due reminders, handles the notification's Done / Snooze buttons, and re-arms the next alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        thread(name = "mimo-reminder") {
            try {
                val id = intent.getStringExtra(EXTRA_ID)
                when (intent.action) {
                    ACTION_FIRE -> fireDue(app)
                    ACTION_DONE -> if (id != null) { Graph.reminders.markDone(id); NotificationManagerCompat.from(app).cancel(id.hashCode()); Graph.onReminderDone() }
                    ACTION_SNOOZE -> if (id != null) { Graph.reminders.snooze(id); NotificationManagerCompat.from(app).cancel(id.hashCode()) }
                }
                ReminderAlarm.reschedule(app)
            } finally { pending.finish() }
        }
    }

    private fun fireDue(app: Context) {
        val prefs = app.getSharedPreferences("reminder_runtime", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        // Window: since the last check, but never more than 10 minutes back (a phone that was off does not replay old reminders).
        val from = maxOf(prefs.getLong("lastCheck", now - 120_000L), now - 10 * 60_000L)
        val to = now + 30_000L
        val due = Graph.reminders.due(from, to)
        prefs.edit().putLong("lastCheck", to).apply()
        due.forEach { r ->
            Graph.reminders.consumeSnooze(r.id)
            deliver(app, r)
        }
    }

    private fun deliver(app: Context, r: Reminder) {
        val text = r.message(app.getString(R.string.reminder_time_for))
        if (Delivery.NOTIFICATION in r.delivery) notify(app, r, text)
        // Speech / bubble only while the companion is on screen; speech additionally respects quiet hours and calls.
        if (com.mimo.companion.overlay.FloatingService.running) {
            val quiet = Graph.dialogue.isQuietHours() || Graph.currentContext().inCall
            Graph.announce(text, speak = Delivery.SPEECH in r.delivery && !quiet, show = Delivery.VISUAL in r.delivery)
        }
    }

    private fun notify(app: Context, r: Reminder, text: String) {
        val nm = NotificationManagerCompat.from(app)
        if (!nm.areNotificationsEnabled()) return
        app.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        fun action(a: String, code: Int) = PendingIntent.getBroadcast(
            app, r.id.hashCode() + code, Intent(app, ReminderReceiver::class.java).setAction(a).putExtra(EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_mimo)
            .setContentTitle(r.name).setContentText(text)
            .setContentIntent(open).setAutoCancel(true)
            .addAction(0, app.getString(R.string.reminder_done), action(ACTION_DONE, 1))
            .addAction(0, app.getString(R.string.reminder_snooze, r.snoozeMinutes), action(ACTION_SNOOZE, 2))
            .build()
        try { nm.notify(r.id.hashCode(), n) } catch (_: SecurityException) { /* notification permission revoked meanwhile */ }
    }

    companion object {
        const val ACTION_FIRE = "com.mimo.companion.REMINDER_FIRE"
        const val ACTION_DONE = "com.mimo.companion.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.mimo.companion.REMINDER_SNOOZE"
        const val EXTRA_ID = "reminder_id"
        private const val CHANNEL = "reminders"
    }
}
