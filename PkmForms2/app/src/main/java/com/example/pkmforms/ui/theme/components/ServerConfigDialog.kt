package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pkmforms.ui.theme.AppColors

@Composable
fun ServerConfigDialog(
    ipActual: String,
    onConfirm: (ip: String) -> Unit,
    onDismiss: () -> Unit
) {
    var ip      by remember { mutableStateOf(ipActual) }
    var ipError by remember { mutableStateOf(false) }

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
                text       = "Configuracion del servidor",
                color      = AppColors.Text,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text     = "Ingresa la IP de la PC donde corre el servidor. " +
                        "Puedes verla ejecutando: ip addr show",
                color    = AppColors.TextSecondary,
                fontSize = 12.sp
            )

            // Mostrar la URL completa que se usara
            Text(
                text     = "URL: http://$ip:8080",
                color    = AppColors.Accent,
                fontSize = 12.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text     = "IP del servidor",
                    color    = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value         = ip,
                    onValueChange = {
                        ip      = it
                        ipError = false
                    },
                    placeholder   = { Text("Ej: 192.168.1.25", color = AppColors.TextSecondary) },
                    isError       = ipError,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = AppColors.Text,
                        unfocusedTextColor   = AppColors.Text,
                        focusedBorderColor   = AppColors.Accent,
                        unfocusedBorderColor = AppColors.CodeBorder,
                        errorBorderColor     = AppColors.Error,
                        cursorColor          = AppColors.Accent
                    )
                )
                if (ipError) {
                    Text(
                        text     = "Ingresa una IP valida (Ej: 192.168.1.25)",
                        color    = AppColors.Error,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = AppColors.TextSecondary)
                }
                Button(
                    onClick = {
                        val ipLimpia = ip.trim()
                        // Validacion basica de IP
                        if (ipLimpia.isBlank() || !ipLimpia.matches(Regex("^[\\d\\.a-zA-Z\\-]+"))) {
                            ipError = true
                        } else {
                            onConfirm(ipLimpia)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape  = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = AppColors.Text)
                }
            }
        }
    }
}