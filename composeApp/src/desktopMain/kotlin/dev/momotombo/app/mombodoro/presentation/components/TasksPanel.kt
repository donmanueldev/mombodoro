package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppMutedText
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppOutline
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppSurface
import dev.momotombo.app.mombodoro.presentation.ui.theme.AppText
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun TasksPanel(
    tasks: List<FocusTask>,
    selectedTaskId: Long?,
    onAddTask: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    onToggleTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    fun submitTask() {
        val title = draft.trim()
        if (title.isNotEmpty()) {
            onAddTask(title)
            draft = ""
        }
    }

    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tareas", color = AppText, fontFamily = GetFontPoppinsSemiBold(), fontSize = 22.sp)
                Text("Elige una para concentrarte", color = AppMutedText, fontFamily = GetFontPoppinsMedium(), fontSize = 13.sp)
            }
            if (onClose != null) {
                TextButton(onClick = onClose) { Text("Cerrar") }
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                    submitTask()
                    true
                } else {
                    false
                }
            },
            label = { Text("Nueva tarea") },
            placeholder = { Text("¿En qué vas a trabajar?") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppText,
                unfocusedTextColor = AppText,
                focusedBorderColor = AppText,
                unfocusedBorderColor = AppOutline,
                focusedLabelColor = AppText,
                unfocusedLabelColor = AppMutedText,
            ),
        )
        Button(
            onClick = ::submitTask,
            enabled = draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppText, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
        ) { Text("Añadir tarea", fontFamily = GetFontPoppinsSemiBold()) }
        HorizontalDivider(Modifier.padding(vertical = 20.dp), color = AppOutline)
        if (tasks.isEmpty()) {
            EmptyTasks()
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        isSelected = task.id == selectedTaskId,
                        onSelect = { onSelectTask(task.id) },
                        onToggle = { onToggleTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasks() {
    Surface(color = AppSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AppOutline)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Aún no hay tareas", color = AppText, fontFamily = GetFontPoppinsSemiBold())
            Text("Añade una arriba y selecciónala para empezar.", modifier = Modifier.padding(top = 6.dp), color = AppMutedText, fontFamily = GetFontPoppinsMedium(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun TaskRow(task: FocusTask, isSelected: Boolean, onSelect: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        color = if (isSelected) Color(0xFFF8E9E5) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = AppText,
                    fontFamily = GetFontPoppinsMedium(),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
                if (isSelected && !task.isCompleted) {
                    Text("Tarea seleccionada", color = Color(0xFFC85B4D), fontFamily = GetFontPoppinsMedium(), fontSize = 11.sp)
                }
            }
            TextButton(onClick = onToggle) { Text(if (task.isCompleted) "Reabrir" else "Hecha") }
            TextButton(onClick = onDelete) { Text("Eliminar") }
        }
    }
}
