package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppCanvas
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun FocusTypeSelector(
    onSelectFocusType: (String, Int, Int, Int, Int) -> Unit
) {
    val backgroundColor = AppCanvas
    val textColor = AppText

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        val contentWidth = minOf(maxWidth - 64.dp, 900.dp)
        Column(
            modifier = Modifier.width(contentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
        Text(
            text = "Selecciona un tipo de enfoque",
            fontFamily = GetFontPoppinsSemiBold(),
            fontSize = 24.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        FocusTypeCard(
            title = "Pomodoro",
            focusTime = 25,
            shortBreakTime = 5,
            longBreakTime = 15,
            cycles = 4,
            backgroundColor = backgroundColor,
            textColor = textColor,
            onClick = { onSelectFocusType("Pomodoro", 25 * 60, 5 * 60, 15 * 60, 4) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FocusTypeCard(
            title = "Estudio",
            focusTime = 50,
            shortBreakTime = 10,
            longBreakTime = 30,
            cycles = 3,
            backgroundColor = backgroundColor,
            textColor = textColor,
            onClick = { onSelectFocusType("Estudio", 50 * 60, 10 * 60, 30 * 60, 3) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FocusTypeCard(
            title = "Trabajo",
            focusTime = 45,
            shortBreakTime = 5,
            longBreakTime = 15,
            cycles = 4,
            backgroundColor = backgroundColor,
            textColor = textColor,
            onClick = { onSelectFocusType("Trabajo", 45 * 60, 5 * 60, 15 * 60, 4) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FocusTypeCard(
            title = "Sesión Corta",
            focusTime = 15,
            shortBreakTime = 3,
            longBreakTime = 10,
            cycles = 5,
            backgroundColor = backgroundColor,
            textColor = textColor,
            onClick = { onSelectFocusType("Sesión Corta", 15 * 60, 3 * 60, 10 * 60, 5) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSelectFocusType("Personalizado", 25 * 60, 5 * 60, 15 * 60, 4)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = textColor
            ),
            shape = RoundedCornerShape(8.dp),
            interactionSource = remember { MutableInteractionSource() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                text = "Personalizado",
                fontFamily = GetFontPoppinsMedium(),
                fontSize = 16.sp,
                color = backgroundColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        }
    }
}

@Composable
fun FocusTypeCard(
    title: String,
    focusTime: Int,
    shortBreakTime: Int,
    longBreakTime: Int,
    cycles: Int,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontFamily = GetFontPoppinsSemiBold(),
                fontSize = 18.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeInfoItem(label = "Enfoque", value = "$focusTime min", textColor = textColor)
                TimeInfoItem(label = "Descanso corto", value = "$shortBreakTime min", textColor = textColor)
                TimeInfoItem(label = "Descanso largo", value = "$longBreakTime min", textColor = textColor)
                TimeInfoItem(label = "Ciclos", value = "$cycles", textColor = textColor)
            }
        }
    }
}
