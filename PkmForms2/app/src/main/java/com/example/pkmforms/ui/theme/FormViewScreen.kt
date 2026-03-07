package com.example.pkmforms.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.ui.theme.components.TopBar
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun FormViewScreen(
    elements          : List<FormElement>,
    onBackToEditor    : () -> Unit,
    onNavigateOptions : () -> Unit
) {
    var correctAnswerMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopBar(title = "PKM_FORMS", onMenuClick = onNavigateOptions)

        correctAnswerMessage?.let { msg ->
            CorrectAnswerBanner(message = msg, onDismiss = { correctAnswerMessage = null })
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppColors.FormBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (elements.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "El formulario generado aparecera aqui",
                        color      = AppColors.FormTextSecondary,
                        fontSize   = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                elements.forEach { element ->
                    FormElementView(element = element)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        FormViewActionBar(
            onBackToEditor = onBackToEditor,
            onSend = {
                // TODO: verificar respuestas correctas y mostrar banner
            }
        )
    }
}

@Composable
fun FormElementView(element: FormElement) {
    when (element) {
        is FormElement.OpenQuestion    -> OpenQuestionView(element)
        is FormElement.DropQuestion    -> DropQuestionView(element)
        is FormElement.SelectQuestion  -> SelectQuestionView(element)
        is FormElement.MultipleQuestion -> MultipleQuestionView(element)
        is FormElement.TextElement     -> TextElementView(element)
    }
}

@Composable
fun OpenQuestionView(element: FormElement.OpenQuestion) {
    var respuesta by remember { mutableStateOf("") }

    QuestionCard(width = element.width, height = element.height)  {
        if (element.label.isNotBlank()) {
            QuestionLabel(text = element.label)
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value         = respuesta,
            onValueChange = { respuesta = it },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text("Escribe tu respuesta...", color = AppColors.FormTextSecondary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AppColors.Accent,
                unfocusedBorderColor = Color(0xFFCCCCCC)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropQuestionView(element: FormElement.DropQuestion) {
    var expanded       by remember { mutableStateOf(false) }
    var seleccionada   by remember { mutableStateOf("Selecciona una opcion") }

    QuestionCard(width = element.width, height = element.height) {
        if (element.label.isNotBlank()) {
            QuestionLabel(text = element.label)
            Spacer(modifier = Modifier.height(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value            = seleccionada,
                onValueChange    = {},
                readOnly         = true,
                modifier         = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AppColors.Accent,
                    unfocusedBorderColor = Color(0xFFCCCCCC)
                )
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                element.options.forEach { opcion ->
                    DropdownMenuItem(
                        text    = { Text(opcion) },
                        onClick = {
                            seleccionada = opcion
                            expanded     = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectQuestionView(element: FormElement.SelectQuestion) {
    var seleccionada by remember { mutableStateOf<String?>(null) }

    QuestionCard(width = element.width, height = element.height) {
        if (element.options.size > 5) {
            Text(
                text     = "Advertencia: esta pregunta tiene mas de 5 opciones",
                color    = Color(0xFFFF9800),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        element.options.forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = seleccionada == opcion,
                    onClick  = { seleccionada = opcion },
                    colors   = RadioButtonDefaults.colors(
                        selectedColor = AppColors.Accent
                    )
                )
                Text(
                    text     = opcion,
                    color    = AppColors.FormText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MultipleQuestionView(element: FormElement.MultipleQuestion) {
    val seleccionadas = remember { mutableStateListOf<String>() }

    QuestionCard(width = element.width, height = element.height) {
        element.options.forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked         = seleccionadas.contains(opcion),
                    onCheckedChange = { checked ->
                        if (checked) seleccionadas.add(opcion)
                        else seleccionadas.remove(opcion)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Accent
                    )
                )
                Text(
                    text     = opcion,
                    color    = AppColors.FormText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}


@Composable
fun TextElementView(element: FormElement.TextElement) {
    if (element.content.isNotBlank()) {
        Text(
            text     = element.content,
            color    = AppColors.FormText,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun QuestionCard(
    width   : Int?                           = null,
    height  : Int?                           = null,
    content : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .then(if (width  != null) Modifier.width(width.dp)    else Modifier.fillMaxWidth())
            .then(if (height != null) Modifier.height(height.dp)  else Modifier.wrapContentHeight())
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun QuestionLabel(text: String) {
    Text(
        text       = text,
        color      = AppColors.FormText,
        fontSize   = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CorrectAnswerBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Accent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = message, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}

@Composable
private fun FormViewActionBar(onBackToEditor: () -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.FormBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick  = onBackToEditor,
            modifier = Modifier.weight(1f),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.FormText),
            border   = BorderStroke(1.dp, Color(0xFF9E9E9E))
        ) {
            Text("Back to edit", fontSize = 13.sp)
        }
        Button(
            onClick  = onSend,
            modifier = Modifier.weight(1f),
            colors   = ButtonDefaults.buttonColors(containerColor = AppColors.SendButton)
        ) {
            Text("Send", fontSize = 13.sp, color = Color.White)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FormViewScreenPreview() {
    FormViewScreen(
        elements = listOf(
            FormElement.OpenQuestion(label = "Tu nombre:"),
            FormElement.DropQuestion(
                label   = "Elige un color:",
                options = listOf("Rojo", "Azul", "Verde"),
                correct = 1
            ),
            FormElement.SelectQuestion(
                options = listOf("Opcion A", "Opcion B", "Opcion C")
            ),
            FormElement.MultipleQuestion(
                options = listOf("Java", "Kotlin", "Python"),
                correct = listOf(0, 1)
            )
        ),
        onBackToEditor    = {},
        onNavigateOptions = {}
    )
}