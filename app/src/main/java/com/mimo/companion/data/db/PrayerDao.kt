package com.mimo.companion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrayerDao {
    @Query("SELECT prayer FROM prayer_done WHERE dayKey = :dayKey") fun done(dayKey: String): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun addDone(e: PrayerDoneEntity): Long
    @Query("DELETE FROM prayer_done WHERE dayKey = :dayKey AND prayer = :prayer") fun removeDone(dayKey: String, prayer: String)
    @Query("SELECT * FROM prayer_state WHERE dayKey = :dayKey AND prayer = :prayer") fun state(dayKey: String, prayer: String): PrayerStateEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun putState(e: PrayerStateEntity)
    @Query("DELETE FROM prayer_done WHERE dayKey < :dayKey") fun pruneDone(dayKey: String)
    @Query("DELETE FROM prayer_state WHERE dayKey < :dayKey") fun pruneState(dayKey: String)
    @Query("DELETE FROM prayer_done") fun clearDone()
    @Query("DELETE FROM prayer_state") fun clearState()
}
