package dev.momotombo.app.mombodoro.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.donmanuel.app.pomodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.donmanuel.app.pomodoro.presentation.ui.theme.GetFontPoppinsSemiBold
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_close

@Composable
fun CustomDialog(
    modifier: Modifier = Modifier,
    textColor: Color,
    backgroundColor: Color,
    onCloseDialog: () -> Unit
) {
    Dialog(
        onDismissRequest = {}
    ) {
        Surface(
            modifier = modifier
                .width(280.dp)
                .wrapContentHeight(),
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Acerca de",
                        fontSize = 16.sp,
                        fontFamily = GetFontPoppinsSemiBold(),
                        color = textColor
                    )

                    Image(
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onCloseDialog()
                            },
                        painter = painterResource(Res.drawable.ic_close),
                        colorFilter = ColorFilter.tint(color = Color.Black.copy(alpha = 0.5f)),
                        contentDescription = ""
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color(0xFF471515).copy(alpha = 0.1f)
                )

                ContentAboutApp(
                    title = "Desarrollador",
                    content = "Momotombo Devs",
                    textColor = textColor
                )

                ContentAboutApp(
                    title = "Versión",
                    content = "1.0.0",
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
private fun ContentAboutApp(
    modifier: Modifier = Modifier,
    title: String,
    content: String,
    textColor: Color
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontFamily = GetFontPoppinsMedium(),
            fontSize = 12.sp,
            color = textColor
        )

        Text(
            text = content,
            fontFamily = GetFontPoppinsMedium(),
            fontSize = 10.sp,
            color = textColor
        )
    }
}
