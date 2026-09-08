package dev.momotombo.app.mombodoro.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.data.FocusTaskStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns task interactions so composables do not perform persistence operations. */
class FocusTasksController {
    private val mutableTasks = mutableStateListOf<FocusTask>()
    private var store: FocusTaskStore? = null

    val tasks: List<FocusTask>
        get() = mutableTasks

    var selectedTaskId by mutableStateOf<Long?>(null)
        private set

    var isReady by mutableStateOf(false)
        private set

    suspend fun load(store: FocusTaskStore) {
        val loadedTasks = withContext(Dispatchers.IO) { store.loadAll() }
        this.store = store
        mutableTasks.clear()
        mutableTasks += loadedTasks
        isReady = true
    }

    suspend fun add(title: String) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return

        val activeStore = store ?: return
        val task = withContext(Dispatchers.IO) { activeStore.add(normalizedTitle) }
        mutableTasks += task
        selectedTaskId = task.id
    }

    fun select(id: Long) {
        if (mutableTasks.any { it.id == id }) selectedTaskId = id
    }

    suspend fun toggleCompletion(id: Long) {
        val index = mutableTasks.indexOfFirst { it.id == id }
        if (index < 0) return

        val updatedTask = mutableTasks[index].copy(isCompleted = !mutableTasks[index].isCompleted)
        val activeStore = store ?: return
        withContext(Dispatchers.IO) { activeStore.updateCompletion(updatedTask.id, updatedTask.isCompleted) }
        mutableTasks[index] = updatedTask
    }

    suspend fun delete(id: Long) {
        if (mutableTasks.none { it.id == id }) return

        val activeStore = store ?: return
        withContext(Dispatchers.IO) { activeStore.delete(id) }
        mutableTasks.removeAll { it.id == id }
        if (selectedTaskId == id) selectedTaskId = mutableTasks.firstOrNull { !it.isCompleted }?.id
    }

}
