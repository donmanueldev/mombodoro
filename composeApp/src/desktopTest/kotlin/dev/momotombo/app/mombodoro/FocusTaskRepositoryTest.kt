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
}
