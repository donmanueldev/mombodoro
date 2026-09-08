package dev.donmanuel.app.pomodoro.data

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_break
import pomodoro.composeapp.generated.resources.ic_focus

enum class Pomodoro(
    val title: String,
    val timer: Int,
    val textColor: Color,
    val icon: DrawableResource,
    val iconColor: Color,
    val backgroundColor: Color,
    val buttonColorPrimary: Color,
    val buttonColorSecond: Color,
) {

    FOCUS(
        title = "Focus",
        timer = 1500,
        textColor = Color(0xFF471515),
        icon = Res.drawable.ic_focus,
        iconColor = Color(0xFF471515),
        backgroundColor = Color(0xFFFFFF2F2),
        buttonColorPrimary = Color(0xFFFF7C7C),
        buttonColorSecond = Color(0xFFFFD9D9)
    ),

    BREAK(
        title = "Short Break",
        timer = 300,
        textColor = Color(0xFF14401D),
        icon = Res.drawable.ic_break,
        iconColor = Color(0xFF14401D),
        backgroundColor = Color(0xFFF2FFF5),
        buttonColorPrimary = Color(0xFF8CE8A1),
        buttonColorSecond = Color(0xFFDAFAE0)
    ),

    LONG_BREAK(
        title = "Long Break",
        timer = 900,
        textColor = Color(0xFF0A3A58),
        icon = Res.drawable.ic_break,
        iconColor = Color(0xFF0A3A58),
        backgroundColor = Color(0xFFF0F9FF),
        buttonColorPrimary = Color(0xFF7CBBFF),
        buttonColorSecond = Color(0xFFD9EEFF)
    )

}
