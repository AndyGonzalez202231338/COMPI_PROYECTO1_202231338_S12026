package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.AppColors

@Composable
fun EditorActionBar(
    onReplace: () -> Unit,
    onAdd: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onReplace,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppColors.Text
            ),
            border = BorderStroke(1.dp, AppColors.TextSecondary)
        ) {
            Text("Replace", fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppColors.Text
            ),
            border = BorderStroke(1.dp, AppColors.TextSecondary)
        ) {
            Text("Add", fontSize = 13.sp)
        }

        Button(
            onClick = onFinish,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary
            )
        ) {
            Text("Finish", fontSize = 13.sp, color = AppColors.Text)
        }
    }
}