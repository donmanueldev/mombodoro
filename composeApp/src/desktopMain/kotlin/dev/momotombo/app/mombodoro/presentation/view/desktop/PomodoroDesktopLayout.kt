package dev.momotombo.app.mombodoro.presentation.view.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.TimerSpeed
import dev.momotombo.app.mombodoro.presentation.components.PomodoroContent
import dev.momotombo.app.mombodoro.presentation.components.SettingsDialog
import dev.momotombo.app.mombodoro.presentation.components.TasksPanel
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_settings

@Composable
fun PomodoroDesktopLayout(
    modifier: Modifier = Modifier,
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speedTime: TimerSpeed,
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
    onDeleteTask: (Long) -> Unit,
    onAbout: () -> Unit,
    onSettingsToggle: (Boolean) -> Unit,
    onSaveSettings: (PomodoroConfiguration) -> Unit,
    onBackToFocusSelector: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = pomodoro.backgroundColor)
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
                colorFilter = ColorFilter.tint(pomodoro.textColor.copy(alpha = 0.7f)),
                contentDescription = "Settings"
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
                color = pomodoro.textColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBackToFocusSelector() }
                    .padding(8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = pomodoro.buttonColorSecond.copy(alpha = 0.6f),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.widthIn(min = 460.dp, max = 560.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 34.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PomodoroContent(
                        pomodoro = pomodoro,
                        phaseTitle = phaseTitle,
                        isPlayPomodoro = isPlayPomodoro,
                        timerLeft = timerLeft,
                        speedTime = speedTime,
                        completedPomodoros = completedPomodoros,
                        totalPomodoros = configuration.cyclesBeforeLongBreak,
                        onPlayPause = onPlayPause,
                        onSpeedChange = onSpeedChange,
                        onPhaseChange = onPhaseChange,
                        onDialogToggle = { onAbout() }
                    )
                }
            }
            TasksPanel(
                tasks = tasks,
                selectedTaskId = selectedTaskId,
                textColor = pomodoro.textColor,
                surfaceColor = pomodoro.buttonColorSecond,
                onAddTask = onAddTask,
                onSelectTask = onSelectTask,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
            )
        }

        if (isShowSettingsDialog) {
            SettingsDialog(
                configuration = configuration,
                textColor = pomodoro.textColor,
                backgroundColor = pomodoro.backgroundColor,
                onCloseDialog = { onSettingsToggle(false) },
                onSaveSettings = onSaveSettings,
            )
        }
    }
}
