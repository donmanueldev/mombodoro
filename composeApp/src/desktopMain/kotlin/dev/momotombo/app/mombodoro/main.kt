package dev.momotombo.app.mombodoro

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import dev.momotombo.app.mombodoro.data.TimerEvent
import dev.momotombo.app.mombodoro.data.FocusTaskRepository
import dev.momotombo.app.mombodoro.presentation.FocusTasksController
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.mombo_app_icon
import pomodoro.composeapp.generated.resources.mombo_status_icon
import java.awt.Dimension
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    if (System.getProperty("os.name") == "Mac OS X") {
        System.setProperty("apple.awt.application.name", "Mombodoro")
    }

    launchMombodoro()
}

private fun launchMombodoro() = application {
    val windowState = rememberWindowState(size = DpSize(1280.dp, 820.dp))
    val trayState = rememberTrayState()
    val timerState = remember { PomodoroTimerState() }
    val taskController = remember { FocusTasksController() }
    var selectedTaskTitle by remember { mutableStateOf<String?>(null) }
    val macMenuBarHost = remember { if (System.getProperty("os.name") == "Mac OS X") MacMenuBarHost.start() else null }

    DisposableEffect(macMenuBarHost) {
        onDispose { macMenuBarHost?.close() }
    }

    LaunchedEffect(taskController) {
        taskController.load(withContext(kotlinx.coroutines.Dispatchers.IO) { FocusTaskRepository.openDefault() })
    }

    LaunchedEffect(macMenuBarHost) {
        val host = macMenuBarHost ?: return@LaunchedEffect
        snapshotFlow { timerState.session to selectedTaskTitle }
            .collect { (session, taskTitle) -> host.update(session, taskTitle) }
    }

    LaunchedEffect(macMenuBarHost) {
        macMenuBarHost?.actions?.collect { action ->
            when (action) {
                MacMenuBarAction.Toggle -> timerState.toggleRunning()
                MacMenuBarAction.Show -> windowState.isMinimized = false
                MacMenuBarAction.Hide -> windowState.isMinimized = true
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
            Item("Ocultar Mombodoro", onClick = { windowState.isMinimized = true })
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
        val density = LocalDensity.current
        SideEffect {
            window.minimumSize = Dimension(
                with(density) { 1024.dp.roundToPx() },
                with(density) { 720.dp.roundToPx() },
            )
        }
        PomodoroApp(
            timerState = timerState,
            taskController = taskController,
            onExit = ::exitApplication,
            onSelectedTaskTitleChange = { selectedTaskTitle = it },
        )
    }
}

private fun trayTooltip(session: dev.momotombo.app.mombodoro.data.PomodoroSession): String =
    "Mombodoro · %02d:%02d · %s%s".format(
        session.remainingSeconds / 60,
        session.remainingSeconds % 60,
        session.phaseTitle,
        if (session.isRunning) "" else " · Pausado",
    )
