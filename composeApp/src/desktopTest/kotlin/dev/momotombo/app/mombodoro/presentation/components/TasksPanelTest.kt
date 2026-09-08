package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TasksPanelTest {
    @Test
    fun `selected task control exposes an accessible completion checkbox`() = runComposeUiTest {
        var completed = 0

        setContent {
            SelectedTaskControl(
                title = "Enviar el correo",
                accent = AppText,
                onComplete = { completed++ },
            )
        }

        onNodeWithContentDescription("Completar tarea: Enviar el correo").assertExists()
        onNode(isToggleable()).performClick()

        assertEquals(1, completed)
    }

    @Test
    fun `task row reveals delete after a left swipe`() = runComposeUiTest {
        setContent {
            TasksPanel(
                tasks = listOf(FocusTask(id = 1, title = "Enviar el correo")),
                selectedTaskId = 1,
                onAddTask = {},
                onSelectTask = {},
                onToggleTask = {},
                onDeleteTask = {},
            )
        }

        onNodeWithText("Eliminar").assertDoesNotExist()
        onNodeWithText("Enviar el correo").performTouchInput { swipeLeft() }

        onNodeWithText("Enviar el correo").assertIsDisplayed()
        onNodeWithText("Eliminar").assertIsDisplayed()
    }

    @Test
    fun `task filters default to pending and show completed tasks on demand`() = runComposeUiTest {
        setContent {
            TasksPanel(
                tasks = listOf(
                    FocusTask(id = 1, title = "Pendiente"),
                    FocusTask(id = 2, title = "Completada", isCompleted = true),
                ),
                selectedTaskId = 1,
                onAddTask = {},
                onSelectTask = {},
                onToggleTask = {},
                onDeleteTask = {},
            )
        }

        onNodeWithText("Pendiente").assertIsDisplayed()
        onNodeWithText("Completada").assertDoesNotExist()

        onNodeWithText("Completadas").performClick()

        onNodeWithText("Pendiente").assertDoesNotExist()
        onNodeWithText("Completada").assertIsDisplayed()
    }
}
