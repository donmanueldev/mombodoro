package dev.momotombo.app.mombodoro.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager

/** Almacenamiento local de las tareas de enfoque para la aplicación de escritorio. */
interface FocusTaskStore {
    fun loadAll(): List<FocusTask>
    fun loadSelectedTaskId(): Long?
    fun add(title: String): FocusTask
    fun updateCompletion(id: Long, isCompleted: Boolean)
    fun updateCompletionAndSelection(id: Long, isCompleted: Boolean, selectedTaskId: Long?) {
        updateCompletion(id, isCompleted)
        saveSelectedTaskId(selectedTaskId)
    }
    fun saveSelectedTaskId(id: Long?)
    fun delete(id: Long)
    fun deleteAndSelect(id: Long, selectedTaskId: Long?) {
        delete(id)
        saveSelectedTaskId(selectedTaskId)
    }
}

class FocusTaskRepository private constructor(private val databasePath: Path) : FocusTaskStore {

    init {
        Class.forName("org.sqlite.JDBC")
        Files.createDirectories(databasePath.parent)
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS focus_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS focus_task_selection (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        selected_task_id INTEGER
                    )
                    """.trimIndent()
                )
            }
        }
    }

    override fun loadAll(): List<FocusTask> = connection().use { connection ->
        connection.prepareStatement(
            "SELECT id, title, is_completed FROM focus_tasks ORDER BY is_completed ASC, created_at ASC, id ASC"
        ).use { statement ->
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            FocusTask(
                                id = result.getLong("id"),
                                title = result.getString("title"),
                                isCompleted = result.getInt("is_completed") == 1,
                            )
                        )
                    }
                }
            }
        }
    }

    override fun loadSelectedTaskId(): Long? = connection().use { connection ->
        connection.prepareStatement(
            "SELECT selected_task_id FROM focus_task_selection WHERE id = 1"
        ).use { statement ->
            statement.executeQuery().use { result ->
                if (result.next()) result.getLong("selected_task_id").takeUnless { result.wasNull() } else null
            }
        }
    }

    override fun add(title: String): FocusTask = connection().use { connection ->
        connection.prepareStatement(
            "INSERT INTO focus_tasks (title, created_at) VALUES (?, ?)",
            java.sql.Statement.RETURN_GENERATED_KEYS,
        ).use { statement ->
            statement.setString(1, title)
            statement.setLong(2, System.currentTimeMillis())
            statement.executeUpdate()
            statement.generatedKeys.use { keys ->
                check(keys.next()) { "SQLite no devolvió el id de la tarea creada." }
                FocusTask(id = keys.getLong(1), title = title)
            }
        }
    }

    override fun updateCompletion(id: Long, isCompleted: Boolean) {
        connection().use { connection ->
            connection.prepareStatement("UPDATE focus_tasks SET is_completed = ? WHERE id = ?").use { statement ->
                statement.setInt(1, if (isCompleted) 1 else 0)
                statement.setLong(2, id)
                statement.executeUpdate()
            }
        }
    }

    override fun updateCompletionAndSelection(id: Long, isCompleted: Boolean, selectedTaskId: Long?) {
        connection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("UPDATE focus_tasks SET is_completed = ? WHERE id = ?").use { statement ->
                    statement.setInt(1, if (isCompleted) 1 else 0)
                    statement.setLong(2, id)
                    statement.executeUpdate()
                }
                saveSelectedTaskId(connection, selectedTaskId)
                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }

    override fun saveSelectedTaskId(id: Long?) {
        connection().use { connection ->
            saveSelectedTaskId(connection, id)
        }
    }

    override fun delete(id: Long) {
        connection().use { connection ->
            connection.prepareStatement("DELETE FROM focus_tasks WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate()
            }
        }
    }

    override fun deleteAndSelect(id: Long, selectedTaskId: Long?) {
        connection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("DELETE FROM focus_tasks WHERE id = ?").use { statement ->
                    statement.setLong(1, id)
                    statement.executeUpdate()
                }
                saveSelectedTaskId(connection, selectedTaskId)
                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }

    private fun saveSelectedTaskId(connection: java.sql.Connection, id: Long?) {
        connection.prepareStatement(
            """
            INSERT INTO focus_task_selection (id, selected_task_id) VALUES (1, ?)
            ON CONFLICT(id) DO UPDATE SET selected_task_id = excluded.selected_task_id
            """.trimIndent()
        ).use { statement ->
            if (id == null) statement.setNull(1, java.sql.Types.INTEGER) else statement.setLong(1, id)
            statement.executeUpdate()
        }
    }

    private fun connection() = DriverManager.getConnection("jdbc:sqlite:${databasePath.toAbsolutePath()}")

    companion object {
        fun openDefault(): FocusTaskRepository {
            val userHome = Paths.get(System.getProperty("user.home"))
            val directory = if (System.getProperty("os.name") == "Mac OS X") {
                userHome.resolve("Library/Application Support/Mombodoro")
            } else {
                userHome.resolve(".mombodoro")
            }
            return FocusTaskRepository(directory.resolve("mombodoro.db"))
        }

        fun open(databasePath: Path): FocusTaskRepository = FocusTaskRepository(databasePath)
    }
}
