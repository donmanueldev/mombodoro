package dev.momotombo.app.mombodoro.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_close
import pomodoro.composeapp.generated.resources.mombo_app_icon
import java.awt.Desktop
import java.net.URI

@Composable
fun CustomDialog(
    modifier: Modifier = Modifier,
    textColor: Color,
    backgroundColor: Color,
    onCloseDialog: () -> Unit
) {
    Dialog(
        onDismissRequest = onCloseDialog
    ) {
        Surface(
            modifier = modifier
                .width(420.dp)
                .wrapContentHeight(),
            color = backgroundColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Acerca de",
                        fontSize = 16.sp,
                        fontFamily = GetFontPoppinsSemiBold(),
                        color = textColor,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(onClick = onCloseDialog, modifier = Modifier.size(44.dp)) {
                        Image(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(Res.drawable.ic_close),
                            colorFilter = ColorFilter.tint(color = Color.Black.copy(alpha = 0.5f)),
                            contentDescription = "Cerrar",
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 18.dp).fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color(0xFF471515).copy(alpha = 0.1f)
                )

                Image(
                    painter = painterResource(Res.drawable.mombo_app_icon),
                    contentDescription = "Logo de Mombodoro",
                    modifier = Modifier.size(76.dp),
                )
                Text(
                    text = "Mombodoro",
                    modifier = Modifier.padding(top = 14.dp),
                    color = textColor,
                    fontFamily = GetFontPoppinsSemiBold(),
                    fontSize = 24.sp,
                )
                Text(
                    text = "Un temporizador de enfoque simple para trabajar con intención, descansar a tiempo y volver a lo importante.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = textColor.copy(alpha = 0.72f),
                    fontFamily = GetFontPoppinsMedium(),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(20.dp))
                AboutLink("Proyecto de Mombodoro", "https://github.com/donmanueldev/PomodoroKT", textColor)
                AboutLink("Momotombo Dev", "https://momotombo.dev/", textColor)
                Text(
                    text = "Versión ${System.getProperty("mombodoro.version", "1.0.0")}",
                    modifier = Modifier.padding(top = 16.dp),
                    color = textColor.copy(alpha = 0.5f),
                    fontFamily = GetFontPoppinsMedium(),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun AboutLink(label: String, url: String, textColor: Color) {
    Text(
        text = label,
        modifier = Modifier.padding(vertical = 6.dp).clickable { openExternalUrl(url) },
        color = textColor,
        fontFamily = GetFontPoppinsSemiBold(),
        fontSize = 13.sp,
        textDecoration = TextDecoration.Underline,
    )
}

private fun openExternalUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
