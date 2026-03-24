package com.example.pkmforms.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pkmforms.analyzer.PkmImporter
import com.example.pkmforms.analyzer.PkmParser
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType
import com.example.pkmforms.analyzer.model.FormElement
import kotlinx.coroutines.launch

object Routes {
    const val EDITOR    = "editor"
    const val OPTIONS   = "options"
    const val FORM_VIEW = "form_view"
    const val EXPLORE   = "explore"
}

@Composable
fun PkmFormsApp() {
    val navController    = rememberNavController()
    val scope            = rememberCoroutineScope()

    var codeText         by remember { mutableStateOf("") }
    var errors           by remember { mutableStateOf<List<ErrorToken>>(emptyList()) }
    var elements         by remember { mutableStateOf<List<FormElement>>(emptyList()) }
    var hasErrors        by remember { mutableStateOf(false) }
    var modoContestacion by remember { mutableStateOf(false) }
    var parseando        by remember { mutableStateOf(false) }
    val parser           = remember { PkmParser() }

    // Overlay de carga mientras se parsea (puede llamar a PokeAPI)
    if (parseando) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando...", color = Color.White, fontSize = 14.sp)
            }
        }
        return
    }

    NavHost(
        navController    = navController,
        startDestination = Routes.EDITOR
    ) {
        composable(Routes.EDITOR) {
            EditorScreen(
                codeText          = codeText,
                onCodeChange      = { codeText = it },
                errors            = errors,
                onErrorsDismiss   = { errors = emptyList() },
                onNavigateOptions = {
                    scope.launch {
                        parseando = true
                        val result = parser.parse(codeText)
                        elements   = result.elements
                        errors     = result.errors
                        hasErrors  = result.errors.any {
                            it.type != ErrorType.ADVERTENCIA
                        }
                        parseando = false
                        navController.navigate(Routes.OPTIONS)
                    }
                },
                onFinish = {
                    scope.launch {
                        parseando = true
                        val result = parser.parse(codeText)
                        parseando = false
                        if (result.errors.isEmpty()) {
                            elements         = result.elements
                            hasErrors        = false
                            modoContestacion = false
                            navController.navigate(Routes.FORM_VIEW)
                        } else {
                            errors    = result.errors.sortedWith(compareBy({ it.line }, { it.column }))
                            hasErrors = true
                        }
                    }
                },
               /* onReplace = {
                    // Reemplaza TODOS los elementos actuales con los nuevos
                    scope.launch {
                        parseando = true
                        val result = parser.parse(codeText)
                        parseando = false
                        if (result.errors.any { it.type != ErrorType.ADVERTENCIA }) {
                            errors    = result.errors.sortedWith(compareBy({ it.line }, { it.column }))
                            hasErrors = true
                        } else {
                            elements         = result.elements
                            errors           = result.errors
                            hasErrors        = false
                            modoContestacion = false
                            navController.navigate(Routes.FORM_VIEW)
                        }
                    }
                },*/
                /*onAdd = {
                    // Agrega los nuevos elementos AL FINAL de los existentes
                    scope.launch {
                        parseando = true
                        val result = parser.parse(codeText)
                        parseando = false
                        if (result.errors.any { it.type != ErrorType.ADVERTENCIA }) {
                            errors    = result.errors.sortedWith(compareBy({ it.line }, { it.column }))
                            hasErrors = true
                        } else {
                            elements         = elements + result.elements
                            errors           = result.errors
                            hasErrors        = false
                            modoContestacion = false
                            navController.navigate(Routes.FORM_VIEW)
                        }
                    }
                },*/
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