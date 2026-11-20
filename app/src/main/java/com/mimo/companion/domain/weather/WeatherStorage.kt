package com.mimo.companion.domain.weather

/**
 * Persistence contract for the last-known weather snapshot.
 * Implementations live outside the domain layer (SharedPrefs, Room…).
 *
 * Pure Kotlin — no Android imports.
 */
interface WeatherStorage {
    fun load(): WeatherData?
    fun save(data: WeatherData)
}
