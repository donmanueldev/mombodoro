package dev.donmanuel.app.pomodoro.presentation.view.mobile

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
import dev.donmanuel.app.pomodoro.data.Pomodoro
import dev.donmanuel.app.pomodoro.data.PomodoroConfiguration
import dev.donmanuel.app.pomodoro.data.TimerSpeed
import dev.donmanuel.app.pomodoro.presentation.components.PomodoroContent
import dev.donmanuel.app.pomodoro.presentation.components.SettingsDialog
import dev.donmanuel.app.pomodoro.presentation.ui.theme.GetFontPoppinsMedium
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_settings

@Composable
fun PomodoroMobileLayout(
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speedTime: TimerSpeed,
    isShowSettingsDialog: Boolean,
    configuration: PomodoroConfiguration,
    completedPomodoros: Int,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onAbout: () -> Unit,
    onSettingsToggle: (Boolean) -> Unit,
    onSaveSettings: (PomodoroConfiguration) -> Unit,
    onBackToFocusSelector: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pomodoro.backgroundColor)
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
                    speedTime = speedTime,
                    completedPomodoros = completedPomodoros,
                    totalPomodoros = configuration.cyclesBeforeLongBreak,
                    onPlayPause = onPlayPause,
                    onSpeedChange = onSpeedChange,
                    onDialogToggle = { onAbout() }
                )
            }
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
