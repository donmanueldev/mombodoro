package dev.momotombo.app.mombodoro

import dev.momotombo.app.mombodoro.data.FocusTaskRepository
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusTaskRepositoryTest {

    @Test
    fun `selection migration supports existing task databases`() {
        val databasePath = Files.createTempDirectory("mombodoro-selection").resolve("tasks.db")
        DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE focus_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    "INSERT INTO focus_tasks (title, created_at) VALUES ('Preparar presentación', 1)"
                )
            }
        }

        val repository = FocusTaskRepository.open(databasePath)

        repository.saveSelectedTaskId(1)

        assertEquals(1, FocusTaskRepository.open(databasePath).loadSelectedTaskId())
    }

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

    @Test
    fun `completing and deleting selected tasks update selection atomically`() {
        val databasePath = Files.createTempDirectory("mombodoro-task-transactions").resolve("tasks.db")
        val repository = FocusTaskRepository.open(databasePath)
        val first = repository.add("Primera")
        val second = repository.add("Segunda")
        repository.saveSelectedTaskId(first.id)

        repository.updateCompletionAndSelection(first.id, isCompleted = true, selectedTaskId = null)

        assertEquals(null, repository.loadSelectedTaskId())
        assertTrue(repository.loadAll().single { it.id == first.id }.isCompleted)

        repository.saveSelectedTaskId(second.id)
        repository.deleteAndSelect(second.id, selectedTaskId = null)

        assertEquals(null, repository.loadSelectedTaskId())
        assertFalse(repository.loadAll().any { it.id == second.id })
    }
}
