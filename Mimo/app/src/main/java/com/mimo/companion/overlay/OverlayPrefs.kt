package com.mimo.companion.overlay

import android.content.Context

/** Position and size of the floating companion (plain SharedPreferences: tiny, local, synchronous). */
class OverlayPrefs(context: Context) {
    private val p = context.getSharedPreferences("overlay", Context.MODE_PRIVATE)
    var x: Int get() = p.getInt("x", 24); set(v) = p.edit().putInt("x", v).apply()
    var y: Int get() = p.getInt("y", 400); set(v) = p.edit().putInt("y", v).apply()
    /** Companion width in dp. */
    var sizeDp: Int get() = p.getInt("sizeDp", 120); set(v) = p.edit().putInt("sizeDp", v.coerceIn(60, 260)).apply()
    /** 0.3 .. 1.0 */
    var alpha: Float get() = p.getFloat("alpha", 1f); set(v) = p.edit().putFloat("alpha", v.coerceIn(0.3f, 1f)).apply()
}
