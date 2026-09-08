package dev.momotombo.app.mombodoro

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import dev.donmanuel.app.pomodoro.data.TimerEvent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.mombo_app_icon
import pomodoro.composeapp.generated.resources.mombo_status_icon

fun main() = application {
    val windowState = rememberWindowState()
    val trayState = rememberTrayState()
    val timerState = remember { PomodoroTimerState() }
    val isMacOs = System.getProperty("os.name") == "Mac OS X"

    LaunchedEffect(timerState.session?.isRunning, timerState.session?.speed) {
        while (timerState.session?.isRunning == true) {
            val runningSession = timerState.session ?: break
            delay(runningSession.speed.delayMillis)
            if (timerState.session?.isRunning != true) break

            val event = timerState.tick()
            if (event is TimerEvent.PhaseCompleted) {
                val notification = timerState.notification ?: continue
                if (isMacOs) {
                    showMacSystemNotification(notification)
                } else {
                    trayState.sendNotification(
                        androidx.compose.ui.window.Notification(
                            title = notification.title,
                            message = notification.message,
                            type = androidx.compose.ui.window.Notification.Type.Info,
                        )
                    )
                }
            }
        }
    }

    if (isMacOs) {
        MacMenuBar(
            session = timerState.session,
            onToggleTimer = timerState::toggleRunning,
            onShowWindow = { windowState.isMinimized = false },
            onExit = ::exitApplication,
        )
    } else {
        Tray(
            state = trayState,
            icon = painterResource(Res.drawable.mombo_status_icon),
            tooltip = timerState.session?.let { "Mombodoro · ${it.phaseTitle}" } ?: "Mombodoro",
        ) {
            Item("Mostrar Mombodoro", onClick = { windowState.isMinimized = false })
            Item(
                if (timerState.session?.isRunning == true) "Parar temporizador" else "Continuar temporizador",
                enabled = timerState.session != null,
                onClick = timerState::toggleRunning,
            )
            Separator()
            Item("Salir", onClick = ::exitApplication)
        }
    }

    Window(
        state = windowState,
        onCloseRequest = { windowState.isMinimized = true },
        title = "Mombodoro",
        icon = painterResource(Res.drawable.mombo_app_icon),
    ) {
        PomodoroApp(timerState = timerState, onExit = ::exitApplication)
    }
}
