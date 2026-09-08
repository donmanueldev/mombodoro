package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_fast_foward
import pomodoro.composeapp.generated.resources.ic_menu
import pomodoro.composeapp.generated.resources.ic_pause
import pomodoro.composeapp.generated.resources.ic_play
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.TimerSpeed
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsBold
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun PomodoroContent(
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    speedTime: TimerSpeed,
    completedPomodoros: Int = 0,
    totalPomodoros: Int = 4,
    onPlayPause: (Boolean) -> Unit,
    onSpeedChange: (TimerSpeed) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
    onDialogToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Pomodoro.entries.forEach { phase ->
            val selected = phase == pomodoro
            Surface(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clickable { onPhaseChange(phase) },
                color = if (selected) pomodoro.textColor.copy(alpha = 0.18f) else pomodoro.backgroundColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = when (phase) {
                        Pomodoro.FOCUS -> "Enfoque"
                        Pomodoro.BREAK -> "Descanso"
                        Pomodoro.LONG_BREAK -> "Largo"
                    },
                    fontFamily = GetFontPoppinsSemiBold(),
                    fontSize = 13.sp,
                    color = pomodoro.textColor.copy(alpha = if (selected) 1f else 0.7f),
                )
            }
        }
    }

    Surface(
        color = pomodoro.buttonColorSecond,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(width = 1.dp, color = pomodoro.textColor),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(22.dp),
                painter = painterResource(pomodoro.icon),
                contentDescription = null
            )

            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = phaseTitle,
                fontFamily = GetFontPoppinsSemiBold(),
                fontSize = 14.sp,
                color = pomodoro.textColor
            )
        }
    }

    Text(
        text = String.format("%02d:%02d", timerLeft / 60, timerLeft % 60),
        fontFamily = GetFontPoppinsBold(),
        fontSize = 118.sp,
        textAlign = TextAlign.Center,
        lineHeight = 118.sp,
        color = pomodoro.textColor
    )
    
    if (pomodoro == Pomodoro.FOCUS) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            for (i in 1..totalPomodoros) {
                val color = if (i <= completedPomodoros) {
                    pomodoro.buttonColorPrimary
                } else {
                    pomodoro.buttonColorSecond
                }
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = color,
                    border = BorderStroke(1.dp, pomodoro.textColor.copy(alpha = 0.2f))
                ) {}
                if (i < totalPomodoros) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pomodoro.buttonColorSecond,
            ),
            contentPadding = PaddingValues(0.dp),
            onClick = { onDialogToggle(true) }
        ) {
            Image(
                modifier = Modifier.size(18.dp),
                painter = painterResource(Res.drawable.ic_menu),
                contentDescription = "Abrir información"
            )
        }

        Button(
            modifier = Modifier.size(width = 120.dp, height = 80.dp)
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pomodoro.buttonColorPrimary
            ),
            onClick = { onPlayPause(!isPlayPomodoro) }
        ) {
            Image(
                painter = painterResource(
                    if (!isPlayPomodoro)
                        Res.drawable.ic_play
                    else
                        Res.drawable.ic_pause
                ),
                contentDescription = if (isPlayPomodoro) "Pausar temporizador" else "Iniciar temporizador"
            )
        }

        Button(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pomodoro.buttonColorSecond
            ),
            contentPadding = PaddingValues(0.dp),
            onClick = {
                onSpeedChange(if (speedTime == TimerSpeed.NORMAL) TimerSpeed.FAST else TimerSpeed.NORMAL)
            }
        ) {
            Image(
                modifier = Modifier.size(18.dp),
                painter = painterResource(Res.drawable.ic_fast_foward),
                contentDescription = "Cambiar velocidad"
            )
        }
    }

    if (speedTime == TimerSpeed.FAST) {
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "Velocidad rápida",
            fontFamily = GetFontPoppinsMedium(),
            color = pomodoro.textColor.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}
