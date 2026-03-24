package com.example.pkmforms.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pkmforms.analyzer.PkmParser
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.ui.theme.components.CodeEditor
import com.example.pkmforms.ui.theme.components.EditorActionBar
import com.example.pkmforms.ui.theme.components.ErrorList
import com.example.pkmforms.ui.theme.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    codeText           : String,
    onCodeChange       : (String) -> Unit,
    errors             : List<ErrorToken>,
    onErrorsDismiss    : () -> Unit,
    onErrorsFound      : (List<ErrorToken>) -> Unit,
    onNavigateOptions  : () -> Unit,
    onNavigateFormView : (List<FormElement>) -> Unit,
    onFinish           : () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = codeText))
    }

    LaunchedEffect(codeText) {
        if (codeText != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = codeText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopBar(
            title       = "PKM_FORMS",
            onMenuClick = onNavigateOptions
        )

        if (errors.isNotEmpty()) {
            ErrorList(
                errors    = errors,
                onDismiss = onErrorsDismiss
            )
        }

        CodeEditor(
            value         = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onCodeChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        )

        EditorActionBar(
            onFinish  = { onFinish() },
            onTemplateInsert = { template ->
                val cursor  = textFieldValue.selection.start
                val before  = textFieldValue.text.substring(0, cursor)
                val after   = textFieldValue.text.substring(cursor)
                val newText = before + template + after
                val newPos  = cursor + template.length
                textFieldValue = TextFieldValue(
                    text      = newText,
                    selection = TextRange(newPos)
                )
                onCodeChange(newText)
            },
            onColorInsert = { colorString ->
                val cursor  = textFieldValue.selection.start
                val before  = textFieldValue.text.substring(0, cursor)
                val after   = textFieldValue.text.substring(cursor)
                val newText = before + colorString + after
                val newPos  = cursor + colorString.length
                textFieldValue = TextFieldValue(
                    text      = newText,
                    selection = TextRange(newPos)
                )
                onCodeChange(newText)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditorScreenPreview() {
    EditorScreen(
        codeText           = "",
        onCodeChange       = {},
        errors             = emptyList(),
        onErrorsDismiss    = {},
        onErrorsFound      = {},
        onNavigateOptions  = {},
        onNavigateFormView = {}
    )
}