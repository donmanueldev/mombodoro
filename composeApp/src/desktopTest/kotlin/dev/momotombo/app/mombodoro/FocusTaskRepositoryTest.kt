package dev.momotombo.app.mombodoro

import dev.momotombo.app.mombodoro.data.FocusTaskRepository
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusTaskRepositoryTest {

    @Test
    fun `tasks survive reopening the local SQLite database`() {
        val databasePath = Files.createTempDirectory("mombodoro-tasks").resolve("tasks.db")
        val repository = FocusTaskRepository.open(databasePath)

        val task = repository.add("Preparar presentación")
        repository.updateCompletion(task.id, isCompleted = true)

        val reopenedRepository = FocusTaskRepository.open(databasePath)
        val persistedTask = reopenedRepository.loadAll().single()

        assertEquals("Preparar presentación", persistedTask.title)
        assertTrue(persistedTask.isCompleted)

        reopenedRepository.delete(task.id)

        assertFalse(reopenedRepository.loadAll().isNotEmpty())
    }
}
