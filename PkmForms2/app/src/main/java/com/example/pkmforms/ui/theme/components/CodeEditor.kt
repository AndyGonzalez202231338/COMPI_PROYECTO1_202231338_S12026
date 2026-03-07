package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.AppColors

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // TextFieldValue permite manejar el texto y la posicion del cursor
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = code))
    }

    // Sincroniza cuando el codigo cambia desde afuera
    LaunchedEffect(code) {
        if (textFieldValue.text != code) {
            textFieldValue = textFieldValue.copy(text = code)
        }
    }

    val lineCount = remember(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) 1 else textFieldValue.text.lines().size
    }

    Row(
        modifier = modifier
            .background(AppColors.CodeBackground, RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.CodeBorder, RoundedCornerShape(4.dp))
    ) {
        // Columna de numeros de linea
        Column(
            modifier = Modifier
                .background(AppColors.LineNumberBackground)
                .fillMaxHeight()
                .width(36.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .verticalScroll(verticalScrollState, enabled = false)
        ) {
            repeat(lineCount) { index ->
                Text(
                    text = "${index + 1}",
                    color = AppColors.LineNumber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    softWrap = false
                )
            }
        }

        // Divisor vertical
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(AppColors.CodeBorder)
        )

        // Editor con syntax highlighting
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onCodeChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppColors.Text
            ),
            cursorBrush = SolidColor(AppColors.Accent),
            singleLine = false,
            onTextLayout = {},
            visualTransformation = SyntaxHighlightTransformation()
        )
    }
}