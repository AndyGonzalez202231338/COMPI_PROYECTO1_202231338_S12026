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
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState   = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lineCount = remember(value.text) {
        if (value.text.isEmpty()) 1 else value.text.lines().size
    }

    Row(
        modifier = modifier
            .background(AppColors.CodeBackground, RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.CodeBorder, RoundedCornerShape(4.dp))
    ) {
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
                    text       = "${index + 1}",
                    color      = AppColors.LineNumber,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 13.sp,
                    lineHeight = 20.sp,
                    softWrap   = false
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(AppColors.CodeBorder)
        )

        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                color      = AppColors.Text
            ),
            cursorBrush         = SolidColor(AppColors.Accent),
            singleLine          = false,
            onTextLayout        = {},
            visualTransformation = SyntaxHighlightTransformation()
        )
    }
}