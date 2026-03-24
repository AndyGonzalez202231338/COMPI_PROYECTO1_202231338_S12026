package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.AppColors

/* ── Plantillas de codigo disponibles ── */
private data class Plantilla(val nombre: String, val codigo: String)

private val PLANTILLAS = listOf(
    Plantilla("SECTION", """
SECTION [
    width: 400,
    height: 300,
    pointX: 0,
    pointY: 0,
    orientation: VERTICAL,
    elements: {

    },
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("TEXT", """
TEXT [
    content: "Tu texto aqui",
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("OPEN_QUESTION", """
OPEN_QUESTION [
    width: 380,
    height: 80,
    label: "Escribe tu respuesta",
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("DROP_QUESTION", """
DROP_QUESTION [
    width: 380,
    height: 60,
    label: "Selecciona una opcion",
    options: {"Opcion 1", "Opcion 2", "Opcion 3"},
    correct: 0,
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("SELECT_QUESTION", """
SELECT_QUESTION [
    width: 380,
    height: 160,
    label: "Selecciona una respuesta",
    options: {"Opcion 1", "Opcion 2", "Opcion 3"},
    correct: 0,
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("MULTIPLE_QUESTION", """
MULTIPLE_QUESTION [
    width: 380,
    height: 200,
    label: "Selecciona todas las correctas",
    options: {"Opcion 1", "Opcion 2", "Opcion 3", "Opcion 4"},
    correct: {0, 1},
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("TABLE", """
TABLE [
    width: 400,
    height: 200,
    pointX: 0,
    pointY: 0,
    elements: {
        [
            { TEXT [ content: "Celda 1" ] },
            { TEXT [ content: "Celda 2" ] }
        ],
        [
            { TEXT [ content: "Celda 3" ] },
            { TEXT [ content: "Celda 4" ] }
        ]
    },
    styles [
        "color": BLACK,
        "background color": WHITE,
        "font family": SANS_SERIF,
        "text size": 14
    ]
]""".trimIndent()),

    Plantilla("IF", """
IF (condicion > 0) {

}""".trimIndent()),

    Plantilla("WHILE", """
WHILE (condicion > 0) {

}""".trimIndent()),

    Plantilla("FOR clasico", """
FOR (i = 0 ; i <= 10 ; i = i + 1) {

}""".trimIndent()),

    Plantilla("FOR rango", """
FOR (i in 1 .. 5) {

}""".trimIndent()),

    Plantilla("number", "number miVar = 0"),
    Plantilla("string", "string miVar = \"\""),
    Plantilla("special", """
special miPregunta = OPEN_QUESTION [
    width: 380,
    height: 80,
    label: "Pregunta dinamica"
]""".trimIndent()),
)

@Composable
fun EditorActionBar(
    onReplace      : () -> Unit = {},
    onAdd          : () -> Unit = {},
    onFinish       : () -> Unit,
    onColorInsert  : (String) -> Unit,
    onTemplateInsert: (String) -> Unit = {}
) {
    var showColorPicker    by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { colorString ->
                onColorInsert(colorString)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showTemplatePicker) {
        TemplatePicker(
            onSelected = { codigo ->
                // Inserta comentario + codigo de la plantilla
                onTemplateInsert("\n$ codigo insertado\n$codigo\n")
                showTemplatePicker = false
            },
            onDismiss = { showTemplatePicker = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActionChip(
            label    = "Template",
            color    = Color(0xFF0F766E),
            onClick  = { showTemplatePicker = true },
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label    = "Color",
            color    = Color(0xFF6D28D9),
            onClick  = { showColorPicker = true },
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            label    = "Finish",
            color    = AppColors.Accent,
            onClick  = onFinish,
            modifier = Modifier.weight(1f)
        )
    }
}

/* ── Dialog selector de plantillas ── */
@Composable
private fun TemplatePicker(
    onSelected : (String) -> Unit,
    onDismiss  : () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = "Insertar plantilla",
                fontSize   = 16.sp,
                fontFamily = FontFamily.Monospace,
                color      = AppColors.Text
            )
            HorizontalDivider(color = AppColors.CodeBorder)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PLANTILLAS.forEach { plantilla ->
                    Button(
                        onClick  = { onSelected(plantilla.codigo) },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Surface
                        ),
                        shape    = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text       = plantilla.nombre,
                            fontSize   = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color      = AppColors.Accent
                        )
                    }
                }
            }
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancelar", color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ActionChip(
    label    : String,
    color    : Color,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(36.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = color),
        shape          = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            color      = Color.White
        )
    }
}