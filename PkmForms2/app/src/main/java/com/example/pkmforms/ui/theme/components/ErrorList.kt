package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType
import com.example.pkmforms.ui.theme.AppColors

private val ColorLexicoHeader     = Color(0xFFE53935) // rojo - errores lexicos
private val ColorSintacticoHeader = Color(0xFFFF9800) // naranja - errores sintacticos
private val ColorRowOdd           = Color(0xFF1A1A2F)
private val ColorRowEven          = Color(0xFF12121F)

@Composable
fun ErrorList(
    errors: List<ErrorToken>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .background(Color(0xFF0D0D1A))
    ) {
        // Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2F))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Errores encontrados: ${errors.size}",
                color = AppColors.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = AppColors.Text
                )
            }
        }

        // Cabecera de columnas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252540))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Lexema",
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(70.dp)
            )
            Text(
                text = "Linea",
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(45.dp)
            )
            Text(
                text = "Col",
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = "Tipo",
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = "Descripcion",
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(color = AppColors.CodeBorder, thickness = 1.dp)

        // Lista de errores
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(errors.mapIndexed { i, e -> i to e }) { (index, error) ->
                ErrorRow(
                    error = error,
                    background = if (index % 2 == 0) ColorRowEven else ColorRowOdd
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(
    error: ErrorToken,
    background: Color
) {
    val typeColor = when (error.type) {
        ErrorType.LEXICO     -> ColorLexicoHeader
        ErrorType.SINTACTICO -> ColorSintacticoHeader
    }
    val typeLabel = when (error.type) {
        ErrorType.LEXICO     -> "Lexico"
        ErrorType.SINTACTICO -> "Sintactico"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = error.lexeme.ifBlank { "-" },
            color = AppColors.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(70.dp),
            maxLines = 1
        )
        Text(
            text = "${error.line}",
            color = AppColors.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(45.dp)
        )
        Text(
            text = "${error.column}",
            color = AppColors.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = typeLabel,
            color = typeColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = error.description,
            color = AppColors.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
    Divider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
}