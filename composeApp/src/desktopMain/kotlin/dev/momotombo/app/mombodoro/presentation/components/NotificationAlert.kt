package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

/**
 * Componente para mostrar una notificación como un AlertDialog
 */
@Composable
fun NotificationAlert(
    isVisible: Boolean,
    title: String,
    message: String,
    backgroundColor: Color,
    textColor: Color,
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
                    }
                ) {
                    Text(
                        text = "Aceptar",
                        fontFamily = GetFontPoppinsMedium(),
                        color = backgroundColor
                    )
                }
            },
            containerColor = backgroundColor,
            titleContentColor = textColor,
            textContentColor = textColor
        )
    }
}
