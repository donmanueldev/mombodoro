package dev.momotombo.app.mombodoro

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.TimerSpeed
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsState
import dev.momotombo.app.mombodoro.presentation.components.FocusTypeSelector
import dev.momotombo.app.mombodoro.presentation.view.desktop.PomodoroDesktopLayout
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

@OptIn(ExperimentalTestApi::class)
class AppStoreScreenshotGeneratorTest {
    private val configuration = PomodoroConfiguration(
        name = "Pomodoro",
        focusSeconds = 25 * 60,
        shortBreakSeconds = 5 * 60,
        longBreakSeconds = 15 * 60,
        cyclesBeforeLongBreak = 4,
    )

    private val tasks = listOf(
        FocusTask(1, "Preparar la presentación del proyecto"),
        FocusTask(2, "Revisar las tareas de hoy"),
        FocusTask(3, "Responder mensajes pendientes", isCompleted = true),
    )

    @Test
    fun `generate App Store screenshots when an output directory is configured`() {
        val outputDirectory = System.getenv("MOMBODORO_SCREENSHOT_OUTPUT")?.let(Path::of) ?: return
        Files.createDirectories(outputDirectory)

        runDesktopComposeUiTest(width = 1440, height = 900) {
            setContent { MaterialTheme { FocusTypeSelector(onSelectFocusType = { _, _, _, _, _ -> }) } }
            saveScreenshot(outputDirectory.resolve("01-selector-de-sesion.png"))
        }

        runDesktopComposeUiTest(width = 1440, height = 900) {
            setContent {
                MaterialTheme {
                    timerLayout(
                        pomodoro = Pomodoro.FOCUS,
                        phaseTitle = "Enfoque · Pomodoro",
                        timerLeft = 18 * 60 + 42,
                        completedPomodoros = 1,
                    )
                }
            }
            onNodeWithContentDescription("Mostrar tareas").performClick()
            saveScreenshot(outputDirectory.resolve("02-enfoque-con-tareas.png"))
        }

        runDesktopComposeUiTest(width = 1440, height = 900) {
            setContent {
                MaterialTheme {
                    timerLayout(
                        pomodoro = Pomodoro.BREAK,
                        phaseTitle = "Descanso corto",
                        timerLeft = 4 * 60 + 18,
                        completedPomodoros = 2,
                    )
                }
            }
            saveScreenshot(outputDirectory.resolve("03-descanso.png"))
        }

        runDesktopComposeUiTest(width = 1440, height = 900) {
            setContent {
                MaterialTheme {
                    timerLayout(
                        pomodoro = Pomodoro.FOCUS,
                        phaseTitle = "Enfoque · Pomodoro",
                        timerLeft = 25 * 60,
                        completedPomodoros = 0,
                        showSettings = true,
                    )
                }
            }
            saveScreenshot(outputDirectory.resolve("04-ajustes.png"))
        }
    }

    @Composable
    private fun timerLayout(
        pomodoro: Pomodoro,
        phaseTitle: String,
        timerLeft: Int,
        completedPomodoros: Int,
        showSettings: Boolean = false,
    ) = PomodoroDesktopLayout(
        pomodoro = pomodoro,
        phaseTitle = phaseTitle,
        isPlayPomodoro = pomodoro == Pomodoro.FOCUS,
        timerLeft = timerLeft,
        speed = TimerSpeed.NORMAL,
        isShowSettingsDialog = showSettings,
        configuration = configuration,
        completedPomodoros = completedPomodoros,
        tasks = tasks,
        selectedTaskId = 1,
        onPlayPause = {},
        onSpeedChange = {},
        onPhaseChange = {},
        onAddTask = {},
        onSelectTask = {},
        onToggleTask = {},
        onCompleteSelectedTask = {},
        onDeleteTask = {},
        onSettingsToggle = {},
        onSaveSettings = {},
        notificationSettings = NotificationSettingsState(
            permission = MacNotificationStatus.Enabled,
            test = MacNotificationTestStatus.Idle,
        ),
        onNotificationAction = {},
    )

    private fun androidx.compose.ui.test.ComposeUiTest.saveScreenshot(path: Path) {
        val bitmap = onRoot().captureToImage()
        assertEquals(1440, bitmap.width)
        assertEquals(900, bitmap.height)
        val png = Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
            ?: error("Could not encode App Store screenshot")
        Files.write(path, png.bytes)
    }
}
