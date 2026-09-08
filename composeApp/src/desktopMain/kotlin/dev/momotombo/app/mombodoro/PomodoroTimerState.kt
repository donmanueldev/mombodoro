package dev.momotombo.app.mombodoro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.PomodoroSession
import dev.momotombo.app.mombodoro.data.TimerEvent

data class TimerNotification(
    val phase: Pomodoro,
    val title: String,
    val message: String,
)

/** Single source of truth shared by the window, tray and macOS menu bar. */
class PomodoroTimerState {
    var session by mutableStateOf<PomodoroSession?>(null)
        private set

    var notification by mutableStateOf<TimerNotification?>(null)
        private set

    fun selectFocusType(configuration: PomodoroConfiguration) {
        session = PomodoroSession.start(configuration)
        notification = null
    }

    fun clearSession() {
        session = null
        notification = null
    }

    fun toggleRunning() {
        session = session?.toggleRunning()
    }

    fun restart() {
        session = session?.let { PomodoroSession.start(it.configuration) }
        notification = null
    }

    fun saveSettings(configuration: PomodoroConfiguration) {
        session = session?.restartWith(configuration)
        notification = null
    }

    fun switchPhase(phase: Pomodoro) {
        session = session?.switchTo(phase)
        notification = null
    }

    fun dismissNotification() {
        notification = null
    }

    fun tick(): TimerEvent? {
        val currentSession = session ?: return null
        val result = currentSession.tick()
        session = result.session
        notification = result.event?.toNotification(currentSession.configuration)
        return result.event
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
    )

    is TimerEvent.PhaseCompleted -> TimerNotification(
        phase = phase,
        title = "Tiempo completado · ${phase.displayTitle(configuration)}",
        message = when (phase) {
            Pomodoro.FOCUS -> "Tu sesión de enfoque terminó. Es momento de descansar."
            Pomodoro.BREAK, Pomodoro.LONG_BREAK -> "Tu descanso terminó. Es momento de volver a enfocarte."
        },
    )
}

internal fun Pomodoro.displayTitle(configuration: PomodoroConfiguration): String =
    if (this == Pomodoro.FOCUS) "Enfoque · ${configuration.name}" else title
