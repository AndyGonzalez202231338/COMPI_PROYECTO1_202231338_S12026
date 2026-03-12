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
fun SavePkmDialog(
    onConfirm: (nombre: String, author: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre      by remember { mutableStateOf("") }
    var author      by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nombreError by remember { mutableStateOf(false) }
    var authorError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Surface)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Titulo
            Text(
                text = "Guardar como .pkm",
                color = AppColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Estos datos se guardarán como reporte en el encabezado del archivo.",
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )

            // Campo Nombre del formulario
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Nombre del formulario",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = false
                    },
                    placeholder = { Text("Ej: Encuesta_2026", color = AppColors.TextSecondary) },
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
                        text = "El nombre del formulario es obligatorio",
                        color = AppColors.Error,
                        fontSize = 11.sp
                    )
                }
            }

            // Campo Author
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Autor",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = {
                        author = it
                        authorError = false
                    },
                    placeholder = { Text("Nombre del autor", color = AppColors.TextSecondary) },
                    isError = authorError,
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
                if (authorError) {
                    Text(
                        text = "El autor es obligatorio",
                        color = AppColors.Error,
                        fontSize = 11.sp
                    )
                }
            }

            // Campo Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Descripcion",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Descripcion del formulario (opcional)", color = AppColors.TextSecondary) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = AppColors.Text,
                        unfocusedTextColor   = AppColors.Text,
                        focusedBorderColor   = AppColors.Accent,
                        unfocusedBorderColor = AppColors.CodeBorder,
                        cursorColor          = AppColors.Accent
                    )
                )
            }

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = AppColors.TextSecondary)
                }
                Button(
                    onClick = {
                        nombreError = nombre.isBlank()
                        authorError = author.isBlank()
                        if (!nombreError && !authorError) {
                            onConfirm(nombre.trim(), author.trim(), description.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = AppColors.Text)
                }
            }
        }
    }
}