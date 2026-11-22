package com.mimo.companion.focus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PomodoroPhase { IDLE, WORK, BREAK }

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val secondsLeft: Int = 0,
    val round: Int = 0,
)

/**
 * Manages a Pomodoro timer loop.
 * Work = [workMinutes] minutes, Break = [breakMinutes] minutes.
 * Callers observe [state] and pass callbacks for phase transitions.
 */
class PomodoroManager(
    private val workMinutes: Int = 25,
    private val breakMinutes: Int = 5,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> get() = _state

    var onWorkStart: (() -> Unit)? = null
    var onBreakStart: (() -> Unit)? = null
    var onSessionDone: (() -> Unit)? = null

    fun start() {
        if (_state.value.phase != PomodoroPhase.IDLE) return
        runWorkPhase(round = 1)
    }

    fun stop() {
        timerJob?.cancel()
        _state.value = PomodoroState()
    }

    val isRunning get() = _state.value.phase != PomodoroPhase.IDLE

    private fun runWorkPhase(round: Int) {
        timerJob?.cancel()
        _state.value = PomodoroState(PomodoroPhase.WORK, workMinutes * 60, round)
        onWorkStart?.invoke()
        timerJob = scope.launch {
            tick(workMinutes * 60) { secondsLeft ->
                _state.value = _state.value.copy(secondsLeft = secondsLeft)
            }
            runBreakPhase(round)
        }
    }

    private fun runBreakPhase(round: Int) {
        _state.value = PomodoroState(PomodoroPhase.BREAK, breakMinutes * 60, round)
        onBreakStart?.invoke()
        timerJob = scope.launch {
            tick(breakMinutes * 60) { secondsLeft ->
                _state.value = _state.value.copy(secondsLeft = secondsLeft)
            }
            if (isActive) {
                onSessionDone?.invoke()
                runWorkPhase(round + 1)
            }
        }
    }

    private suspend fun tick(totalSeconds: Int, onTick: (Int) -> Unit) {
        for (s in totalSeconds downTo 1) {
            onTick(s)
            delay(1_000L)
        }
        onTick(0)
    }
}
