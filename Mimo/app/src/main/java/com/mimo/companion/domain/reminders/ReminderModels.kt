package com.mimo.companion.domain.reminders

import java.time.DayOfWeek

enum class RepeatType { ONCE, DAILY, WEEKLY }

/** How the user wants to be reminded. VISUAL = text bubble on the floating companion. */
enum class Delivery { NOTIFICATION, SPEECH, VISUAL }

/**
 * A user-defined habit / reminder (water, sleep, study, plants, medication appointment, anything).
 * This is a scheduling tool only: it gives no medical or health advice.
 */
data class Reminder(
    val id: String,
    val name: String,
    val description: String? = null,
    /** Minutes since midnight, device time zone. */
    val timeMin: Int,
    val repeat: RepeatType = RepeatType.DAILY,
    /** Used by WEEKLY. */
    val days: Set<DayOfWeek> = emptySet(),
    /** ISO date (yyyy-MM-dd), used by ONCE. */
    val onceDate: String? = null,
    val enabled: Boolean = true,
    /** Overrides the default wording when set. */
    val customMessage: String? = null,
    val delivery: Set<Delivery> = setOf(Delivery.NOTIFICATION),
    val snoozeMinutes: Int = 10,
) {
    init {
        require(id.isNotBlank()) { "id" }
        require(name.isNotBlank()) { "name" }
        require(timeMin in 0..1439) { "time" }
        require(snoozeMinutes in 1..240) { "snooze" }
        when (repeat) {
            RepeatType.WEEKLY -> require(days.isNotEmpty()) { "weekly reminder needs at least one day" }
            RepeatType.ONCE -> require(onceDate != null) { "one-time reminder needs a date" }
            RepeatType.DAILY -> Unit
        }
    }

    fun message(defaultPrefix: String): String = customMessage?.takeIf { it.isNotBlank() } ?: "$defaultPrefix $name"
}

/** Mutable bits kept apart from the definition. */
data class ReminderState(val completedDays: Set<String> = emptySet(), val snoozedUntil: Long = 0L)
