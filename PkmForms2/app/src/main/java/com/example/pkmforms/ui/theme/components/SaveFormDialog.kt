package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pkmforms.ui.theme.AppColors

@Composable
fun SaveFormDialog(
    onConfirm: (nombre: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre      by remember { mutableStateOf("") }
    var nombreError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Surface)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Guardar codigo como .form",
                color = AppColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "El archivo se guardara en Descargas/PKM_Forms/",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Nombre del archivo",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = false
                    },
                    placeholder = { Text("Ej: mi_formulario", color = AppColors.TextSecondary) },
                    isError = nombreError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = AppColors.Text,
                        unfocusedTextColor   = AppColors.Text,
                        focusedBorderColor   = AppColors.Accent,
                        unfocusedBorderColor = AppColors.CodeBorder,
                        errorBorderColor     = AppColors.Error,
                        cursorColor          = AppColors.Accent
                    )
                )
                if (nombreError) {
                    Text(
                        text = "El nombre es obligatorio",
                        color = AppColors.Error,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = AppColors.TextSecondary)
                }
                Button(
                    onClick = {
                        if (nombre.isBlank()) nombreError = true
                        else onConfirm(nombre.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = AppColors.Text)
                }
            }
        }
    }
}