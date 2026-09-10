package dev.momotombo.app.mombodoro.presentation

import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.FocusTaskStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FocusTasksControllerTest {
    @Test
    fun `task mutations persist through the store and keep the selected task valid`() = runBlocking {
        val store = InMemoryFocusTaskStore()
        val controller = FocusTasksController()
        controller.load(store)

        controller.add("  Preparar presentación  ")
        val taskId = assertNotNull(controller.selectedTaskId)
        controller.toggleCompletion(taskId)
        controller.delete(taskId)

        assertEquals(emptyList(), controller.tasks)
        assertEquals(null, controller.selectedTaskId)
        assertEquals(emptyList(), store.tasks)
    }

    @Test
    fun `adding a task keeps the current pending task selected`() = runBlocking {
        val controller = FocusTasksController()
        controller.load(InMemoryFocusTaskStore())

        controller.add("Preparar presentación")
        val firstTaskId = controller.selectedTaskId
        controller.add("Revisar diseño")

        assertEquals(firstTaskId, controller.selectedTaskId)
    }

    @Test
    fun `adding a task selects it when no task is active`() = runBlocking {
        val controller = FocusTasksController()
        controller.load(InMemoryFocusTaskStore())

        controller.add("Preparar presentación")
        val firstTaskId = assertNotNull(controller.selectedTaskId)
        controller.toggleCompletion(firstTaskId)
        controller.add("Revisar diseño")

        assertEquals("Revisar diseño", controller.tasks.single { it.id == controller.selectedTaskId }.title)
    }

    @Test
    fun `completing the active task clears the selection and completed tasks cannot be selected`() = runBlocking {
        val controller = FocusTasksController()
        controller.load(InMemoryFocusTaskStore())

        controller.add("Preparar presentación")
        val taskId = assertNotNull(controller.selectedTaskId)
        controller.toggleCompletion(taskId)
        controller.select(taskId)

        assertEquals(null, controller.selectedTaskId)
        assertTrue(controller.tasks.single().isCompleted)
    }

    @Test
    fun `deleting the selected task selects the next pending task`() = runBlocking {
        val controller = FocusTasksController()
        controller.load(InMemoryFocusTaskStore())

        controller.add("Preparar presentación")
        val firstTaskId = assertNotNull(controller.selectedTaskId)
        controller.add("Revisar diseño")
        val secondTaskId = controller.tasks.single { it.title == "Revisar diseño" }.id
        controller.delete(firstTaskId)

        assertEquals(secondTaskId, controller.selectedTaskId)
        assertFalse(controller.tasks.single().isCompleted)
    }

    @Test
    fun `reopening restores the last selected pending task`() = runBlocking {
        val store = InMemoryFocusTaskStore()
        val firstController = FocusTasksController()
        firstController.load(store)
        firstController.add("Preparar presentación")
        firstController.add("Revisar diseño")
        val selectedTaskId = firstController.tasks.single { it.title == "Revisar diseño" }.id

        firstController.select(selectedTaskId)

        val reopenedController = FocusTasksController()
        reopenedController.load(store)

        assertEquals(selectedTaskId, reopenedController.selectedTaskId)
    }

    @Test
    fun `reopening clears a selection whose task was completed`() = runBlocking {
        val store = InMemoryFocusTaskStore()
        val firstController = FocusTasksController()
        firstController.load(store)
        firstController.add("Preparar presentación")
        val selectedTaskId = assertNotNull(firstController.selectedTaskId)

        store.updateCompletion(selectedTaskId, isCompleted = true)

        val reopenedController = FocusTasksController()
        reopenedController.load(store)

        assertEquals(null, reopenedController.selectedTaskId)
    }

    @Test
    fun `reopening clears a selection whose task was deleted`() = runBlocking {
        val store = InMemoryFocusTaskStore()
        val firstController = FocusTasksController()
        firstController.load(store)
        firstController.add("Preparar presentación")
        val selectedTaskId = assertNotNull(firstController.selectedTaskId)

        store.delete(selectedTaskId)

        val reopenedController = FocusTasksController()
        reopenedController.load(store)

        assertEquals(null, reopenedController.selectedTaskId)
    }

    @Test
    fun `a storage failure is exposed without changing the current task state`() = runBlocking {
        val controller = FocusTasksController()
        controller.load(InMemoryFocusTaskStore())
        controller.add("Preparar presentación")
        val currentTasks = controller.tasks.toList()

        controller.load(FailingFocusTaskStore())

        assertEquals(currentTasks, controller.tasks)
        assertEquals("No se pudieron cargar tus tareas.", controller.errorMessage)
        assertTrue(controller.isReady)

        controller.dismissError()

        assertEquals(null, controller.errorMessage)
    }

    private class InMemoryFocusTaskStore : FocusTaskStore {
        val tasks = mutableListOf<FocusTask>()
        private var nextId = 1L
        private var selectedTaskId: Long? = null

        override fun loadAll(): List<FocusTask> = tasks.toList()

        override fun loadSelectedTaskId(): Long? = selectedTaskId

        override fun add(title: String): FocusTask = FocusTask(nextId++, title).also(tasks::add)

        override fun updateCompletion(id: Long, isCompleted: Boolean) {
            val index = tasks.indexOfFirst { it.id == id }
            tasks[index] = tasks[index].copy(isCompleted = isCompleted)
        }

        override fun saveSelectedTaskId(id: Long?) {
            selectedTaskId = id
        }

        override fun delete(id: Long) {
            tasks.removeAll { it.id == id }
        }
    }

    private class FailingFocusTaskStore : FocusTaskStore {
        override fun loadAll(): List<FocusTask> = error("Database unavailable")
        override fun loadSelectedTaskId(): Long? = null
        override fun add(title: String): FocusTask = error("Database unavailable")
        override fun updateCompletion(id: Long, isCompleted: Boolean) = error("Database unavailable")
        override fun saveSelectedTaskId(id: Long?) = error("Database unavailable")
        override fun delete(id: Long) = error("Database unavailable")
    }
}
