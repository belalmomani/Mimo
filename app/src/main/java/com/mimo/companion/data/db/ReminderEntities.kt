package com.mimo.companion.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val timeMin: Int,
    val repeatType: String,
    /** Bit i = DayOfWeek with value i+1 (Mon=bit0 ... Sun=bit6). */
    val daysMask: Int,
    val onceDate: String?,
    val enabled: Boolean,
    val customMessage: String?,
    /** bit0 notification, bit1 speech, bit2 visual bubble. */
    val deliveryMask: Int,
    val snoozeMinutes: Int,
    val snoozedUntil: Long,
)

@Entity(tableName = "reminder_done", primaryKeys = ["reminderId", "dayKey"])
data class ReminderDoneEntity(val reminderId: String, val dayKey: String)
