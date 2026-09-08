package dev.momotombo.app.mombodoro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.FocusTaskRepository
import dev.momotombo.app.mombodoro.presentation.components.CustomFocusDialog
import dev.momotombo.app.mombodoro.presentation.components.FocusTypeSelector
import dev.momotombo.app.mombodoro.presentation.components.NotificationAlert
import dev.momotombo.app.mombodoro.presentation.view.desktop.PomodoroDesktopLayout
import dev.momotombo.app.mombodoro.presentation.view.mobile.PomodoroMobileLayout
import dev.momotombo.app.mombodoro.utils.CustomDialog
import dev.momotombo.app.mombodoro.utils.platform

@Composable
fun FrameWindowScope.PomodoroApp(timerState: PomodoroTimerState, onExit: () -> Unit) {
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowSettingsDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val taskRepository = remember { FocusTaskRepository.openDefault() }
    val tasks = remember(taskRepository) {
        mutableStateListOf<FocusTask>().also { persistedTasks ->
            persistedTasks.addAll(taskRepository.loadAll())
        }
    }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }

    fun addTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val task = taskRepository.add(trimmedTitle)
        tasks += task
        selectedTaskId = task.id
    }

    fun toggleTask(id: Long) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updatedTask = tasks[index].copy(isCompleted = !tasks[index].isCompleted)
            taskRepository.updateCompletion(updatedTask.id, updatedTask.isCompleted)
            tasks[index] = updatedTask
        }
    }

    fun deleteTask(id: Long) {
        taskRepository.delete(id)
        tasks.removeAll { it.id == id }
        if (selectedTaskId == id) selectedTaskId = tasks.firstOrNull { !it.isCompleted }?.id
    }

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
                    tasks = tasks,
                    selectedTaskId = selectedTaskId,
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
                    onPhaseChange = timerState::switchPhase,
                    onAddTask = ::addTask,
                    onSelectTask = { selectedTaskId = it },
                    onToggleTask = ::toggleTask,
                    onDeleteTask = ::deleteTask,
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
                    tasks = tasks,
                    selectedTaskId = selectedTaskId,
                    onPlayPause = { timerState.toggleRunning() },
                    onSpeedChange = timerState::changeSpeed,
                    onPhaseChange = timerState::switchPhase,
                    onAddTask = ::addTask,
                    onSelectTask = { selectedTaskId = it },
                    onToggleTask = ::toggleTask,
                    onDeleteTask = ::deleteTask,
                    onAbout = { isShowDialog = true },
                    onSettingsToggle = { isShowSettingsDialog = it },
                    onSaveSettings = onSaveSettings,
                    onBackToFocusSelector = ::showFocusTypeSelector,
                )
            }
        }
    }
}
