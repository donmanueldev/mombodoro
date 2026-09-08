package dev.momotombo.app.mombodoro.data

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
        textColor = Color.White,
        icon = Res.drawable.ic_focus,
        iconColor = Color.White,
        backgroundColor = Color(0xFFBA4949),
        buttonColorPrimary = Color.White,
        buttonColorSecond = Color(0xFFCB6464)
    ),

    BREAK(
        title = "Short Break",
        timer = 300,
        textColor = Color.White,
        icon = Res.drawable.ic_break,
        iconColor = Color.White,
        backgroundColor = Color(0xFF2D7B80),
        buttonColorPrimary = Color.White,
        buttonColorSecond = Color(0xFF4B9094)
    ),

    LONG_BREAK(
        title = "Long Break",
        timer = 900,
        textColor = Color.White,
        icon = Res.drawable.ic_break,
        iconColor = Color.White,
        backgroundColor = Color(0xFF3977A3),
        buttonColorPrimary = Color.White,
        buttonColorSecond = Color(0xFF568CB1)
    )

}
