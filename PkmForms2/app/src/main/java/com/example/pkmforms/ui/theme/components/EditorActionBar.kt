package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.AppColors

@Composable
fun EditorActionBar(
    onReplace     : () -> Unit,
    onAdd         : () -> Unit,
    onFinish      : () -> Unit,
    onColorInsert : (String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { colorString ->
                onColorInsert(colorString)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActionChip(
            label    = "Replace",
            color    = Color(0xFF374151),
            onClick  = onReplace,
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label    = "Add",
            color    = Color(0xFF374151),
            onClick  = onAdd,
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label    = "Color",
            color    = Color(0xFF6D28D9),
            onClick  = { showColorPicker = true },
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label    = "Finish",
            color    = AppColors.Accent,
            onClick  = onFinish,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionChip(
    label    : String,
    color    : Color,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(36.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = color),
        shape          = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            color      = Color.White
        )
    }
}