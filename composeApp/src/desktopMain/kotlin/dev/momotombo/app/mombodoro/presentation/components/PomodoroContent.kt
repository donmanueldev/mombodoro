package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppMutedText
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppOutline
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsBold
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold
import dev.momotombo.app.mombodoro.presentation.ui.theme.appearance
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_pause
import pomodoro.composeapp.generated.resources.ic_play

@Composable
fun PomodoroContent(
    pomodoro: Pomodoro,
    phaseTitle: String,
    isPlayPomodoro: Boolean,
    timerLeft: Int,
    totalSeconds: Int,
    completedPomodoros: Int,
    totalPomodoros: Int,
    ringSize: Dp,
    onPlayPause: (Boolean) -> Unit,
    onPhaseChange: (Pomodoro) -> Unit,
) {
    val appearance = pomodoro.appearance
    val progress = timerLeft.toFloat() / totalSeconds.coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PhaseSelector(selectedPhase = pomodoro, onPhaseChange = onPhaseChange)
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                val strokeWidth = 12.dp.toPx()
                val inset = strokeWidth / 2
                val arcSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
                drawArc(AppOutline, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokeWidth))
                drawArc(appearance.accent, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d:%02d", timerLeft / 60, timerLeft % 60),
                    color = AppText,
                    fontFamily = GetFontPoppinsBold(),
                    fontSize = 82.sp,
                    lineHeight = 88.sp,
                )
                Surface(color = appearance.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp)) {
                    Text(
                        text = phaseTitle,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = appearance.accent,
                        fontFamily = GetFontPoppinsSemiBold(),
                        fontSize = 13.sp,
                    )
                }
            }
        }
        CycleProgress(completedPomodoros, totalPomodoros, appearance.accent)
        Spacer(Modifier.height(24.dp))
        TimerControls(isPlayPomodoro, appearance.accent, onPlayPause)
    }
}

@Composable
private fun PhaseSelector(selectedPhase: Pomodoro, onPhaseChange: (Pomodoro) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(100.dp), border = BorderStroke(1.dp, AppOutline)) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Pomodoro.entries.forEach { phase ->
                val appearance = phase.appearance
                val selected = phase == selectedPhase
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(100.dp)).clickable { onPhaseChange(phase) },
                    color = if (selected) appearance.accent else Color.Transparent,
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(
                        text = appearance.label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (selected) Color.White else AppMutedText,
                        fontFamily = GetFontPoppinsSemiBold(),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleProgress(completedPomodoros: Int, totalPomodoros: Int, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Ciclo ${completedPomodoros + 1} de $totalPomodoros", color = AppMutedText, fontFamily = GetFontPoppinsMedium(), fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        repeat(totalPomodoros) { index ->
            Surface(
                modifier = Modifier.size(10.dp),
                color = if (index < completedPomodoros) accent else Color.Transparent,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (index < completedPomodoros) accent else AppOutline),
            ) {}
            if (index < totalPomodoros - 1) Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    accent: Color,
    onPlayPause: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            modifier = Modifier.width(190.dp).height(58.dp),
            onClick = { onPlayPause(!isRunning) },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Image(painterResource(if (isRunning) Res.drawable.ic_pause else Res.drawable.ic_play), if (isRunning) "Pausar temporizador" else "Iniciar temporizador")
            Spacer(Modifier.width(10.dp))
            Text(if (isRunning) "Pausar" else "Empezar", fontFamily = GetFontPoppinsSemiBold(), color = Color.White)
        }
    }
}
