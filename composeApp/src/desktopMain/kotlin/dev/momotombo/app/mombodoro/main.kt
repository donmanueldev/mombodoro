package dev.momotombo.app.mombodoro

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import dev.momotombo.app.mombodoro.data.FocusTaskRepository
import dev.momotombo.app.mombodoro.data.PomodoroSession
import dev.momotombo.app.mombodoro.presentation.FocusTasksController
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsAction
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.mombo_app_icon
import pomodoro.composeapp.generated.resources.mombo_status_icon
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    if (System.getProperty("os.name") == "Mac OS X") {
        System.setProperty("apple.awt.application.name", "Mombodoro")
    }

    launchMombodoro()
}

private const val MinimumWindowWidth = 760
private const val MinimumWindowHeight = 720

private fun launchMombodoro() = application {
    val isMacOS = System.getProperty("os.name") == "Mac OS X"
    val windowState = rememberWindowState(size = DpSize(1280.dp, 820.dp))
    val trayState = rememberTrayState()
    val timerState = remember { PomodoroTimerState() }
    val taskController = remember { FocusTasksController() }
    var isWindowActive by remember { mutableStateOf(false) }
    var shouldActivateWindow by remember { mutableStateOf(false) }
    val macMenuBarHostStart = remember {
        if (isMacOS) MacMenuBarHost.start() else null
    }
    val macMenuBarHost = macMenuBarHostStart?.getOrNull()
    var isMacMenuBarAvailable by remember { mutableStateOf(macMenuBarHost != null) }
    var notificationSystemMessage by remember {
        mutableStateOf(
            if (macMenuBarHostStart?.isFailure == true) {
                "No se pudo iniciar la integración de macOS. Los avisos usarán el sistema alternativo."
            } else {
                null
            }
        )
    }
    var notificationSettings by remember {
        mutableStateOf(
            if (!isMacOS) null
            else NotificationSettingsState(
                permission = if (isMacMenuBarAvailable) MacNotificationStatus.Checking else null,
                test = if (macMenuBarHostStart?.isFailure == true) {
                    MacNotificationTestStatus.Failed
                } else {
                    MacNotificationTestStatus.Idle
                },
            )
        )
    }

    DisposableEffect(macMenuBarHost) {
        onDispose { macMenuBarHost?.close() }
    }

    LaunchedEffect(taskController) {
        taskController.load(withContext(kotlinx.coroutines.Dispatchers.IO) { FocusTaskRepository.openDefault() })
    }

    SideEffect {
        if (isMacMenuBarAvailable) macMenuBarHost?.update(timerState.session)
    }

    LaunchedEffect(macMenuBarHost) {
        macMenuBarHost?.actions?.collect { action ->
            when (action) {
                MacMenuBarAction.Show -> {
                    windowState.isMinimized = false
                    shouldActivateWindow = true
                }
                MacMenuBarAction.Hide -> windowState.isMinimized = true
                MacMenuBarAction.Exit -> exitApplication()
                MacMenuBarAction.NotificationOpened -> {
                    timerState.dismissNotification()
                    windowState.isMinimized = false
                    shouldActivateWindow = true
                }

                MacMenuBarAction.NotificationPermissionGranted -> {
                    notificationSettings = notificationSettings?.copy(permission = MacNotificationStatus.Enabled)
                }

                MacMenuBarAction.NotificationPermissionDenied -> {
                    notificationSettings = notificationSettings?.copy(permission = MacNotificationStatus.Disabled)
                    notificationSystemMessage =
                        "Activa las notificaciones para recibir los avisos de Mombodoro."
                }

                MacMenuBarAction.NotificationDeliveryFailed -> {
                    notificationSystemMessage =
                        "Mombodoro no pudo mostrar el aviso. Revisa las notificaciones."
                }

                MacMenuBarAction.NotificationTestDelivered -> {
                    notificationSettings = notificationSettings?.copy(
                        permission = MacNotificationStatus.Enabled,
                        test = MacNotificationTestStatus.Delivered,
                    )
                }

                MacMenuBarAction.NotificationTestDenied -> {
                    notificationSettings = notificationSettings?.copy(
                        permission = MacNotificationStatus.Disabled,
                        test = MacNotificationTestStatus.Denied,
                    )
                }

                MacMenuBarAction.NotificationTestFailed -> {
                    notificationSettings = notificationSettings?.copy(test = MacNotificationTestStatus.Failed)
                }

                MacMenuBarAction.HostFailed -> {
                    isMacMenuBarAvailable = false
                    macMenuBarHost.close()
                    notificationSettings = notificationSettings?.copy(
                        permission = null,
                        test = MacNotificationTestStatus.Failed,
                    )
                    notificationSystemMessage =
                        "La integración de macOS dejó de responder. Los avisos usarán el sistema alternativo."
                }
            }
        }
    }

    LaunchedEffect(timerState.notification?.requiresAttention) {
        MacDockBadge.update(timerState.notification?.requiresAttention == true)
    }

    LaunchedEffect(isWindowActive) {
        if (isWindowActive && timerState.notification?.requiresAttention == true) {
            timerState.dismissNotification()
        }
    }

    LaunchedEffect(timerState.session?.isRunning, timerState.session?.speed) {
        while (timerState.session?.isRunning == true) {
            val runningSession = timerState.session ?: break
            delay(runningSession.speed.delayMillis.milliseconds)
            if (timerState.session?.isRunning != true) break
            timerState.tick()
        }
    }

    LaunchedEffect(timerState.notification) {
        val notification = timerState.notification ?: return@LaunchedEffect
        if (macMenuBarHost != null && isMacMenuBarAvailable) {
            macMenuBarHost.notify(notification)
        } else {
            trayState.sendNotification(
                Notification(
                    title = notification.title,
                    message = notification.message,
                    type = Notification.Type.Info,
                )
            )
        }
    }

    if (!isMacMenuBarAvailable) {
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
        DisposableEffect(window) {
            val listener = object : WindowAdapter() {
                override fun windowGainedFocus(event: WindowEvent) {
                    isWindowActive = true
                }

                override fun windowLostFocus(event: WindowEvent) {
                    isWindowActive = false
                }
            }
            window.addWindowFocusListener(listener)
            isWindowActive = window.isFocused
            onDispose { window.removeWindowFocusListener(listener) }
        }

        SideEffect {
            // AWT expects logical screen points; converting Compose dp to pixels here doubles
            // the minimum on Retina displays and pushes the layout outside the visible screen.
            window.minimumSize = Dimension(
                MinimumWindowWidth,
                MinimumWindowHeight,
            )
            if (shouldActivateWindow) {
                window.toFront()
                window.requestFocus()
                shouldActivateWindow = false
            }
        }

        PomodoroApp(
            timerState = timerState,
            taskController = taskController,
            onExit = ::exitApplication,
            showNotificationAlert = isWindowActive && !windowState.isMinimized,
            notificationSystemMessage = notificationSystemMessage,
            onDismissNotificationSystemMessage = { notificationSystemMessage = null },
            notificationSettings = notificationSettings,
            onNotificationAction = { action ->
                when (action) {
                    NotificationSettingsAction.Test -> {
                        if (macMenuBarHost != null && isMacMenuBarAvailable) {
                            notificationSettings = notificationSettings?.copy(
                                test = MacNotificationTestStatus.Testing
                            )
                            macMenuBarHost.testNotification()
                        } else {
                            notificationSettings = notificationSettings?.copy(
                                test = MacNotificationTestStatus.Failed
                            )
                        }
                    }

                    NotificationSettingsAction.OpenSystemSettings -> {
                        macMenuBarHost?.openNotificationSettings()
                    }
                }
            },
        )
    }
}

private fun trayTooltip(session: PomodoroSession): String =
    "Mombodoro · %02d:%02d · %s%s".format(
        session.remainingSeconds / 60,
        session.remainingSeconds % 60,
        session.phaseTitle,
        if (session.isRunning) "" else " · Pausado",
    )
