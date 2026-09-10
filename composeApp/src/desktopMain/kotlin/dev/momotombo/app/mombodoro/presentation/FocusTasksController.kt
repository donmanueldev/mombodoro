package dev.momotombo.app.mombodoro.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.FocusTaskStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Owns task interactions so composables do not perform persistence operations. */
class FocusTasksController {
    private val mutableTasks = mutableStateListOf<FocusTask>()
    private val mutationMutex = Mutex()
    private var store: FocusTaskStore? = null

    val tasks: List<FocusTask>
        get() = mutableTasks

    var selectedTaskId by mutableStateOf<Long?>(null)
        private set

    var isReady by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    suspend fun load(store: FocusTaskStore) = mutationMutex.withLock {
        isReady = false
        runOperation("No se pudieron cargar tus tareas.") {
            val loadedTasks = withContext(Dispatchers.IO) { store.loadAll() }
            val storedSelectedTaskId = withContext(Dispatchers.IO) { store.loadSelectedTaskId() }
            this.store = store
            mutableTasks.clear()
            mutableTasks += loadedTasks
            selectedTaskId = storedSelectedTaskId?.takeIf { selectedId ->
                loadedTasks.any { it.id == selectedId && !it.isCompleted }
            }
            if (storedSelectedTaskId != null && selectedTaskId == null) {
                withContext(Dispatchers.IO) { store.saveSelectedTaskId(null) }
            }
        }
        isReady = true
    }

    suspend fun add(title: String) = mutationMutex.withLock {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return@withLock

        val activeStore = store ?: return@withLock
        runOperation("No se pudo guardar la tarea.") {
            val task = withContext(Dispatchers.IO) { activeStore.add(normalizedTitle) }
            mutableTasks += task
            if (mutableTasks.none { it.id == selectedTaskId && !it.isCompleted }) {
                selectedTaskId = task.id
                withContext(Dispatchers.IO) { activeStore.saveSelectedTaskId(task.id) }
            }
        }
    }

    suspend fun select(id: Long) = mutationMutex.withLock {
        if (mutableTasks.any { it.id == id && !it.isCompleted }) {
            val activeStore = store ?: return@withLock
            runOperation("No se pudo seleccionar la tarea.") {
                withContext(Dispatchers.IO) { activeStore.saveSelectedTaskId(id) }
                selectedTaskId = id
            }
        }
    }

    suspend fun toggleCompletion(id: Long) = mutationMutex.withLock {
        val index = mutableTasks.indexOfFirst { it.id == id }
        if (index < 0) return@withLock

        val updatedTask = mutableTasks[index].copy(isCompleted = !mutableTasks[index].isCompleted)
        val activeStore = store ?: return@withLock
        runOperation("No se pudo actualizar la tarea.") {
            val clearsSelection = updatedTask.isCompleted && selectedTaskId == id
            withContext(Dispatchers.IO) {
                if (clearsSelection) {
                    activeStore.updateCompletionAndSelection(updatedTask.id, updatedTask.isCompleted, null)
                } else {
                    activeStore.updateCompletion(updatedTask.id, updatedTask.isCompleted)
                }
            }
            mutableTasks[index] = updatedTask
            if (clearsSelection) selectedTaskId = null
        }
    }

    suspend fun delete(id: Long) = mutationMutex.withLock {
        if (mutableTasks.none { it.id == id }) return@withLock

        val activeStore = store ?: return@withLock
        runOperation("No se pudo eliminar la tarea.") {
            val replacementId = if (selectedTaskId == id) {
                mutableTasks.firstOrNull { it.id != id && !it.isCompleted }?.id
            } else {
                selectedTaskId
            }
            withContext(Dispatchers.IO) {
                if (selectedTaskId == id) activeStore.deleteAndSelect(id, replacementId)
                else activeStore.delete(id)
            }
            mutableTasks.removeAll { it.id == id }
            if (selectedTaskId == id) {
                selectedTaskId = replacementId
            }
        }
    }

    fun dismissError() {
        errorMessage = null
    }

    private suspend fun runOperation(message: String, operation: suspend () -> Unit) {
        try {
            operation()
            errorMessage = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            errorMessage = message
        }
    }
}
