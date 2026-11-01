package com.mimo.companion.domain.prayer

import java.time.ZoneId

data class City(val id: String, val ar: String, val en: String, val lat: Double, val lng: Double, val zoneId: String) {
    val zone: ZoneId get() = ZoneId.of(zoneId)
}

/** Built-in city list (manual choice; no GPS involved). Add a row to support another city. */
object Cities {
    val ALL = listOf(
        City("amman", "عمّان", "Amman", 31.9539, 35.9106, "Asia/Amman"),
        City("zarqa", "الزرقاء", "Zarqa", 32.0728, 36.0876, "Asia/Amman"),
        City("irbid", "إربد", "Irbid", 32.5556, 35.8500, "Asia/Amman"),
        City("aqaba", "العقبة", "Aqaba", 29.5321, 35.0063, "Asia/Amman"),
        City("salt", "السلط", "As-Salt", 32.0392, 35.7272, "Asia/Amman"),
        City("madaba", "مادبا", "Madaba", 31.7167, 35.7936, "Asia/Amman"),
        City("jerash", "جرش", "Jerash", 32.2747, 35.8961, "Asia/Amman"),
        City("karak", "الكرك", "Karak", 31.1853, 35.7048, "Asia/Amman"),
        City("maan", "معان", "Ma'an", 30.1962, 35.7341, "Asia/Amman"),
        City("mafraq", "المفرق", "Mafraq", 32.3406, 36.2080, "Asia/Amman"),
        City("ajloun", "عجلون", "Ajloun", 32.3326, 35.7517, "Asia/Amman"),
        City("tafilah", "الطفيلة", "Tafilah", 30.8375, 35.6042, "Asia/Amman"),
        City("jerusalem", "القدس", "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem"),
        City("mecca", "مكة المكرمة", "Mecca", 21.4225, 39.8262, "Asia/Riyadh"),
        City("medina", "المدينة المنورة", "Medina", 24.4686, 39.6142, "Asia/Riyadh"),
        City("riyadh", "الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        City("jeddah", "جدة", "Jeddah", 21.5433, 39.1728, "Asia/Riyadh"),
        City("cairo", "القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"),
        City("dubai", "دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
        City("baghdad", "بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad"),
        City("damascus", "دمشق", "Damascus", 33.5138, 36.2765, "Asia/Damascus"),
        City("beirut", "بيروت", "Beirut", 33.8938, 35.5018, "Asia/Beirut"),
        City("kuwait", "الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"),
        City("doha", "الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar"),
        City("istanbul", "إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
        City("london", "لندن", "London", 51.5074, -0.1278, "Europe/London"),
        City("newyork", "نيويورك", "New York", 40.7128, -74.0060, "America/New_York"),
    )
    fun byId(id: String): City = ALL.firstOrNull { it.id == id } ?: ALL.first()
    init { require(ALL.map { it.id }.toSet().size == ALL.size) }
}

/** Everything the user can tune about prayer times and prayer reminders. Stored locally only. */
data class PrayerSettings(
    val enabled: Boolean = false,
    val cityId: String = "amman",
    val method: CalcMethod = CalcMethod.MUSLIM_WORLD_LEAGUE,
    val asr: AsrMethod = AsrMethod.STANDARD,
    /** Per-prayer shift in minutes (to match a local timetable). */
    val adjustMin: Map<Prayer, Int> = emptyMap(),
    /** Which prayers to be reminded about. */
    val remindFor: Set<Prayer> = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
    /** Remind this many minutes BEFORE the time (0 = at the time). */
    val minutesBefore: Int = 10,
    /** Total reminders per prayer (first one + follow-ups). */
    val reminderCount: Int = 1,
    /** Gap between follow-ups and the snooze length. */
    val gapMinutes: Int = 10,
    /** Overrides the default wording when not blank. */
    val customMessage: String? = null,
) {
    init {
        require(minutesBefore in 0..60 && reminderCount in 1..3 && gapMinutes in 1..60)
        require(adjustMin.values.all { it in -30..30 })
        require(remindFor.all { it.isPrayer })
    }
}

data class PrayerTrigger(val dayKey: String, val prayer: Prayer, val atMillis: Long)
data class PrayerState(val sentCount: Int = 0, val nextAtMillis: Long? = null)
