package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppCanvas
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppMutedText
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppOutline
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun SettingsPanel(
    configuration: PomodoroConfiguration,
    onClose: () -> Unit,
    onSave: (PomodoroConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(configuration) { mutableStateOf(configuration) }

    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)) {
        Column(Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ajustes de sesión", color = AppText, fontFamily = GetFontPoppinsSemiBold(), fontSize = 21.sp)
                    Text("Personaliza el siguiente ciclo", color = AppMutedText, fontFamily = GetFontPoppinsMedium(), fontSize = 13.sp)
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = AppCanvas, contentColor = AppText),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("Cerrar") }
            }
            HorizontalDivider(Modifier.padding(vertical = 24.dp), color = AppOutline)
            Text("Duraciones", color = AppText, fontFamily = GetFontPoppinsSemiBold(), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            TimeSettingItem("Enfoque (minutos)", draft.focusSeconds / 60, AppText, onValueChange = { value ->
                draft = draft.copy(focusSeconds = value * 60)
            })
            Spacer(Modifier.height(16.dp))
            TimeSettingItem("Descanso corto (minutos)", draft.shortBreakSeconds / 60, AppText, onValueChange = { value ->
                draft = draft.copy(shortBreakSeconds = value * 60)
            })
            Spacer(Modifier.height(16.dp))
            TimeSettingItem("Descanso largo (minutos)", draft.longBreakSeconds / 60, AppText, onValueChange = { value ->
                draft = draft.copy(longBreakSeconds = value * 60)
            })
            HorizontalDivider(Modifier.padding(vertical = 24.dp), color = AppOutline)
            Text("Ciclos", color = AppText, fontFamily = GetFontPoppinsSemiBold(), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            TimeSettingItem("Antes del descanso largo", draft.cyclesBeforeLongBreak, AppText, onValueChange = { value ->
                draft = draft.copy(cyclesBeforeLongBreak = value)
            }, minValue = 1, maxValue = 10)
            Surface(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), color = AppCanvas, shape = RoundedCornerShape(12.dp)) {
                Text(
                    "Guardar reinicia la sesión actual desde enfoque.",
                    modifier = Modifier.padding(14.dp),
                    color = AppMutedText,
                    fontFamily = GetFontPoppinsMedium(),
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = AppCanvas, contentColor = AppText),
                ) { Text("Cancelar", fontFamily = GetFontPoppinsSemiBold()) }
                Button(
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        onSave(draft)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppText, contentColor = Color.White),
                ) { Text("Guardar y reiniciar", fontFamily = GetFontPoppinsSemiBold()) }
            }
        }
    }
}
