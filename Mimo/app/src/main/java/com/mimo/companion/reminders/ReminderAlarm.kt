package com.mimo.companion.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mimo.companion.Graph

/**
 * Keeps exactly ONE system alarm: the earliest upcoming habit reminder or prayer reminder. Uses setAndAllowWhileIdle
 * (no special permission): Android may delay it by a few minutes in Doze / battery saver, so reminders are
 * "about on time", not to-the-second. Exact alarms were deliberately not requested (extra permission, policy review).
 * Call from a background thread (reads the database).
 */
object ReminderAlarm {
    private const val REQUEST = 4101

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(app)
        val habit = try { Graph.reminders.nextUpcoming()?.atMillis } catch (e: Exception) { null }
        val prayer = try { Graph.prayer.nextTrigger(Graph.prayerSettings.value)?.atMillis } catch (e: Exception) { null }
        val next = listOfNotNull(habit, prayer).minOrNull()
        if (next == null) { am.cancel(pi); return }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }

    private fun pendingIntent(app: Context): PendingIntent =
        PendingIntent.getBroadcast(
            app, REQUEST,
            Intent(app, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
