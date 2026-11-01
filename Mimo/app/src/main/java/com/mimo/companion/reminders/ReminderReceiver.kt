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
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerSettings
import com.mimo.companion.domain.prayer.PrayerTrigger
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
                    ACTION_PRAYED -> prayerAction(app, intent, prayed = true)
                    ACTION_PRAYER_SNOOZE -> prayerAction(app, intent, prayed = false)
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
        val ps = Graph.prayerSettings.value
        Graph.prayer.due(ps, from, to).forEach { t ->
            Graph.prayer.onFired(ps, t, now)      // books the follow-up (if any) BEFORE notifying
            deliverPrayer(app, ps, t)
        }
        Graph.prayer.pruneOld(ps, 14)
    }

    private fun prayerAction(app: Context, intent: Intent, prayed: Boolean) {
        val dayKey = intent.getStringExtra(EXTRA_DAY) ?: return
        val prayer = Prayer.values().firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) } ?: return
        val ps = Graph.prayerSettings.value
        if (prayed) { Graph.prayer.markPrayed(java.time.LocalDate.parse(dayKey), prayer); Graph.onPrayerLogged() }
        else Graph.prayer.snooze(ps, dayKey, prayer)
        NotificationManagerCompat.from(app).cancel(("prayer:$dayKey:${prayer.name}").hashCode())
    }

    private fun deliverPrayer(app: Context, ps: PrayerSettings, t: PrayerTrigger) {
        val name = app.getString(prayerNameRes(t.prayer))
        val text = ps.customMessage?.takeIf { it.isNotBlank() }
            ?: app.getString(if (ps.minutesBefore > 0) R.string.prayer_soon else R.string.prayer_now, name)
        val nm = NotificationManagerCompat.from(app)
        if (nm.areNotificationsEnabled()) {
            app.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(PRAYER_CHANNEL, app.getString(R.string.prayer_channel), NotificationManager.IMPORTANCE_DEFAULT))
            fun act(a: String, code: Int) = PendingIntent.getBroadcast(
                app, ("prayer:${t.dayKey}:${t.prayer.name}").hashCode() + code,
                Intent(app, ReminderReceiver::class.java).setAction(a).putExtra(EXTRA_DAY, t.dayKey).putExtra(EXTRA_PRAYER, t.prayer.name),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(app, PRAYER_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_mimo).setContentTitle(name).setContentText(text)
                .setContentIntent(open).setAutoCancel(true)
                .addAction(0, app.getString(R.string.prayer_prayed), act(ACTION_PRAYED, 1))
                .addAction(0, app.getString(R.string.prayer_snooze, ps.gapMinutes), act(ACTION_PRAYER_SNOOZE, 2))
                .build()
            try { nm.notify(("prayer:${t.dayKey}:${t.prayer.name}").hashCode(), n) } catch (_: SecurityException) { }
        }
        if (com.mimo.companion.overlay.FloatingService.running) {
            val quiet = Graph.dialogue.isQuietHours() || Graph.currentContext().inCall
            Graph.announce(text, speak = !quiet, show = true)
        }
    }

    private fun prayerNameRes(p: Prayer) = when (p) {
        Prayer.FAJR -> R.string.prayer_fajr; Prayer.SUNRISE -> R.string.prayer_sunrise; Prayer.DHUHR -> R.string.prayer_dhuhr
        Prayer.ASR -> R.string.prayer_asr; Prayer.MAGHRIB -> R.string.prayer_maghrib; Prayer.ISHA -> R.string.prayer_isha
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
        const val ACTION_PRAYED = "com.mimo.companion.PRAYER_PRAYED"
        const val ACTION_PRAYER_SNOOZE = "com.mimo.companion.PRAYER_SNOOZE"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_DAY = "prayer_day"
        const val EXTRA_PRAYER = "prayer_name"
        private const val PRAYER_CHANNEL = "prayer"
        private const val CHANNEL = "reminders"
    }
}
