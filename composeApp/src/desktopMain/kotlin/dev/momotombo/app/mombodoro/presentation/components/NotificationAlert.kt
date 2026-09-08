package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun NotificationAlert(
    isVisible: Boolean,
    title: String,
    message: String,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = {
                Text(
                    text = title,
                    fontFamily = GetFontPoppinsSemiBold(),
                    color = textColor
                )
            },
            text = {
                Text(
                    text = message,
                    fontFamily = GetFontPoppinsMedium(),
                    color = textColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                ) {
                    Text(
                        text = "Aceptar",
                        fontFamily = GetFontPoppinsMedium(),
                        color = Color.White,
                    )
                }
            },
            containerColor = backgroundColor,
            titleContentColor = textColor,
            textContentColor = textColor
        )
    }
}
