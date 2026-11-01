package com.mimo.companion.data.db

import androidx.room.Entity

@Entity(tableName = "prayer_done", primaryKeys = ["dayKey", "prayer"])
data class PrayerDoneEntity(val dayKey: String, val prayer: String)

@Entity(tableName = "prayer_state", primaryKeys = ["dayKey", "prayer"])
data class PrayerStateEntity(val dayKey: String, val prayer: String, val sentCount: Int, val nextAt: Long?)
