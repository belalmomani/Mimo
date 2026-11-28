package com.mimo.companion.store

import android.content.Context
import android.content.SharedPreferences

class ShapeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mimo_shapes", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE = "active_shape"
        private const val PREFIX_ADS = "ads_"
        private const val PREFIX_OWNED = "owned_"
        private const val PREFIX_UNLOCKED_AT = "unlocked_at_"
    }

    fun getActiveShapeId(): String = prefs.getString(KEY_ACTIVE, "s01") ?: "s01"
    fun setActiveShapeId(id: String) = prefs.edit().putString(KEY_ACTIVE, id).apply()

    fun getAdsWatched(shapeId: String): Int = prefs.getInt("$PREFIX_ADS$shapeId", 0)

    fun incrementAds(shapeId: String): Int {
        val current = getAdsWatched(shapeId) + 1
        prefs.edit().putInt("$PREFIX_ADS$shapeId", current).apply()
        return current
    }

    fun isOwned(shapeId: String): Boolean = prefs.getBoolean("$PREFIX_OWNED$shapeId", false)

    fun setOwned(shapeId: String) {
        prefs.edit()
            .putBoolean("$PREFIX_OWNED$shapeId", true)
            .putLong("$PREFIX_UNLOCKED_AT$shapeId", System.currentTimeMillis())
            .apply()
    }

    fun getUnlockedAt(shapeId: String): Long = prefs.getLong("$PREFIX_UNLOCKED_AT$shapeId", 0L)

    fun getAllOwnerships(): List<ShapeOwnership> {
        return ShapeCatalog.all.map { shape ->
            ShapeOwnership(
                shapeId = shape.id,
                adsWatched = getAdsWatched(shape.id),
                owned = isOwned(shape.id),
                unlockedAt = getUnlockedAt(shape.id)
            )
        }
    }
}
