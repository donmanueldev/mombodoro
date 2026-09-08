package dev.momotombo.app.mombodoro.presentation.view.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.TimerSpeed
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.presentation.components.PomodoroContent
import dev.momotombo.app.mombodoro.presentation.components.SelectedTaskControl
import dev.momotombo.app.mombodoro.presentation.components.SettingsDialog
import dev.momotombo.app.mombodoro.presentation.components.TasksPanel
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppCanvas
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.appearance
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_settings

@Composable
fun PomodoroMobileLayout(
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
    onBackToFocusSelector: () -> Unit = {}
) {
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppCanvas)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSettingsToggle(true) },
                painter = painterResource(Res.drawable.ic_settings),
                colorFilter = ColorFilter.tint(AppText.copy(alpha = 0.7f)),
                contentDescription = "Ajustes"
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = "Cambiar tipo",
                fontFamily = GetFontPoppinsMedium(),
                fontSize = 14.sp,
                color = AppText.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBackToFocusSelector() }
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(300.dp),
                contentAlignment = Alignment.Center
            ) {
                PomodoroContent(
                    pomodoro = pomodoro,
                    phaseTitle = phaseTitle,
                    isPlayPomodoro = isPlayPomodoro,
                    timerLeft = timerLeft,
                    totalSeconds = configuration.durationFor(pomodoro),
                    completedPomodoros = completedPomodoros,
                    totalPomodoros = configuration.cyclesBeforeLongBreak,
                    ringSize = 280.dp,
                    speed = speed,
                    onPlayPause = onPlayPause,
                    onSpeedChange = onSpeedChange,
                    onPhaseChange = onPhaseChange,
                )
            }
            selectedTask?.let { task ->
                SelectedTaskControl(
                    title = task.title,
                    accent = pomodoro.appearance.accent,
                    onComplete = { onCompleteSelectedTask(task.id) },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            TasksPanel(
                tasks = tasks,
                selectedTaskId = selectedTaskId,
                onAddTask = onAddTask,
                onSelectTask = onSelectTask,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }

        if (isShowSettingsDialog) {
            SettingsDialog(
                configuration = configuration,
                textColor = AppText,
                backgroundColor = AppCanvas,
                onCloseDialog = { onSettingsToggle(false) },
                onSaveSettings = onSaveSettings,
            )
        }
    }
}
