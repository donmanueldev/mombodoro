package dev.donmanuel.app.pomodoro

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.mombo_app_icon
import pomodoro.composeapp.generated.resources.mombo_status_icon

fun main() = application {
    val windowState = rememberWindowState()
    val trayState = rememberTrayState()

    Tray(
        state = trayState,
        icon = painterResource(Res.drawable.mombo_status_icon),
        tooltip = "Mombo",
    ) {
        Item("Mostrar Mombo", onClick = { windowState.isMinimized = false })
        Separator()
        Item("Salir", onClick = ::exitApplication)
    }

    Window(
        state = windowState,
        onCloseRequest = { windowState.isMinimized = true },
        title = "Mombo",
        icon = painterResource(Res.drawable.mombo_app_icon),
    ) {
        PomodoroApp(onExit = ::exitApplication)
    }
}
