package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.momotombo.app.mombodoro.MacNotificationStatus
import dev.momotombo.app.mombodoro.MacNotificationTestStatus
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsAction
import dev.momotombo.app.mombodoro.presentation.NotificationSettingsState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsPanelTest {
    private val configuration = PomodoroConfiguration(
        name = "Pomodoro",
        focusSeconds = 25 * 60,
        shortBreakSeconds = 5 * 60,
        longBreakSeconds = 15 * 60,
        cyclesBeforeLongBreak = 4,
    )

    @Test
    fun `notification test button invokes the native test`() = runComposeUiTest {
        var testRequests = 0
        setContent {
            SettingsPanel(
                configuration = configuration,
                onClose = {},
                onSave = {},
                notificationSettings = NotificationSettingsState(
                    permission = MacNotificationStatus.Enabled,
                    test = MacNotificationTestStatus.Idle,
                ),
                onNotificationAction = { action ->
                    if (action == NotificationSettingsAction.Test) testRequests++
                },
                modifier = Modifier.width(420.dp),
            )
        }

        onNodeWithText("Enviar prueba").performClick()

        assertEquals(1, testRequests)
    }

    @Test
    fun `notification test shows the delivered result`() = runComposeUiTest {
        setContent {
            SettingsPanel(
                configuration = configuration,
                onClose = {},
                onSave = {},
                notificationSettings = NotificationSettingsState(
                    permission = MacNotificationStatus.Enabled,
                    test = MacNotificationTestStatus.Delivered,
                ),
                onNotificationAction = {},
                modifier = Modifier.width(420.dp),
            )
        }

        onNodeWithText("Prueba entregada a macOS.").assertIsDisplayed()
    }

    @Test
    fun `notification test result copy distinguishes denied and failed`() {
        assertEquals(
            "Prueba denegada por macOS.",
            NotificationSettingsState(MacNotificationStatus.Disabled, MacNotificationTestStatus.Denied).testResultLabel,
        )
        assertEquals(
            "La prueba falló.",
            NotificationSettingsState(MacNotificationStatus.Enabled, MacNotificationTestStatus.Failed).testResultLabel,
        )
    }
}
