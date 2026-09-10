package dev.momotombo.app.mombodoro.data

data class PomodoroConfiguration(
    val name: String,
    val focusSeconds: Int,
    val shortBreakSeconds: Int,
    val longBreakSeconds: Int,
    val cyclesBeforeLongBreak: Int,
) {
    init {
        require(name.isNotBlank()) { "A focus type needs a name." }
        require(focusSeconds > 0) { "Focus duration must be positive." }
        require(shortBreakSeconds > 0) { "Short-break duration must be positive." }
        require(longBreakSeconds > 0) { "Long-break duration must be positive." }
        require(cyclesBeforeLongBreak > 0) { "Cycles must be positive." }
    }

    fun durationFor(phase: Pomodoro): Int = when (phase) {
        Pomodoro.FOCUS -> focusSeconds
        Pomodoro.BREAK -> shortBreakSeconds
        Pomodoro.LONG_BREAK -> longBreakSeconds
    }
}

enum class TimerSpeed(val delayMillis: Long) {
    NORMAL(1_000L),
    FAST(300L),
}

sealed interface TimerEvent {
    data class TimeWarning(val phase: Pomodoro, val secondsRemaining: Int) : TimerEvent
    data class PhaseCompleted(val phase: Pomodoro) : TimerEvent
}

data class TimerTickResult(
    val session: PomodoroSession,
    val event: TimerEvent? = null,
)

data class PomodoroSession(
    val configuration: PomodoroConfiguration,
    val phase: Pomodoro,
    val remainingSeconds: Int,
    val completedFocusSessions: Int,
    val isRunning: Boolean,
    val speed: TimerSpeed = TimerSpeed.NORMAL,
) {
    companion object {
        fun start(configuration: PomodoroConfiguration): PomodoroSession = PomodoroSession(
            configuration = configuration,
            phase = Pomodoro.FOCUS,
            remainingSeconds = configuration.focusSeconds,
            completedFocusSessions = 0,
            isRunning = false,
        )
    }

    val phaseTitle: String
        get() = if (phase == Pomodoro.FOCUS) "Enfoque · ${configuration.name}" else phase.title

    fun toggleRunning(): PomodoroSession = copy(isRunning = !isRunning)

    fun changeSpeed(speed: TimerSpeed): PomodoroSession = copy(speed = speed)

    fun restartWith(configuration: PomodoroConfiguration): PomodoroSession = start(configuration)

    fun switchTo(phase: Pomodoro): PomodoroSession = copy(
        phase = phase,
        remainingSeconds = configuration.durationFor(phase),
        isRunning = false,
    )

    fun tick(): TimerTickResult = advanceBy(1)

    fun advanceBy(elapsedSeconds: Int): TimerTickResult {
        require(elapsedSeconds >= 0) { "Elapsed seconds cannot be negative." }
        if (!isRunning || remainingSeconds <= 0 || elapsedSeconds == 0) return TimerTickResult(this)

        val updatedRemainingSeconds = (remainingSeconds - elapsedSeconds).coerceAtLeast(0)
        if (updatedRemainingSeconds > 0) {
            val event = listOf(5 * 60, 3 * 60)
                .lastOrNull { threshold -> remainingSeconds > threshold && updatedRemainingSeconds <= threshold }
                ?.let { threshold -> TimerEvent.TimeWarning(phase, threshold) }
            return TimerTickResult(copy(remainingSeconds = updatedRemainingSeconds), event)
        }

        val nextPhase: Pomodoro
        val nextCompletedFocusSessions: Int
        if (phase == Pomodoro.FOCUS) {
            val completed = completedFocusSessions + 1
            if (completed >= configuration.cyclesBeforeLongBreak) {
                nextPhase = Pomodoro.LONG_BREAK
                nextCompletedFocusSessions = 0
            } else {
                nextPhase = Pomodoro.BREAK
                nextCompletedFocusSessions = completed
            }
        } else {
            nextPhase = Pomodoro.FOCUS
            nextCompletedFocusSessions = completedFocusSessions
        }

        return TimerTickResult(
            session = copy(
                phase = nextPhase,
                remainingSeconds = configuration.durationFor(nextPhase),
                completedFocusSessions = nextCompletedFocusSessions,
                isRunning = false,
            ),
            event = TimerEvent.PhaseCompleted(phase),
        )
    }
}
