package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun CustomFocusDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int, Int) -> Unit
) {
    var focusTime by remember { mutableStateOf(25) }
    var shortBreakTime by remember { mutableStateOf(5) }
    var longBreakTime by remember { mutableStateOf(15) }
    var cycles by remember { mutableStateOf(4) }

    val backgroundColor = Pomodoro.FOCUS.backgroundColor
    val textColor = Pomodoro.FOCUS.textColor

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configuración personalizada",
                    fontFamily = GetFontPoppinsSemiBold(),
                    fontSize = 20.sp,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                TimeSettingItem(
                    title = "Tiempo de enfoque (min)",
                    value = focusTime,
                    textColor = textColor,
                    onValueChange = { focusTime = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Tiempo de descanso corto (min)",
                    value = shortBreakTime,
                    textColor = textColor,
                    onValueChange = { shortBreakTime = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Tiempo de descanso largo (min)",
                    value = longBreakTime,
                    textColor = textColor,
                    onValueChange = { longBreakTime = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeSettingItem(
                    title = "Ciclos antes de Long Break",
                    value = cycles,
                    textColor = textColor,
                    onValueChange = { cycles = it },
                    minValue = 1,
                    maxValue = 10
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = textColor.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = "Cancelar",
                            fontFamily = GetFontPoppinsMedium(),
                            fontSize = 16.sp,
                            color = backgroundColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm("Personalizado", focusTime * 60, shortBreakTime * 60, longBreakTime * 60, cycles)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = textColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Text(
                            text = "Guardar",
                            fontFamily = GetFontPoppinsMedium(),
                            fontSize = 16.sp,
                            color = backgroundColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
