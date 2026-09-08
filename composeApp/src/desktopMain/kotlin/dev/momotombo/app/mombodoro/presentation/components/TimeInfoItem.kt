package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun TimeInfoItem(
    label: String,
    value: String,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = GetFontPoppinsMedium(),
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            fontFamily = GetFontPoppinsSemiBold(),
            fontSize = 14.sp,
            color = textColor
        )
    }
}