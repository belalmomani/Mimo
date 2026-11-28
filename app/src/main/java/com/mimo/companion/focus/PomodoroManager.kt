package com.mimo.companion.focus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PomodoroPhase { IDLE, WORK, BREAK }

data class PomodoroState(val phase: PomodoroPhase, val secondsLeft: Int, val round: Int)

class PomodoroManager(
    private val workMinutes: Int = 25,
    private val breakMinutes: Int = 5
) {
    private val _state = MutableStateFlow(PomodoroState(PomodoroPhase.IDLE, workMinutes * 60, 0))
    val state: StateFlow<PomodoroState> = _state

    var onWorkStart: (() -> Unit)? = null
    var onBreakStart: (() -> Unit)? = null
    var onSessionDone: (() -> Unit)? = null

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (_state.value.phase != PomodoroPhase.IDLE) return
        job = scope.launch { runWork() }
    }

    fun stop() {
        job?.cancel()
        _state.value = PomodoroState(PomodoroPhase.IDLE, workMinutes * 60, 0)
    }

    private suspend fun runWork() {
        val round = _state.value.round + 1
        onWorkStart?.invoke()
        for (s in workMinutes * 60 downTo 1) {
            _state.value = PomodoroState(PomodoroPhase.WORK, s, round)
            delay(1000)
        }
        onSessionDone?.invoke()
        runBreak(round)
    }

    private suspend fun runBreak(round: Int) {
        onBreakStart?.invoke()
        for (s in breakMinutes * 60 downTo 1) {
            _state.value = PomodoroState(PomodoroPhase.BREAK, s, round)
            delay(1000)
        }
        _state.value = PomodoroState(PomodoroPhase.IDLE, workMinutes * 60, round)
    }
}
