package com.mimo.companion.data.db

import com.mimo.companion.domain.reminders.Delivery
import com.mimo.companion.domain.reminders.Reminder
import com.mimo.companion.domain.reminders.ReminderStorage
import com.mimo.companion.domain.reminders.RepeatType
import java.time.DayOfWeek

/** Room-backed reminders. Call from a background thread. */
class RoomReminderStorage(private val db: MimoDatabase) : ReminderStorage {
    private val dao = db.reminderDao()

    override fun all() = dao.all().map { it.toDomain() }
    override fun get(id: String) = dao.get(id)?.toDomain()

    override fun upsert(r: Reminder) {
        // keep an existing snooze when only the definition changes
        val snooze = dao.get(r.id)?.snoozedUntil ?: 0L
        dao.upsert(r.toEntity(snooze))
    }

    override fun delete(id: String) { dao.delete(id); dao.deleteDone(id) }
    override fun completedDays(id: String) = dao.doneDays(id).toSet()
    override fun addCompletion(id: String, dayKey: String) { dao.addDone(ReminderDoneEntity(id, dayKey)) }
    override fun snoozedUntil(id: String) = dao.get(id)?.snoozedUntil ?: 0L
    override fun setSnoozedUntil(id: String, millis: Long) = dao.setSnooze(id, millis)
    fun prune(oldestKeepDayKey: String) = dao.pruneDone(oldestKeepDayKey)

    private fun Reminder.toEntity(snoozedUntil: Long) = ReminderEntity(
        id, name, description, timeMin, repeat.name,
        days.fold(0) { m, d -> m or (1 shl (d.value - 1)) }, onceDate, enabled, customMessage,
        delivery.fold(0) { m, d -> m or (1 shl d.ordinal) }, snoozeMinutes, snoozedUntil,
    )

    private fun ReminderEntity.toDomain() = Reminder(
        id = id, name = name, description = description, timeMin = timeMin,
        repeat = RepeatType.values().firstOrNull { it.name == repeatType } ?: RepeatType.DAILY,
        days = DayOfWeek.values().filter { daysMask and (1 shl (it.value - 1)) != 0 }.toSet(),
        onceDate = onceDate, enabled = enabled, customMessage = customMessage,
        delivery = Delivery.values().filter { deliveryMask and (1 shl it.ordinal) != 0 }.toSet().ifEmpty { setOf(Delivery.NOTIFICATION) },
        snoozeMinutes = snoozeMinutes,
    )
}
