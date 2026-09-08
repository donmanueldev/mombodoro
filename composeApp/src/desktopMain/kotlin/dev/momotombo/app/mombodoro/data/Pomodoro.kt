package dev.momotombo.app.mombodoro.data

enum class Pomodoro(
    val title: String,
    val timer: Int,
) {
    FOCUS(title = "Enfoque", timer = 1500),
    BREAK(title = "Descanso corto", timer = 300),
    LONG_BREAK(title = "Descanso largo", timer = 900),
}
