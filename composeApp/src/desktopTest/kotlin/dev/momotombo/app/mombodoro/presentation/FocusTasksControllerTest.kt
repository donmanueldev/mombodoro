package dev.momotombo.app.mombodoro.presentation

import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.FocusTaskStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class FocusTasksControllerTest {
    @Test
    fun `task mutations persist through the store and keep the selected task valid`() = runBlocking {
        val store = InMemoryFocusTaskStore()
        val controller = FocusTasksController()
        controller.load(store)

        controller.add("  Preparar presentación  ")
        val taskId = controller.selectedTaskId!!
        controller.toggleCompletion(taskId)
        controller.delete(taskId)

        assertEquals(emptyList(), controller.tasks)
        assertEquals(null, controller.selectedTaskId)
        assertEquals(emptyList(), store.tasks)
    }

    private class InMemoryFocusTaskStore : FocusTaskStore {
        val tasks = mutableListOf<FocusTask>()
        private var nextId = 1L

        override fun loadAll(): List<FocusTask> = tasks.toList()

        override fun add(title: String): FocusTask = FocusTask(nextId++, title).also(tasks::add)

        override fun updateCompletion(id: Long, isCompleted: Boolean) {
            val index = tasks.indexOfFirst { it.id == id }
            tasks[index] = tasks[index].copy(isCompleted = isCompleted)
        }

        override fun delete(id: Long) {
            tasks.removeAll { it.id == id }
        }
    }
}
