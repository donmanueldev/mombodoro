package dev.momotombo.app.mombodoro.data

data class FocusTask(
    val id: Long,
    val title: String,
    val isCompleted: Boolean = false,
)
