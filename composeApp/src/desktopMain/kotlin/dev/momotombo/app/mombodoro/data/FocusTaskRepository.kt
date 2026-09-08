package dev.momotombo.app.mombodoro.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager

/** Almacenamiento local de las tareas de enfoque para la aplicación de escritorio. */
class FocusTaskRepository private constructor(private val databasePath: Path) {

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
            }
        }
    }

    fun loadAll(): List<FocusTask> = connection().use { connection ->
        connection.prepareStatement(
            "SELECT id, title, is_completed FROM focus_tasks ORDER BY is_completed ASC, created_at ASC"
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

    fun add(title: String): FocusTask = connection().use { connection ->
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

    fun updateCompletion(id: Long, isCompleted: Boolean) {
        connection().use { connection ->
            connection.prepareStatement("UPDATE focus_tasks SET is_completed = ? WHERE id = ?").use { statement ->
                statement.setInt(1, if (isCompleted) 1 else 0)
                statement.setLong(2, id)
                statement.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        connection().use { connection ->
            connection.prepareStatement("DELETE FROM focus_tasks WHERE id = ?").use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate()
            }
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
