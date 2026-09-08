package dev.momotombo.app.mombodoro.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momotombo.app.mombodoro.presentation.ui.theme.GetFontPoppinsMedium

@Composable
fun SelectedTaskControl(title: String, accent: Color, onComplete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { contentDescription = "Completar tarea: $title" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = { if (it) onComplete() },
            colors = CheckboxDefaults.colors(checkedColor = accent),
        )
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp),
            color = accent,
            fontFamily = GetFontPoppinsMedium(),
            fontSize = 14.sp,
        )
    }
}
