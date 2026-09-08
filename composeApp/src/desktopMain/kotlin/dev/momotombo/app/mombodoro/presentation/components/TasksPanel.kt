package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsSemiBold

@Composable
fun TasksPanel(
    tasks: List<FocusTask>,
    selectedTaskId: Long?,
    textColor: Color,
    surfaceColor: Color,
    onAddTask: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    onToggleTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    fun submitTask() {
        if (draft.isBlank()) return
        onAddTask(draft)
        draft = ""
    }

    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        color = surfaceColor.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tareas", fontFamily = GetFontPoppinsSemiBold(), color = textColor)
            HorizontalDivider(color = textColor.copy(alpha = 0.35f))

            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    isSelected = task.id == selectedTaskId,
                    textColor = textColor,
                    onSelect = { onSelectTask(task.id) },
                    onToggle = { onToggleTask(task.id) },
                    onDelete = { onDeleteTask(task.id) },
                )
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submitTask() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            submitTask()
                            true
                        } else {
                            false
                        }
                    },
                label = { Text("¿En qué vas a trabajar?") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.55f),
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor.copy(alpha = 0.75f),
                ),
            )
            Button(
                onClick = {
                    submitTask()
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = surfaceColor,
                ),
            ) { Text("+ Agregar tarea", fontFamily = GetFontPoppinsSemiBold()) }
        }
    }
}

@Composable
private fun TaskRow(
    task: FocusTask,
    isSelected: Boolean,
    textColor: Color,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = if (isSelected) textColor.copy(alpha = 0.18f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                color = textColor,
                fontFamily = GetFontPoppinsMedium(),
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            )
            TextButton(onClick = onToggle) { Text(if (task.isCompleted) "Reabrir" else "Hecha") }
            TextButton(onClick = onDelete) { Text("×") }
        }
    }
}
