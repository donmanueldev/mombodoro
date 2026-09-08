package dev.momotombo.app.mombodoro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.presentation.components.CustomFocusDialog
import dev.momotombo.app.mombodoro.presentation.components.FocusTypeSelector
import dev.momotombo.app.mombodoro.presentation.components.NotificationAlert
import dev.momotombo.app.mombodoro.presentation.FocusTasksController
import dev.momotombo.app.mombodoro.presentation.view.desktop.PomodoroDesktopLayout
import dev.momotombo.app.mombodoro.presentation.view.mobile.PomodoroMobileLayout
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppCanvas
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.appearance
import dev.momotombo.app.mombodoro.utils.CustomDialog
import dev.momotombo.app.mombodoro.utils.platform
import kotlinx.coroutines.launch

@Composable
fun FrameWindowScope.PomodoroApp(
    timerState: PomodoroTimerState,
    taskController: FocusTasksController,
    onExit: () -> Unit,
    onSelectedTaskTitleChange: (String?) -> Unit,
    showNotificationAlert: Boolean,
    notificationSystemMessage: String?,
    onDismissNotificationSystemMessage: () -> Unit,
    notificationStatus: MacNotificationStatus?,
    onOpenNotificationSettings: () -> Unit,
) {
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowSettingsDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val taskScope = rememberCoroutineScope()
    val selectedTaskTitle = taskController.tasks.firstOrNull { it.id == taskController.selectedTaskId }?.title

    SideEffect { onSelectedTaskTitleChange(selectedTaskTitle) }

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
        notificationSystemMessage?.let { message ->
            NotificationAlert(
                isVisible = true,
                title = "Avisos de Mombodoro",
                message = message,
                backgroundColor = AppCanvas,
                textColor = AppText,
                accentColor = Pomodoro.FOCUS.appearance.accent,
                onDismiss = onDismissNotificationSystemMessage,
            )
        }

        timerState.notification?.takeIf { showNotificationAlert }?.let { activeNotification ->
            NotificationAlert(
                isVisible = true,
                title = activeNotification.title,
                message = activeNotification.message,
                backgroundColor = AppCanvas,
                textColor = AppText,
                accentColor = activeNotification.phase.appearance.accent,
                onDismiss = timerState::dismissNotification,
            )
        }

        if (isShowDialog) {
            CustomDialog(
                textColor = AppText,
                backgroundColor = AppCanvas,
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
                    speed = layout.speed,
                    isShowSettingsDialog = isShowSettingsDialog,
                    configuration = layout.configuration,
                    completedPomodoros = layout.completedFocusSessions,
                    tasks = taskController.tasks,
                    selectedTaskId = taskController.selectedTaskId,
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
                    onPhaseChange = timerState::switchPhase,
                    onAddTask = { title -> taskScope.launch { taskController.add(title) } },
                    onSelectTask = { id -> taskScope.launch { taskController.select(id) } },
                    onToggleTask = { id -> taskScope.launch { taskController.toggleCompletion(id) } },
                    onCompleteSelectedTask = { id -> taskScope.launch { taskController.toggleCompletion(id) } },
                    onDeleteTask = { id -> taskScope.launch { taskController.delete(id) } },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    notificationStatus = notificationStatus,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            } else {
                PomodoroMobileLayout(
                    pomodoro = layout.phase,
                    phaseTitle = layout.phaseTitle,
                    isPlayPomodoro = layout.isRunning,
                    timerLeft = layout.remainingSeconds,
                    speed = layout.speed,
                    isShowSettingsDialog = isShowSettingsDialog,
                    configuration = layout.configuration,
                    completedPomodoros = layout.completedFocusSessions,
                    tasks = taskController.tasks,
                    selectedTaskId = taskController.selectedTaskId,
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
                    onPhaseChange = timerState::switchPhase,
                    onAddTask = { title -> taskScope.launch { taskController.add(title) } },
                    onSelectTask = { id -> taskScope.launch { taskController.select(id) } },
                    onToggleTask = { id -> taskScope.launch { taskController.toggleCompletion(id) } },
                    onCompleteSelectedTask = { id -> taskScope.launch { taskController.toggleCompletion(id) } },
                    onDeleteTask = { id -> taskScope.launch { taskController.delete(id) } },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            }
        }
    }
}
