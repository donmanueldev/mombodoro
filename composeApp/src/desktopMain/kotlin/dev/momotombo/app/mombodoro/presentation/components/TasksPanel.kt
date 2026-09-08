package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.data.FocusTask
import dev.momotombo.app.mombodoro.presentation.ui.theme.*
import kotlinx.coroutines.launch

private val taskRowShape = RoundedCornerShape(12.dp)
private val taskRowHeight = 68.dp
private val deleteActionWidth = 96.dp
private val deleteActionGap = 8.dp
private const val swipeOpenThreshold = 0.5f

private enum class TaskFilter(val label: String) {
    Pending("Pendientes"),
    All("Todas"),
    Completed("Completadas"),
}

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
    var selectedFilter by rememberSaveable { mutableStateOf(TaskFilter.Pending) }
    val visibleTasks = tasks.filter { task ->
        when (selectedFilter) {
            TaskFilter.Pending -> !task.isCompleted
            TaskFilter.All -> true
            TaskFilter.Completed -> task.isCompleted
        }
    }

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
                Text(
                    "Elige una para concentrarte",
                    color = AppMutedText,
                    fontFamily = GetFontPoppinsMedium(),
                    fontSize = 13.sp
                )
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
        if (tasks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            TaskFilterSelector(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )
            Spacer(Modifier.height(12.dp))
        }
        if (tasks.isEmpty()) {
            EmptyTasks()
        } else if (visibleTasks.isEmpty()) {
            EmptyFilteredTasks(selectedFilter)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(visibleTasks, key = { it.id }) { task ->
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
private fun TaskFilterSelector(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        color = AppSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppOutline),
    ) {
        Row {
            TaskFilter.entries.forEachIndexed { index, filter ->
                val isSelected = filter == selectedFilter
                val segmentShape = when (index) {
                    0 -> RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)
                    TaskFilter.entries.lastIndex -> RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) Color(0xFFF8E9E5) else Color.Transparent, segmentShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onFilterSelected(filter) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        filter.label,
                        color = if (isSelected) Color(0xFFC85B4D) else AppMutedText,
                        fontFamily = GetFontPoppinsMedium(),
                        fontSize = 12.sp,
                    )
                }
                if (index != TaskFilter.entries.lastIndex) {
                    VerticalDivider(color = AppOutline)
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
        }
    }
}

@Composable
private fun EmptyFilteredTasks(filter: TaskFilter) {
    val message = when (filter) {
        TaskFilter.Pending -> "No hay tareas pendientes"
        TaskFilter.Completed -> "No hay tareas completadas"
        TaskFilter.All -> "Aún no hay tareas"
    }

    Surface(color = AppSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AppOutline)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = AppMutedText, fontFamily = GetFontPoppinsMedium())
        }
    }
}

@Composable
private fun TaskRow(
    task: FocusTask,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val deleteActionWidthPx = with(density) { deleteActionWidth.toPx() }
    var horizontalOffset by remember(task.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val rowInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }
    val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    val isDeleteRevealed = horizontalOffset < -swipeOpenThreshold

    fun settleSwipe() {
        val targetOffset = if (horizontalOffset <= -deleteActionWidthPx / 2) -deleteActionWidthPx else 0f
        scope.launch {
            animate(
                initialValue = horizontalOffset,
                targetValue = targetOffset,
                animationSpec = tween(180),
            ) { value, _ -> horizontalOffset = value }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(taskRowHeight)) {
        val actionPaneWidth = with(density) { (-horizontalOffset).toDp() }
            .coerceIn(0.dp, deleteActionWidth)
        val revealFraction = actionPaneWidth / deleteActionWidth
        val actionGap = deleteActionGap * revealFraction
        val deleteButtonWidth = (actionPaneWidth - actionGap).coerceAtLeast(0.dp)
        val foregroundWidth = (maxWidth - actionPaneWidth).coerceAtLeast(0.dp)

        Row(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .width(foregroundWidth)
                    .fillMaxHeight()
                    .draggable(
                        state = rememberDraggableState { delta ->
                            horizontalOffset = (horizontalOffset + delta).coerceIn(-deleteActionWidthPx, 0f)
                        },
                        orientation = Orientation.Horizontal,
                        onDragStopped = { settleSwipe() },
                    )
                    .clickable(
                        enabled = !task.isCompleted,
                        interactionSource = rowInteractionSource,
                        indication = null,
                    ) {
                        if (isDeleteRevealed) horizontalOffset = 0f else onSelect()
                    },
                color = if (isSelected) Color(0xFFF8E9E5) else AppSurface,
                shape = taskRowShape,
                border = BorderStroke(1.dp, if (isSelected) Color(0xFFE9DCD8) else AppOutline),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC85B4D)),
                    )
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            task.title,
                            color = AppText,
                            fontFamily = GetFontPoppinsMedium(),
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        )
                        if (isSelected && !task.isCompleted) {
                            Text(
                                "Tarea seleccionada",
                                color = Color(0xFFC85B4D),
                                fontFamily = GetFontPoppinsMedium(),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            if (deleteButtonWidth > 0.dp) {
                Spacer(Modifier.width(actionGap))
                Surface(
                    modifier = Modifier.width(deleteButtonWidth).fillMaxHeight(),
                    shape = taskRowShape,
                    color = when {
                        isDeletePressed -> Color(0xFFAB4338)
                        isDeleteHovered -> Color(0xFFB94F43)
                        else -> Color(0xFFC85B4D)
                    },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = deleteInteractionSource,
                            indication = null,
                            onClick = onDelete,
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Text("Eliminar", color = Color.White, fontFamily = GetFontPoppinsSemiBold()) }
                }
            }
        }
    }
}
