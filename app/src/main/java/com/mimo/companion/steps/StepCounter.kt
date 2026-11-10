package com.mimo.companion.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.mimo.companion.domain.steps.StepMath
import com.mimo.companion.domain.steps.StepState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Reads the TYPE_STEP_COUNTER hardware sensor and maintains today's step count.
 *
 * Design decisions:
 * - TYPE_STEP_COUNTER is cumulative since last reboot, never goes backward (reboot = reset).
 * - We persist a daily baseline in SharedPreferences and clear it at midnight.
 * - The listener is registered as SENSOR_DELAY_NORMAL (lazy, low power).
 * - On Android 10+ (API 29) the ACTIVITY_RECOGNITION permission is required;
 *   [state] will carry [StepState.permissionNeeded] = true until it is granted.
 * - If the device has no step-counter sensor [state] will carry [StepState.available] = false.
 *
 * Call [start] when the app is in the foreground and [stop] when it moves to background.
 * Graph.init calls [start] once; the overlay service will keep it alive.
 */
class StepCounter(private val context: Context) {

    private val prefs = context.getSharedPreferences("step_counter", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(StepState())
    val state: StateFlow<StepState> = _state

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var registered = false
    private val goal get() = prefs.getInt(KEY_GOAL, StepState.DEFAULT_GOAL)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val total = event.values[0].toLong()
            ensureDayReset(total)
            val baseline = prefs.getLong(KEY_BASELINE, total)
            _state.value = _state.value.copy(
                todaySteps = StepMath.todaySteps(total, baseline),
                available = true,
                permissionNeeded = false,
            )
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    /** Call once from Graph.init or whenever the permission is granted. */
    fun start() {
        if (sensor == null) { _state.value = StepState(available = false, goal = goal); return }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                _state.value = StepState(available = true, permissionNeeded = true, goal = goal)
                return
            }
        }
        if (!registered) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            registered = true
        }
    }

    fun stop() {
        if (registered) { sm.unregisterListener(listener); registered = false }
    }

    fun setGoal(steps: Int) {
        val clamped = steps.coerceIn(StepState.MIN_GOAL, StepState.MAX_GOAL)
        prefs.edit().putInt(KEY_GOAL, clamped).apply()
        _state.value = _state.value.copy(goal = clamped)
    }

    /** Re-check permission after the user grants it in the system dialog. */
    fun onPermissionResult() = start()

    // ---- internals ----

    private fun ensureDayReset(sensorTotal: Long) {
        val today = LocalDate.now().toEpochDay()
        val savedDay = prefs.getLong(KEY_DAY, today)
        if (savedDay != today) {
            val oldBaseline = prefs.getLong(KEY_BASELINE, sensorTotal)
            val newBaseline = StepMath.newDayBaseline(sensorTotal, oldBaseline)
            prefs.edit().putLong(KEY_BASELINE, newBaseline).putLong(KEY_DAY, today).apply()
        } else if (!prefs.contains(KEY_BASELINE)) {
            // First ever reading — capture baseline
            prefs.edit().putLong(KEY_BASELINE, sensorTotal).putLong(KEY_DAY, today).apply()
        }
    }

    companion object {
        private const val KEY_BASELINE = "baseline"
        private const val KEY_DAY = "day_epoch"
        private const val KEY_GOAL = "goal"
    }
}
