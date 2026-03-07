package com.example.pkmforms.ui.theme

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.FormElement

object Routes {
    const val EDITOR    = "editor"
    const val OPTIONS   = "options"
    const val FORM_VIEW = "form_view"
}

@Composable
fun PkmFormsApp() {
    val navController = rememberNavController()

    var codeText by remember { mutableStateOf("") }
    var errors   by remember { mutableStateOf<List<ErrorToken>>(emptyList()) }
    var elements by remember { mutableStateOf<List<FormElement>>(emptyList()) }

    NavHost(
        navController = navController,
        startDestination = Routes.EDITOR
    ) {
        composable(Routes.EDITOR) {
            EditorScreen(
                codeText          = codeText,
                onCodeChange      = { codeText = it },
                errors            = errors,
                onErrorsDismiss   = { errors = emptyList() },
                onNavigateOptions = { navController.navigate(Routes.OPTIONS) },
                onNavigateFormView = { result ->
                    elements = result
                    errors   = emptyList()
                    navController.navigate(Routes.FORM_VIEW)
                },
                onErrorsFound = { found ->
                    errors = found
                }
            )
        }

        composable(Routes.OPTIONS) {
            OptionsScreen(
                onBack          = { navController.popBackStack() },
                onOpenCodeFile  = { /* TODO */ },
                onSaveCodeFile  = { /* TODO */ },
                onOpenFormFile  = { /* TODO */ },
                onSaveFormFile  = { /* TODO */ },
                onFinish        = { navController.navigate(Routes.FORM_VIEW) }
            )
        }

        composable(Routes.FORM_VIEW) {
            FormViewScreen(
                elements          = elements,
                onBackToEditor    = { navController.popBackStack() },
                onNavigateOptions = { navController.navigate(Routes.OPTIONS) }
            )
        }
    }
}