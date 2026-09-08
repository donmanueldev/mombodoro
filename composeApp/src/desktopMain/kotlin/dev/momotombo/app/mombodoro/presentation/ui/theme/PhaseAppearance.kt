package dev.momotombo.app.mombodoro.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import dev.momotombo.app.mombodoro.data.Pomodoro
import org.jetbrains.compose.resources.DrawableResource
import pomodoro.composeapp.generated.resources.Res
import pomodoro.composeapp.generated.resources.ic_break
import pomodoro.composeapp.generated.resources.ic_focus

data class PhaseAppearance(
    val label: String,
    val icon: DrawableResource,
    val accent: Color,
)

val AppCanvas = Color(0xFFFFFBF8)
val AppSurface = Color(0xFFFFFFFF)
val AppText = Color(0xFF331E1C)
val AppMutedText = Color(0xFF765F5C)
val AppOutline = Color(0xFFE9DCD8)

val Pomodoro.appearance: PhaseAppearance
    get() = when (this) {
        Pomodoro.FOCUS -> PhaseAppearance("Enfoque", Res.drawable.ic_focus, Color(0xFFC85B4D))
        Pomodoro.BREAK -> PhaseAppearance("Descanso corto", Res.drawable.ic_break, Color(0xFF397D78))
        Pomodoro.LONG_BREAK -> PhaseAppearance("Descanso largo", Res.drawable.ic_break, Color(0xFF8E6D3E))
    }
