package com.example.pkmforms.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pkmforms.analyzer.PkmParser
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.ui.theme.components.CodeEditor
import com.example.pkmforms.ui.theme.components.EditorActionBar
import com.example.pkmforms.ui.theme.components.ErrorList
import com.example.pkmforms.ui.theme.components.TopBar

@Composable
fun EditorScreen(
    codeText: String,
    onCodeChange: (String) -> Unit,
    errors: List<ErrorToken>,
    onErrorsDismiss: () -> Unit,
    onErrorsFound: (List<ErrorToken>) -> Unit,
    onNavigateOptions: () -> Unit,
    onNavigateFormView: (List<FormElement>) -> Unit
) {
    val parser = remember { PkmParser() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopBar(
            title = "PKM_FORMS",
            onMenuClick = onNavigateOptions
        )

        if (errors.isNotEmpty()) {
            ErrorList(
                errors    = errors,
                onDismiss = onErrorsDismiss
            )
        }

        CodeEditor(
            code         = codeText,
            onCodeChange = onCodeChange,
            modifier     = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        )

        EditorActionBar(
            onReplace = {
                // TODO: parse en modo replace
            },
            onAdd = {
                // TODO: parse en modo add
            },
            onFinish = {
                val result = parser.parse(codeText)
                if (result.errors.isEmpty()) {
                    onNavigateFormView(result.elements)
                } else {
                    onErrorsFound(
                        result.errors.sortedWith(
                            compareBy({ it.line }, { it.column })
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditorScreenPreview() {
    EditorScreen(
        codeText          = "",
        onCodeChange      = {},
        errors            = emptyList(),
        onErrorsDismiss   = {},
        onErrorsFound     = {},
        onNavigateOptions = {},
        onNavigateFormView = {}
    )
}