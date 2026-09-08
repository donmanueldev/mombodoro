package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.donmanuel.app.pomodoro.data.PomodoroConfiguration
import dev.donmanuel.app.pomodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.donmanuel.app.pomodoro.presentation.ui.theme.GetFontPoppinsSemiBold
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_close

@Composable
fun SettingsDialog(
    configuration: PomodoroConfiguration,
    textColor: Color,
    backgroundColor: Color,
    onCloseDialog: () -> Unit,
    onSaveSettings: (PomodoroConfiguration) -> Unit
) {
    var draft by remember(configuration) { mutableStateOf(configuration) }

    Dialog(onDismissRequest = { onCloseDialog() }) {
        Surface(
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            border = BorderStroke(width = 1.dp, color = textColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes",
                        fontFamily = GetFontPoppinsSemiBold(),
                        fontSize = 18.sp,
                        color = textColor
                    )

                    Image(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                onCloseDialog()
                            },
                        painter = painterResource(Res.drawable.ic_close),
                        colorFilter = ColorFilter.tint(color = textColor.copy(alpha = 0.5f)),
                        contentDescription = "Cerrar"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Tiempo de enfoque (minutos)",
                    value = draft.focusSeconds / 60,
                    textColor = textColor,
                    onValueChange = { newValue ->
                        draft = draft.copy(focusSeconds = newValue * 60)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Descanso corto (minutos)",
                    value = draft.shortBreakSeconds / 60,
                    textColor = textColor,
                    onValueChange = { newValue ->
                        draft = draft.copy(shortBreakSeconds = newValue * 60)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Descanso largo (minutos)",
                    value = draft.longBreakSeconds / 60,
                    textColor = textColor,
                    onValueChange = { newValue ->
                        draft = draft.copy(longBreakSeconds = newValue * 60)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Ciclos antes del descanso largo",
                    value = draft.cyclesBeforeLongBreak,
                    textColor = textColor,
                    onValueChange = { newValue ->
                        draft = draft.copy(cyclesBeforeLongBreak = newValue)
                    },
                    minValue = 1,
                    maxValue = 10
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSaveSettings(draft)
                        onCloseDialog()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text(
                        text = "Guardar y reiniciar",
                        fontFamily = GetFontPoppinsMedium(),
                        color = backgroundColor
                    )
                }
            }
        }
    }
}
