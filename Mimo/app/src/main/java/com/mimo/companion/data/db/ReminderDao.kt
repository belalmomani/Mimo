package com.mimo.companion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders") fun all(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE id = :id") fun get(id: String): ReminderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(e: ReminderEntity)
    @Query("DELETE FROM reminders WHERE id = :id") fun delete(id: String)
    @Query("DELETE FROM reminder_done WHERE reminderId = :id") fun deleteDone(id: String)
    @Query("SELECT dayKey FROM reminder_done WHERE reminderId = :id") fun doneDays(id: String): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun addDone(e: ReminderDoneEntity): Long
    @Query("UPDATE reminders SET snoozedUntil = :millis WHERE id = :id") fun setSnooze(id: String, millis: Long)
    /** Keeps the table small: completions older than the given day key are useless. */
    @Query("DELETE FROM reminder_done WHERE dayKey < :oldestKeep") fun pruneDone(oldestKeep: String)
}
