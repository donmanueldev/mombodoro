package dev.momotombo.app.mombodoro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.PomodoroSession
import dev.momotombo.app.mombodoro.data.TimerEvent
import dev.momotombo.app.mombodoro.data.TimerSpeed

internal fun interface MonotonicClock {
    fun nowNanos(): Long
}

private val SystemMonotonicClock = MonotonicClock(System::nanoTime)

data class TimerNotification(
    val phase: Pomodoro,
    val title: String,
    val message: String,
    val requiresAttention: Boolean,
)

/** Single source of truth shared by the window, tray and macOS menu bar. */
class PomodoroTimerState internal constructor(
    private val clock: MonotonicClock = SystemMonotonicClock,
) {
    private var lastUpdateNanos: Long? = null
    private var elapsedRemainderNanos = 0L

    var session by mutableStateOf<PomodoroSession?>(null)
        private set

    var notification by mutableStateOf<TimerNotification?>(null)
        private set

    fun selectFocusType(configuration: PomodoroConfiguration) {
        session = PomodoroSession.start(configuration)
        notification = null
        resetClock()
    }

    fun clearSession() {
        session = null
        notification = null
        resetClock()
    }

    fun toggleRunning() {
        val currentSession = session ?: return
        if (currentSession.isRunning) {
            advanceToNow()
            session = session?.takeIf { it.isRunning }?.toggleRunning() ?: session
            lastUpdateNanos = null
        } else {
            session = currentSession.toggleRunning()
            lastUpdateNanos = clock.nowNanos()
        }
    }

    fun restart() {
        session = session?.let { PomodoroSession.start(it.configuration) }
        notification = null
        resetClock()
    }

    fun saveSettings(configuration: PomodoroConfiguration) {
        session = session?.restartWith(configuration)
        notification = null
        resetClock()
    }

    fun changeSpeed(speed: TimerSpeed) {
        advanceToNow()
        session = session?.changeSpeed(speed)
        if (session?.isRunning == true) lastUpdateNanos = clock.nowNanos()
    }

    fun switchPhase(phase: Pomodoro) {
        session = session?.switchTo(phase)
        notification = null
        resetClock()
    }

    fun dismissNotification() {
        notification = null
    }

    fun tick(): TimerEvent? {
        return advanceToNow()
    }

    private fun advanceToNow(): TimerEvent? {
        val currentSession = session?.takeIf { it.isRunning } ?: return null
        val nowNanos = clock.nowNanos()
        val previousUpdateNanos = lastUpdateNanos ?: nowNanos.also { lastUpdateNanos = it }
        val elapsedNanos = (nowNanos - previousUpdateNanos).coerceAtLeast(0L)
        lastUpdateNanos = nowNanos
        elapsedRemainderNanos += elapsedNanos

        val nanosPerSessionSecond = currentSession.speed.delayMillis * 1_000_000L
        val elapsedSeconds = (elapsedRemainderNanos / nanosPerSessionSecond)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        if (elapsedSeconds == 0) return null

        elapsedRemainderNanos %= nanosPerSessionSecond
        val result = currentSession.advanceBy(elapsedSeconds)
        session = result.session
        notification = result.event?.toNotification(currentSession.configuration)
        if (!result.session.isRunning) resetClock()
        return result.event
    }

    private fun resetClock() {
        lastUpdateNanos = null
        elapsedRemainderNanos = 0L
    }
}

internal fun TimerEvent.toNotification(configuration: PomodoroConfiguration): TimerNotification = when (this) {
    is TimerEvent.TimeWarning -> TimerNotification(
        phase = phase,
        title = "Alerta de tiempo · ${phase.displayTitle(configuration)}",
        message = when (secondsRemaining) {
            5 * 60 -> "Quedan 5 minutos para terminar."
            else -> "Quedan 3 minutos para terminar."
        },
        requiresAttention = false,
    )

    is TimerEvent.PhaseCompleted -> TimerNotification(
        phase = phase,
        title = "Tiempo completado · ${phase.displayTitle(configuration)}",
        message = when (phase) {
            Pomodoro.FOCUS -> "Tu sesión de enfoque terminó. Es momento de descansar."
            Pomodoro.BREAK, Pomodoro.LONG_BREAK -> "Tu descanso terminó. Es momento de volver a enfocarte."
        },
        requiresAttention = true,
    )
}

internal fun Pomodoro.displayTitle(configuration: PomodoroConfiguration): String =
    if (this == Pomodoro.FOCUS) "Enfoque · ${configuration.name}" else title
