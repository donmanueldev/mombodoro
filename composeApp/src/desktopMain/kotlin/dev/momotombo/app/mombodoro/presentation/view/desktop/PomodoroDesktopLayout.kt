package dev.momotombo.app.mombodoro.presentation.view.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.TimerSpeed
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsAction
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsState
import dev.momotombo.app.mombodoro.presentation.components.PomodoroContent
import dev.momotombo.app.mombodoro.presentation.components.SelectedTaskControl
import dev.momotombo.app.mombodoro.presentation.components.SettingsPanel
import dev.momotombo.app.mombodoro.presentation.components.TasksPanel
import dev.momotombo.app.mombodoro.presentation.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_settings
import pomodoro.composeapp.generated.resources.ic_tasks

@Composable
fun PomodoroDesktopLayout(
    modifier: Modifier = Modifier,
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speed: TimerSpeed,
    isShowSettingsDialog: Boolean,
    configuration: PomodoroConfiguration,
    completedPomodoros: Int,
    tasks: List<FocusTask>,
    selectedTaskId: Long?,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
    onAddTask: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    onToggleTask: (Long) -> Unit,
    onCompleteSelectedTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onSettingsToggle: (Boolean) -> Unit,
    onSaveSettings: (PomodoroConfiguration) -> Unit,
    notificationSettings: NotificationSettingsState?,
    onNotificationAction: (NotificationSettingsAction) -> Unit,
    onBackToFocusSelector: () -> Unit = {},
) {
    var showTasksPanel by remember { mutableStateOf(false) }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(AppCanvas),
    ) {
        val compactLayout = maxWidth < 900.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(
                configurationName = configuration.name,
                onChangeSession = onBackToFocusSelector,
                tasksOpen = showTasksPanel,
                onOpenTasks = {
                    showTasksPanel = !showTasksPanel
                    onSettingsToggle(false)
                },
                onOpenSettings = {
                    showTasksPanel = false
                    onSettingsToggle(true)
                },
            )
            if (compactLayout) {
                CompactWorkspace(
                    pomodoro = pomodoro,
                    phaseTitle = phaseTitle,
                    isPlayPomodoro = isPlayPomodoro,
                    timerLeft = timerLeft,
                    speed = speed,
                    configuration = configuration,
                    completedPomodoros = completedPomodoros,
                    selectedTask = selectedTask,
                    onPlayPause = onPlayPause,
                    onSpeedChange = onSpeedChange,
                    onPhaseChange = onPhaseChange,
                    onCompleteSelectedTask = onCompleteSelectedTask,
                )
            } else {
                WideWorkspace(
                    pomodoro = pomodoro,
                    phaseTitle = phaseTitle,
                    isPlayPomodoro = isPlayPomodoro,
                    timerLeft = timerLeft,
                    speed = speed,
                    configuration = configuration,
                    completedPomodoros = completedPomodoros,
                    selectedTask = selectedTask,
                    tasksVisible = showTasksPanel,
                    onPlayPause = onPlayPause,
                    onSpeedChange = onSpeedChange,
                    onPhaseChange = onPhaseChange,
                    onCompleteSelectedTask = onCompleteSelectedTask,
                )
            }
        }

        if (isShowSettingsDialog) {
            SettingsPanel(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(420.dp),
                configuration = configuration,
                onClose = { onSettingsToggle(false) },
                onSave = onSaveSettings,
                notificationSettings = notificationSettings,
                onNotificationAction = onNotificationAction,
            )
        }

        if (showTasksPanel && compactLayout) {
            Dialog(onDismissRequest = { showTasksPanel = false }) {
                Surface(color = AppSurface, shape = RoundedCornerShape(24.dp)) {
                    TasksPanel(
                        tasks = tasks,
                        selectedTaskId = selectedTaskId,
                        onAddTask = onAddTask,
                        onSelectTask = onSelectTask,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onClose = { showTasksPanel = false },
                        modifier = Modifier.width(420.dp),
                    )
                }
            }
        }

        if (showTasksPanel && !compactLayout) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(420.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppOutline),
            ) {
                TasksPanel(
                    tasks = tasks,
                    selectedTaskId = selectedTaskId,
                    onAddTask = onAddTask,
                    onSelectTask = onSelectTask,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onClose = { showTasksPanel = false },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    configurationName: String,
    onChangeSession: () -> Unit,
    tasksOpen: Boolean,
    onOpenTasks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 32.dp),
    ) {
        Text(
            "Mombodoro",
            modifier = Modifier.align(Alignment.CenterStart),
            color = AppText,
            fontFamily = GetFontPoppinsSemiBold(),
            fontSize = 20.sp,
        )
        Button(
            modifier = Modifier.align(Alignment.Center),
            onClick = onChangeSession,
            colors = ButtonDefaults.buttonColors(containerColor = AppSurface, contentColor = AppText),
            border = BorderStroke(1.dp, AppOutline),
            shape = RoundedCornerShape(12.dp),
        ) { Text(configurationName, fontFamily = GetFontPoppinsMedium()) }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TaskButton(isOpen = tasksOpen, onClick = onOpenTasks)
            Spacer(Modifier.width(12.dp))
            SettingsButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun WideWorkspace(
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speed: TimerSpeed,
    configuration: PomodoroConfiguration,
    completedPomodoros: Int,
    selectedTask: FocusTask?,
    tasksVisible: Boolean,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
    onCompleteSelectedTask: (Long) -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize(),
    ) {
        val sidePanelWidth = if (tasksVisible) 420.dp else 0.dp
        val workspaceWidth = minOf(maxWidth - sidePanelWidth - 68.dp, 980.dp)
        Box(
            modifier = Modifier.fillMaxSize().padding(end = sidePanelWidth),
            contentAlignment = Alignment.TopCenter,
        ) {
            TimerWorkspace(
                modifier = Modifier.width(workspaceWidth).fillMaxHeight().padding(bottom = 32.dp),
                pomodoro = pomodoro,
                phaseTitle = phaseTitle,
                isPlayPomodoro = isPlayPomodoro,
                timerLeft = timerLeft,
                speed = speed,
                configuration = configuration,
                completedPomodoros = completedPomodoros,
                selectedTask = selectedTask,
                onPlayPause = onPlayPause,
                onSpeedChange = onSpeedChange,
                onPhaseChange = onPhaseChange,
                onCompleteSelectedTask = onCompleteSelectedTask,
            )
        }
    }
}

@Composable
private fun CompactWorkspace(
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speed: TimerSpeed,
    configuration: PomodoroConfiguration,
    completedPomodoros: Int,
    selectedTask: FocusTask?,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
    onCompleteSelectedTask: (Long) -> Unit,
) {
    TimerWorkspace(
        modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 20.dp),
        pomodoro = pomodoro,
        phaseTitle = phaseTitle,
        isPlayPomodoro = isPlayPomodoro,
        timerLeft = timerLeft,
        speed = speed,
        configuration = configuration,
        completedPomodoros = completedPomodoros,
        selectedTask = selectedTask,
        onPlayPause = onPlayPause,
        onSpeedChange = onSpeedChange,
        onPhaseChange = onPhaseChange,
        onCompleteSelectedTask = onCompleteSelectedTask,
    )
}

@Composable
private fun TimerWorkspace(
    modifier: Modifier,
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speed: TimerSpeed,
    configuration: PomodoroConfiguration,
    completedPomodoros: Int,
    selectedTask: FocusTask?,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
    onCompleteSelectedTask: (Long) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val ringSize = minOf(maxWidth - 72.dp, maxHeight - 260.dp, 430.dp).coerceAtLeast(220.dp)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PomodoroContent(
                pomodoro = pomodoro,
                phaseTitle = phaseTitle,
                isPlayPomodoro = isPlayPomodoro,
                timerLeft = timerLeft,
                totalSeconds = configuration.durationFor(pomodoro),
                completedPomodoros = completedPomodoros,
                totalPomodoros = configuration.cyclesBeforeLongBreak,
                ringSize = ringSize,
                speed = speed,
                onPlayPause = onPlayPause,
                onSpeedChange = onSpeedChange,
                onPhaseChange = onPhaseChange,
            )
            Spacer(Modifier.height(22.dp))
            if (selectedTask == null) {
                Text(
                    text = "Selecciona una tarea para esta sesión",
                    color = AppMutedText,
                    fontFamily = GetFontPoppinsMedium(),
                    fontSize = 14.sp,
                )
            } else {
                SelectedTaskControl(
                    title = selectedTask.title,
                    accent = pomodoro.appearance.accent,
                    onComplete = { onCompleteSelectedTask(selectedTask.id) },
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.width(44.dp).height(44.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AppSurface, contentColor = AppText),
        border = BorderStroke(1.dp, AppOutline),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_settings),
            contentDescription = "Abrir ajustes",
            colorFilter = ColorFilter.tint(AppText),
        )
    }
}

@Composable
private fun TaskButton(isOpen: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(44.dp).height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOpen) AppText else AppSurface,
            contentColor = if (isOpen) Color.White else AppText,
        ),
        border = BorderStroke(1.dp, AppOutline),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_tasks),
            contentDescription = if (isOpen) "Ocultar tareas" else "Mostrar tareas",
            colorFilter = ColorFilter.tint(if (isOpen) Color.White else AppText),
        )
    }
}
