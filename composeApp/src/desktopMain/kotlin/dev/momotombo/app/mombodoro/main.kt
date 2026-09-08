package dev.momotombo.app.mombodoro

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.*
import dev.momotombo.app.mombodoro.data.TimerEvent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.mombo_app_icon
import pomodoro.composeapp.generated.resources.mombo_status_icon
import kotlin.time.Duration.Companion.milliseconds

fun main() = application {
    val windowState = rememberWindowState()
    val trayState = rememberTrayState()
    val timerState = remember { PomodoroTimerState() }
    val macMenuBarHost = remember { if (System.getProperty("os.name") == "Mac OS X") MacMenuBarHost.start() else null }

    DisposableEffect(macMenuBarHost) {
        onDispose { macMenuBarHost?.close() }
    }

    SideEffect { macMenuBarHost?.update(timerState.session) }

    LaunchedEffect(macMenuBarHost) {
        macMenuBarHost?.actions?.collect { action ->
            when (action) {
                MacMenuBarAction.Toggle -> timerState.toggleRunning()
                MacMenuBarAction.Show -> windowState.isMinimized = false
                MacMenuBarAction.Exit -> exitApplication()
            }
        }
    }

    LaunchedEffect(timerState.session?.isRunning, timerState.session?.speed) {
        while (timerState.session?.isRunning == true) {
            val runningSession = timerState.session ?: break
            delay(runningSession.speed.delayMillis.milliseconds)
            if (timerState.session?.isRunning != true) break

            val event = timerState.tick()
            if (event is TimerEvent.PhaseCompleted) {
                val notification = timerState.notification ?: continue
                trayState.sendNotification(
                    Notification(
                        title = notification.title,
                        message = notification.message,
                        type = Notification.Type.Info,
                    )
                )
            }
        }
    }

    if (macMenuBarHost == null) {
        Tray(
            state = trayState,
            icon = painterResource(Res.drawable.mombo_status_icon),
            tooltip = timerState.session?.let(::trayTooltip) ?: "Mombodoro",
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

private fun trayTooltip(session: dev.momotombo.app.mombodoro.data.PomodoroSession): String =
    "Mombodoro · %02d:%02d · %s%s".format(
        session.remainingSeconds / 60,
        session.remainingSeconds % 60,
        session.phaseTitle,
        if (session.isRunning) "" else " · Pausado",
    )
