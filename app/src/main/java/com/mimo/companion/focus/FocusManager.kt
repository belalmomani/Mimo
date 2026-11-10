package com.mimo.companion.focus

import com.mimo.companion.domain.focus.FocusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the Focus Mode session in memory (no persistence — focus resets on restart by design).
 *
 * Thread-safe: all mutations go through [MutableStateFlow].
 *
 * Usage:
 * - [start] to begin a session (optional duration)
 * - [stop]  to end it manually
 * - [tick]  called by the UI/overlay to auto-stop expired timed sessions
 * - [state] observed by the overlay and [ContextEngine]
 */
class FocusManager {

    private val _state = MutableStateFlow(FocusState())
    val state: StateFlow<FocusState> get() = _state

    val isActive: Boolean get() = _state.value.active

    /** Start a new focus session. If one is already running it is replaced. */
    fun start(durationMinutes: Int? = null, nowMillis: Long = System.currentTimeMillis()) {
        _state.value = FocusState(
            active = true,
            startedAtMillis = nowMillis,
            durationMinutes = durationMinutes,
        )
    }

    /** End the current session immediately. */
    fun stop() { _state.value = FocusState() }

    /**
     * Call periodically (e.g. every minute) to auto-stop expired timed sessions.
     * Returns true if the session was just stopped.
     */
    fun tick(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val s = _state.value
        return if (s.isExpired(nowMillis)) { stop(); true } else false
    }
}
