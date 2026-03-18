package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val ColorLexicoHeader     = Color(0xFFE53935)
private val ColorSintacticoHeader = Color(0xFFFF9800)
private val ColorSemanticoHeader  = Color(0xFF4CAF50)
private val ColorRowOdd           = Color(0xFF1A1A2F)
private val ColorRowEven          = Color(0xFF12121F)

private val W_LEXEMA      = 90.dp
private val W_LINEA       = 50.dp
private val W_COL         = 45.dp
private val W_TIPO        = 90.dp
private val W_DESCRIPCION = 420.dp
private val ROW_TOTAL     = 695.dp

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
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = AppColors.Text)
            }
        }

        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252540))
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "Lexema",      color = AppColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_LEXEMA))
            Text(text = "Linea",       color = AppColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_LINEA))
            Text(text = "Col",         color = AppColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_COL))
            Text(text = "Tipo",        color = AppColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_TIPO))
            Text(text = "Descripcion", color = AppColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_DESCRIPCION))
        }

        Divider(color = AppColors.CodeBorder, thickness = 1.dp)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(errors.mapIndexed { i, e -> i to e }) { (index, error) ->
                ErrorRow(
                    error       = error,
                    background  = if (index % 2 == 0) ColorRowEven else ColorRowOdd,
                    scrollState = scrollState
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(
    error: ErrorToken,
    background: Color,
    scrollState: ScrollState
) {
    val typeColor = when (error.type) {
        ErrorType.LEXICO       -> ColorLexicoHeader
        ErrorType.SINTACTICO   -> ColorSintacticoHeader
        ErrorType.SEMANTICO    -> ColorSemanticoHeader
        ErrorType.ADVERTENCIA  -> Color(0xFFFFEB3B)
    }
    val typeLabel = when (error.type) {
        ErrorType.LEXICO       -> "Lexico"
        ErrorType.SINTACTICO   -> "Sintactico"
        ErrorType.SEMANTICO    -> "Semantico"
        ErrorType.ADVERTENCIA  -> "Advertencia"
    }

    Row(
        modifier = Modifier
            .width(ROW_TOTAL)
            .horizontalScroll(scrollState)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = error.lexeme.ifBlank { "-" }, color = AppColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_LEXEMA),      maxLines = 1)
        Text(text = "${error.line}",               color = AppColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_LINEA))
        Text(text = "${error.column}",             color = AppColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_COL))
        Text(text = typeLabel,                     color = typeColor,      fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_TIPO),        fontWeight = FontWeight.Bold)
        Text(text = error.description,             color = AppColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(W_DESCRIPCION))
    }
    Divider(color = Color(0xFF1E1E3A), thickness = 0.5.dp)
}