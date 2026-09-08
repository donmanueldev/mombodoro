package dev.donmanuel.app.pomodoro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.donmanuel.app.pomodoro.data.Pomodoro
import dev.donmanuel.app.pomodoro.data.PomodoroConfiguration
import dev.donmanuel.app.pomodoro.data.PomodoroSession
import dev.donmanuel.app.pomodoro.data.TimerEvent
import dev.donmanuel.app.pomodoro.presentation.components.CustomFocusDialog
import dev.donmanuel.app.pomodoro.presentation.components.FocusTypeSelector
import dev.donmanuel.app.pomodoro.presentation.components.NotificationAlert
import dev.donmanuel.app.pomodoro.presentation.view.desktop.PomodoroDesktopLayout
import dev.donmanuel.app.pomodoro.presentation.view.mobile.PomodoroMobileLayout
import dev.donmanuel.app.pomodoro.utils.CustomDialog
import dev.donmanuel.app.pomodoro.utils.platform
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private data class TimerNotification(
    val phase: Pomodoro,
    val title: String,
    val message: String,
)

@Composable
fun FrameWindowScope.PomodoroApp(onExit: () -> Unit) {
    var session by remember { mutableStateOf<PomodoroSession?>(null) }
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowSettingsDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var notification by remember { mutableStateOf<TimerNotification?>(null) }

    fun selectFocusType(configuration: PomodoroConfiguration) {
        session = PomodoroSession.start(configuration)
        isShowDialog = false
        isShowSettingsDialog = false
        showCustomDialog = false
        notification = null
    }

    fun showFocusTypeSelector() {
        session = null
        notification = null
        isShowDialog = false
        isShowSettingsDialog = false
        showCustomDialog = false
    }

    MenuBar {
        Menu("Sesión") {
            Item("Nuevo enfoque", onClick = ::showFocusTypeSelector)
            Item(
                if (session?.isRunning == true) "Pausar temporizador" else "Iniciar temporizador",
                onClick = { session = session?.toggleRunning() },
            )
            Item(
                "Reiniciar sesión",
                onClick = {
                    session = session?.let { PomodoroSession.start(it.configuration) }
                    notification = null
                },
            )
            Separator()
            Item("Salir", onClick = onExit)
        }
        Menu("Opciones") {
            Item("Ajustes", onClick = { if (session != null) isShowSettingsDialog = true })
        }
        Menu("Ayuda") {
            Item("Acerca de", onClick = { isShowDialog = true })
        }
    }

    LaunchedEffect(session?.isRunning, session?.speed) {
        while (session?.isRunning == true) {
            val runningSession = session ?: break
            delay(runningSession.speed.delayMillis.milliseconds)
            val currentSession = session ?: break
            if (!currentSession.isRunning) break

            val result = currentSession.tick()
            session = result.session
            notification = result.event?.toNotification(currentSession.configuration)
        }
    }

    val activeSession = session

    MaterialTheme {
        notification?.let { activeNotification ->
            NotificationAlert(
                isVisible = true,
                title = activeNotification.title,
                message = activeNotification.message,
                backgroundColor = activeNotification.phase.backgroundColor,
                textColor = activeNotification.phase.textColor,
                onDismiss = { notification = null },
            )
        }

        val dialogPhase = activeSession?.phase ?: Pomodoro.FOCUS
        if (isShowDialog) {
            CustomDialog(
                textColor = dialogPhase.textColor,
                backgroundColor = dialogPhase.backgroundColor,
                onCloseDialog = { isShowDialog = false },
            )
        }

        if (activeSession == null) {
            FocusTypeSelector(
                onSelectFocusType = { title, focusTime, shortBreakTime, longBreakTime, cycles ->
                    if (title == "Personalizado") {
                        showCustomDialog = true
                    } else {
                        selectFocusType(
                            PomodoroConfiguration(title, focusTime, shortBreakTime, longBreakTime, cycles)
                        )
                    }
                }
            )

            if (showCustomDialog) {
                CustomFocusDialog(
                    onDismiss = { showCustomDialog = false },
                    onConfirm = { title, focusTime, shortBreakTime, longBreakTime, cycles ->
                        selectFocusType(
                            PomodoroConfiguration(title, focusTime, shortBreakTime, longBreakTime, cycles)
                        )
                    }
                )
            }
        } else {
            val layout = activeSession
            val onSaveSettings: (PomodoroConfiguration) -> Unit = { configuration ->
                session = layout.restartWith(configuration)
                notification = null
            }
            if (platform().isDesktop) {
                PomodoroDesktopLayout(
                    pomodoro = layout.phase,
                    phaseTitle = layout.phaseTitle,
                    isPlayPomodoro = layout.isRunning,
                    timerLeft = layout.remainingSeconds,
                    speedTime = layout.speed,
                    isShowSettingsDialog = isShowSettingsDialog,
                    configuration = layout.configuration,
                    completedPomodoros = layout.completedFocusSessions,
                    onPlayPause = { session = layout.toggleRunning() },
                    onSpeedChange = { session = layout.copy(speed = it) },
                    onAbout = { isShowDialog = true },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            } else {
                PomodoroMobileLayout(
                    pomodoro = layout.phase,
                    phaseTitle = layout.phaseTitle,
                    isPlayPomodoro = layout.isRunning,
                    timerLeft = layout.remainingSeconds,
                    speedTime = layout.speed,
                    isShowSettingsDialog = isShowSettingsDialog,
                    configuration = layout.configuration,
                    completedPomodoros = layout.completedFocusSessions,
                    onPlayPause = { session = layout.toggleRunning() },
                    onSpeedChange = { session = layout.copy(speed = it) },
                    onAbout = { isShowDialog = true },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            }
        }
    }
}

private fun TimerEvent.toNotification(configuration: PomodoroConfiguration): TimerNotification = when (this) {
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

private fun Pomodoro.displayTitle(configuration: PomodoroConfiguration): String =
    if (this == Pomodoro.FOCUS) "Focus · ${configuration.name}" else title
