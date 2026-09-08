package dev.momotombo.app.mombodoro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.donmanuel.app.pomodoro.data.Pomodoro
import dev.donmanuel.app.pomodoro.data.PomodoroConfiguration
import dev.donmanuel.app.pomodoro.presentation.components.CustomFocusDialog
import dev.donmanuel.app.pomodoro.presentation.components.FocusTypeSelector
import dev.donmanuel.app.pomodoro.presentation.components.NotificationAlert
import dev.donmanuel.app.pomodoro.presentation.view.desktop.PomodoroDesktopLayout
import dev.donmanuel.app.pomodoro.presentation.view.mobile.PomodoroMobileLayout
import dev.donmanuel.app.pomodoro.utils.CustomDialog
import dev.donmanuel.app.pomodoro.utils.platform

@Composable
fun FrameWindowScope.PomodoroApp(timerState: PomodoroTimerState, onExit: () -> Unit) {
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowSettingsDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    fun selectFocusType(configuration: PomodoroConfiguration) {
        timerState.selectFocusType(configuration)
        isShowDialog = false
        isShowSettingsDialog = false
        showCustomDialog = false
    }

    fun showFocusTypeSelector() {
        timerState.clearSession()
        isShowDialog = false
        isShowSettingsDialog = false
        showCustomDialog = false
    }

    MenuBar {
        Menu("Sesión") {
            Item("Nuevo enfoque", onClick = ::showFocusTypeSelector)
            Item(
                if (timerState.session?.isRunning == true) "Pausar temporizador" else "Iniciar temporizador",
                onClick = timerState::toggleRunning,
            )
            Item(
                "Reiniciar sesión",
                onClick = {
                    timerState.restart()
                },
            )
            Separator()
            Item("Salir", onClick = onExit)
        }
        Menu("Opciones") {
            Item("Ajustes", onClick = { if (timerState.session != null) isShowSettingsDialog = true })
        }
        Menu("Ayuda") {
            Item("Acerca de", onClick = { isShowDialog = true })
        }
    }

    val activeSession = timerState.session

    MaterialTheme {
        timerState.notification?.let { activeNotification ->
            NotificationAlert(
                isVisible = true,
                title = activeNotification.title,
                message = activeNotification.message,
                backgroundColor = activeNotification.phase.backgroundColor,
                textColor = activeNotification.phase.textColor,
                onDismiss = timerState::dismissNotification,
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
                timerState.saveSettings(configuration)
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
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
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
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
                    onAbout = { isShowDialog = true },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            }
        }
    }
}
