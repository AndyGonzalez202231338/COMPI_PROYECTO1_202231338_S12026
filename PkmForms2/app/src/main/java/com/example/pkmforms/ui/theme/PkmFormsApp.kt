package com.example.pkmforms.ui.theme

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pkmforms.analyzer.PkmImporter
import com.example.pkmforms.analyzer.PkmParser
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.FormElement

object Routes {
    const val EDITOR       = "editor"
    const val OPTIONS      = "options"
    const val FORM_VIEW    = "form_view"
    const val EXPLORE      = "explore"
}

@Composable
fun PkmFormsApp() {
    val navController = rememberNavController()

    var codeText          by remember { mutableStateOf("") }
    var errors            by remember { mutableStateOf<List<ErrorToken>>(emptyList()) }
    var elements          by remember { mutableStateOf<List<FormElement>>(emptyList()) }
    var hasErrors         by remember { mutableStateOf(false) }
    var modoContestacion  by remember { mutableStateOf(false) }
    val parser            = remember { PkmParser() }

    NavHost(
        navController    = navController,
        startDestination = Routes.EDITOR
    ) {
        composable(Routes.EDITOR) {
            EditorScreen(
                codeText           = codeText,
                onCodeChange       = { codeText = it },
                errors             = errors,
                onErrorsDismiss    = { errors = emptyList() },
                onNavigateOptions  = {
                    val result = parser.parse(codeText)
                    elements   = result.elements
                    errors     = result.errors
                    hasErrors  = result.errors.any {
                        it.type != com.example.pkmforms.analyzer.model.ErrorType.ADVERTENCIA
                    }
                    navController.navigate(Routes.OPTIONS)
                },
                onNavigateFormView = { result ->
                    elements         = result
                    errors           = emptyList()
                    hasErrors        = false
                    modoContestacion = false
                    navController.navigate(Routes.FORM_VIEW)
                },
                onErrorsFound = { found -> errors = found }
            )
        }

        composable(Routes.OPTIONS) {
            OptionsScreen(
                onBack           = { navController.popBackStack() },
                onOpenCodeFile   = {},
                onSaveCodeFile   = {},
                onOpenFormFile   = {},
                onExploreServer  = { navController.navigate(Routes.EXPLORE) },
                onFinish         = {
                    modoContestacion = false
                    navController.navigate(Routes.FORM_VIEW)
                },
                formElements      = elements,
                codeText          = codeText,
                hasErrors         = hasErrors,
                onCodeLoaded      = { codigo ->
                    codeText  = codigo
                    hasErrors = false
                },
                onPkmLocalCargado = { elementosCargados ->
                    elements         = elementosCargados
                    modoContestacion = true
                    navController.navigate(Routes.FORM_VIEW)
                }
            )
        }

        composable(Routes.FORM_VIEW) {
            FormViewScreen(
                elements          = elements,
                onBackToEditor    = { navController.popBackStack() },
                onNavigateOptions = { navController.navigate(Routes.OPTIONS) },
                modoContestacion  = modoContestacion,
                onSent            = {
                    modoContestacion = false
                    navController.popBackStack(Routes.EDITOR, inclusive = false)
                }
            )
        }

        composable(Routes.EXPLORE) {
            ExploreFormsScreen(
                onBack           = { navController.popBackStack() },
                // Formulario descargado del servidor  para cintestar formularios descargados
                onFormDescargado = { contenidoPkm ->
                    val resultado = PkmImporter.importar(contenidoPkm)
                    elements         = resultado.elementos
                    hasErrors        = false
                    modoContestacion = true
                    navController.navigate(Routes.FORM_VIEW)
                }
            )
        }
    }
}