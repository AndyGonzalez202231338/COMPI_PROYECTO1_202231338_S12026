@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pkmforms.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.components.TopBar
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.analyzer.model.StyleData

// ===== Estado compartido de respuestas del usuario =====

val LocalRespuestas = compositionLocalOf<SnapshotStateMap<Int, Any>> {
    @Suppress("UNCHECKED_CAST")
    mutableStateMapOf<Int, Any>() as SnapshotStateMap<Int, Any>
}

val LocalContadorPreguntas = compositionLocalOf<MutableState<Int>> {
    mutableStateOf(0)
}

// ===== Helpers de estilo =====
private fun parseColor(raw: String, fallback: Color = Color.Unspecified): Color {
    val s = raw.trim()
    if (s.isEmpty()) return fallback
    val su = s.uppercase()
    return try {
        when {
            // HEX: #RRGGBB o #RRGGBBAA
            su.startsWith("#") ->
                Color(android.graphics.Color.parseColor(su))

            // RGB: (r,g,b) — formato del lexer con parentesis
            su.startsWith("(") && su.endsWith(")") -> {
                val nums = su.removePrefix("(").removeSuffix(")").split(",")
                Color(
                    red   = nums[0].trim().toInt().coerceIn(0, 255),
                    green = nums[1].trim().toInt().coerceIn(0, 255),
                    blue  = nums[2].trim().toInt().coerceIn(0, 255)
                )
            }

            // HSL: <h,s,l> — formato del lexer con angulos
            su.startsWith("<") && su.endsWith(">") -> {
                val nums = su.removePrefix("<").removeSuffix(">").split(",")
                val h = nums[0].trim().toFloat()          // 0-360
                val sl = nums[1].trim().toFloat() / 100f  // 0-100 -> 0-1
                val l = nums[2].trim().toFloat() / 100f   // 0-100 -> 0-1
                hslToColor(h, sl, l)
            }

            // Colores predefinidos por nombre
            else -> when (su) {
                "RED"    -> Color(0xFFE53935)
                "BLUE"   -> Color(0xFF1565C0)
                "GREEN"  -> Color(0xFF2E7D32)
                "PURPLE" -> Color(0xFF6A1B9A)
                "SKY"    -> Color(0xFF0288D1)
                "YELLOW" -> Color(0xFFF9A825)
                "BLACK"  -> Color(0xFF000000)
                "WHITE"  -> Color(0xFFFFFFFF)
                "ORANGE" -> Color(0xFFE65100)
                "PINK"   -> Color(0xFFAD1457)
                "GRAY"   -> Color(0xFF616161)
                else     -> fallback
            }
        }
    } catch (e: Exception) { fallback }
}

// Convierte HSL (h: 0-360, s: 0-1, l: 0-1) a Color de Compose
private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val androidColor = android.graphics.Color.HSVToColor(
        floatArrayOf(h, s, l)
    )
    // Android usa HSV no HSL, convertir L a V correctamente
    val v = l + s * minOf(l, 1f - l)
    val sv = if (v == 0f) 0f else 2f * (1f - l / v)
    val hsv = floatArrayOf(h, sv, v)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun StyleData.textColor(fallback: Color = AppColors.FormText): Color =
    parseColor(color, fallback)

private fun StyleData.bgColor(): Color =
    parseColor(backgroundColor, Color.Unspecified)

private fun StyleData.borderColor2(): Color =
    parseColor(borderColor, Color(0xFF9E9E9E))

private fun StyleData.fontFamilyCompose(): FontFamily = when (fontFamily.uppercase()) {
    "MONO"       -> FontFamily.Monospace
    "SANS_SERIF" -> FontFamily.SansSerif
    "CURSIVE"    -> FontFamily.Cursive
    else         -> FontFamily.Default
}

private fun StyleData.textSizeSp() = if (textSize > 0f) textSize.sp else 14.sp

private fun StyleData.borderWidthDp(): Dp = if (borderSize > 0f) borderSize.dp else 0.dp

private fun Modifier.applyStyle(style: StyleData): Modifier {
    var m = this
    val bg = style.bgColor()
    if (bg != Color.Unspecified) m = m.background(bg, RoundedCornerShape(6.dp))
    if (style.borderSize > 0f) {
        val shape = RoundedCornerShape(4.dp)
        m = m.border(style.borderSize.dp, style.borderColor2(), shape)
    }
    return m
}

// ===== Pantalla principal =====

@Composable
fun FormViewScreen(
    elements: List<FormElement> = emptyList(),
    onBackToEditor: () -> Unit,
    onNavigateOptions: () -> Unit,
    onSent: () -> Unit = {},
    modoContestacion: Boolean = false
) {
    var mostrarDialogoEnvio by remember { mutableStateOf(false) }

    // Estado compartido de respuestas se resetea cuando cambian los elements
    val respuestasUsuario = remember(elements) { mutableStateMapOf<Int, Any>() }
    val contadorPreguntas = remember(elements) { mutableStateOf(0) }

    CompositionLocalProvider(
        LocalRespuestas          provides respuestasUsuario,
        LocalContadorPreguntas   provides contadorPreguntas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            TopBar(
                title = "PKM_FORMS",
                onMenuClick = onNavigateOptions
            )

            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AppColors.FormBackground)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(hScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (elements.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "El formulario generado aparecera aqui",
                            color = AppColors.FormTextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    // Resetear contador antes de renderizar
                    contadorPreguntas.value = 0
                    elements.forEach { element ->
                        FormElementRender(element = element, useAbsoluteOffset = false)
                    }
                }
            }

            FormViewActionBar(
                onBackToEditor   = onBackToEditor,
                onSend           = { mostrarDialogoEnvio = true },
                modoContestacion = modoContestacion
            )
        }

        // Dialog de envio
        if (mostrarDialogoEnvio) {
            val resultado = calcularResultado(elements, respuestasUsuario)
            SendDialog(
                resultado = resultado,
                onDismiss = {
                    mostrarDialogoEnvio = false
                    onSent()
                }
            )
        }
    }
}

// ===== Resultado del formulario =====

data class ResultadoFormulario(
    val totalPreguntas:    Int,
    val preguntasConNota:  Int,   // preguntas que tienen respuesta correcta definida
    val correctas:         Int,   // preguntas que el usuario respondió bien
    val detalles:          List<DetalleRespuesta>
)

data class DetalleRespuesta(
    val numero:          Int,
    val label:           String,
    val respuestaUsuario: String,
    val respuestaCorrecta: String,
    val esCorrecta:      Boolean?,  // null = pregunta abierta, sin calificacion
    val tieneNota:       Boolean
)

private fun calcularResultado(
    elementos: List<FormElement>,
    respuestas: Map<Int, Any>
): ResultadoFormulario {
    val detalles   = mutableListOf<DetalleRespuesta>()
    var contador   = 0
    var conNota    = 0
    var correctas  = 0

    fun recorrer(lista: List<FormElement>) {
        for (el in lista) {
            when (el) {
                is FormElement.OpenQuestion -> {
                    val respuesta = respuestas[contador] as? String ?: ""
                    detalles.add(DetalleRespuesta(
                        numero           = contador + 1,
                        label            = el.label.ifBlank { "Pregunta abierta" },
                        respuestaUsuario  = respuesta.ifBlank { "(sin respuesta)" },
                        respuestaCorrecta = "",
                        esCorrecta        = null,
                        tieneNota         = false
                    ))
                    contador++
                }
                is FormElement.DropQuestion -> {
                    val seleccion = respuestas[contador] as? String ?: ""
                    val tieneCorrecta = el.correct >= 0 && el.correct < el.options.size
                    val correcta = if (tieneCorrecta) el.options[el.correct] else ""
                    val esCorrecta = if (tieneCorrecta) seleccion == correcta else null
                    if (tieneCorrecta) {
                        conNota++
                        if (esCorrecta == true) correctas++
                    }
                    detalles.add(DetalleRespuesta(
                        numero            = contador + 1,
                        label             = el.label.ifBlank { "Desplegable" },
                        respuestaUsuario  = seleccion.ifBlank { "(sin respuesta)" },
                        respuestaCorrecta = correcta,
                        esCorrecta        = esCorrecta,
                        tieneNota         = tieneCorrecta
                    ))
                    contador++
                }
                is FormElement.SelectQuestion -> {
                    val seleccion = respuestas[contador] as? Int
                    val tieneCorrecta = el.correct >= 0 && el.correct < el.options.size
                    val correcta = if (tieneCorrecta) el.options[el.correct] else ""
                    val respUsuario = if (seleccion != null && seleccion < el.options.size)
                        el.options[seleccion] else "(sin respuesta)"
                    val esCorrecta = if (tieneCorrecta) seleccion == el.correct else null
                    if (tieneCorrecta) {
                        conNota++
                        if (esCorrecta == true) correctas++
                    }
                    detalles.add(DetalleRespuesta(
                        numero            = contador + 1,
                        label             = el.label.ifBlank { "Selección" },
                        respuestaUsuario  = respUsuario,
                        respuestaCorrecta = correcta,
                        esCorrecta        = esCorrecta,
                        tieneNota         = tieneCorrecta
                    ))
                    contador++
                }
                is FormElement.MultipleQuestion -> {
                    @Suppress("UNCHECKED_CAST")
                    val seleccion = (respuestas[contador] as? List<Int>) ?: emptyList()
                    val tieneCorrecta = el.correct.isNotEmpty()
                    val correctaTexto = el.correct
                        .filter { it >= 0 && it < el.options.size }
                        .joinToString(", ") { el.options[it] }
                    val usuarioTexto = seleccion
                        .filter { it >= 0 && it < el.options.size }
                        .joinToString(", ") { el.options[it] }
                        .ifBlank { "(sin respuesta)" }
                    val esCorrecta = if (tieneCorrecta)
                        seleccion.toSet() == el.correct.toSet() else null
                    if (tieneCorrecta) {
                        conNota++
                        if (esCorrecta == true) correctas++
                    }
                    detalles.add(DetalleRespuesta(
                        numero            = contador + 1,
                        label             = el.label.ifBlank { "Múltiple" },
                        respuestaUsuario  = usuarioTexto,
                        respuestaCorrecta = correctaTexto,
                        esCorrecta        = esCorrecta,
                        tieneNota         = tieneCorrecta
                    ))
                    contador++
                }
                is FormElement.Section     -> recorrer(el.elements)
                is FormElement.Table       -> el.rows.forEach { fila ->
                    fila.filterNotNull().let { recorrer(it) }
                }
                is FormElement.TextElement -> { /* sin respuesta */ }
            }
        }
    }

    recorrer(elementos)
    return ResultadoFormulario(
        totalPreguntas   = contador,
        preguntasConNota = conNota,
        correctas        = correctas,
        detalles         = detalles
    )
}

// ===== Dialog de envio con puntuacion =====

@Composable
private fun SendDialog(
    resultado: ResultadoFormulario,
    onDismiss: () -> Unit
) {
    val tieneNota = resultado.preguntasConNota > 0

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Surface)
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Titulo
            Text(
                text       = if (tieneNota) "Resultado del formulario" else "Formulario enviado",
                color      = AppColors.Text,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (tieneNota) {
                // Puntaje
                val puntaje = if (resultado.preguntasConNota > 0)
                    (resultado.correctas * 100) / resultado.preguntasConNota else 0

                val (mensajeNota, colorNota) = when {
                    puntaje == 100 -> "¡Perfecto! " to Color(0xFF4CAF50)
                    puntaje >= 75  -> "¡Muy bien! " to Color(0xFF8BC34A)
                    puntaje >= 50  -> "Aprobado "   to Color(0xFFFFC107)
                    else           -> "Sigue intentando " to Color(0xFFF44336)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Accent.copy(alpha = 0.15f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = "$puntaje%",
                            color      = colorNota,
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text     = mensajeNota,
                            color    = colorNota,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // Barra de progreso
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(AppColors.Background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(puntaje / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colorNota)
                            )
                        }
                        Text(
                            text     = "${resultado.correctas} de ${resultado.preguntasConNota} preguntas correctas",
                            color    = AppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Detalle por pregunta
                resultado.detalles.forEach { detalle ->
                    if (detalle.tieneNota || detalle.esCorrecta == null) {
                        val colorFondo = when (detalle.esCorrecta) {
                            true  -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                            false -> Color(0xFFB71C1C).copy(alpha = 0.15f)
                            null  -> AppColors.Background
                        }
                        val icono = when (detalle.esCorrecta) {
                            true  -> "✓"
                            false -> "✗"
                            null  -> "•"
                        }
                        val colorIcono = when (detalle.esCorrecta) {
                            true  -> Color(0xFF4CAF50)
                            false -> Color(0xFFF44336)
                            null  -> AppColors.TextSecondary
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorFondo)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icono, color = colorIcono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "P${detalle.numero}: ${detalle.label}",
                                    color = AppColors.Text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Tu respuesta: ${detalle.respuestaUsuario}",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                            if (detalle.esCorrecta == false) {
                                Text(
                                    text = "Correcta: ${detalle.respuestaCorrecta}",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text     = "Tu respuesta ha sido enviada correctamente.",
                    color    = AppColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.End),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text("Cerrar", color = AppColors.Text)
            }
        }
    }
}

// ===== Dispatcher principal =====

@Composable
fun FormElementRender(
    element: FormElement,
    inheritedWidth:      Int?     = null,
    inheritedHeight:     Int?     = null,
    inheritedStyle:      StyleData? = null,
    useAbsoluteOffset:   Boolean  = false
) {
    when (element) {
        is FormElement.Section          -> SectionRender(element, inheritedWidth, inheritedHeight, inheritedStyle, useAbsoluteOffset)
        is FormElement.OpenQuestion     -> OpenQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.DropQuestion     -> DropQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.SelectQuestion   -> SelectQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.MultipleQuestion -> MultipleQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.TextElement      -> TextElementRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.Table            -> TableRender(element, inheritedWidth, inheritedHeight, useAbsoluteOffset)
    }
}

// ==== SECTION =====

@Composable
fun SectionRender(
    section: FormElement.Section,
    inheritedWidth:     Int?       = null,
    inheritedHeight:    Int?       = null,
    inheritedStyle:     StyleData? = null,
    useAbsoluteOffset:  Boolean    = false
) {
    val resolvedWidth  = section.width  ?: inheritedWidth
    val resolvedHeight = section.height ?: inheritedHeight

    val parentStyle = inheritedStyle ?: StyleData()
    val style = StyleData(
        color           = section.style.color.ifEmpty           { parentStyle.color },
        backgroundColor = section.style.backgroundColor.ifEmpty { parentStyle.backgroundColor },
        fontFamily      = section.style.fontFamily.ifEmpty      { parentStyle.fontFamily },
        textSize        = if (section.style.textSize > 0f) section.style.textSize else parentStyle.textSize,
        borderSize      = if (section.style.borderSize > 0f) section.style.borderSize else parentStyle.borderSize,
        borderType      = section.style.borderType.ifEmpty      { parentStyle.borderType },
        borderColor     = section.style.borderColor.ifEmpty     { parentStyle.borderColor }
    )

    // En VERTICAL la seccion crece con su contenido (wrapContentHeight)
    // En HORIZONTAL respeta el height definido
    val offsetModifier = if (useAbsoluteOffset && section.pointX != null && section.pointY != null)
        Modifier.absoluteOffset(section.pointX.dp, section.pointY.dp)
    else
        Modifier

    val sizeModifier = offsetModifier
        .then(if (resolvedWidth != null) Modifier.width(resolvedWidth.dp) else Modifier.fillMaxWidth())
        .then(
            when {
                section.orientation == "HORIZONTAL" && resolvedHeight != null ->
                    Modifier.height(resolvedHeight.dp)
                resolvedHeight != null ->
                    Modifier.heightIn(min = resolvedHeight.dp)
                else ->
                    Modifier.wrapContentHeight()
            }
        )
        .applyStyle(style)

    val childCount = section.elements.size.coerceAtLeast(1)
    val childInheritedWidth = when {
        section.orientation == "HORIZONTAL" && resolvedWidth != null -> resolvedWidth / childCount
        else -> resolvedWidth
    }
    // En VERTICAL los hijos NO heredan height — cada uno toma el espacio que necesita
    // En HORIZONTAL los hijos heredan el height de la seccion padre
    val childInheritedHeight = when {
        section.orientation == "HORIZONTAL" -> resolvedHeight
        else -> null
    }

    if (section.orientation == "HORIZONTAL") {
        Row(
            modifier = sizeModifier,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Top
        ) {
            section.elements.forEach { child ->
                Box(modifier = Modifier.weight(1f)) {
                    FormElementRender(
                        element         = child,
                        inheritedWidth  = childInheritedWidth,
                        inheritedHeight = resolvedHeight,
                        inheritedStyle  = style
                    )
                }
            }
        }
    } else {
        Column(
            modifier = sizeModifier,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            section.elements.forEach { child ->
                FormElementRender(
                    element         = child,
                    inheritedWidth  = resolvedWidth,
                    inheritedHeight = childInheritedHeight,
                    inheritedStyle  = style
                )
            }
        }
    }
}


// Combina el estilo propio con el heredado: el propio tiene prioridad
fun mergeStyle(own: StyleData, parent: StyleData?): StyleData {
    if (parent == null) return own
    return StyleData(
        color           = own.color.ifEmpty           { parent.color },
        backgroundColor = own.backgroundColor.ifEmpty { parent.backgroundColor },
        fontFamily      = own.fontFamily.ifEmpty      { parent.fontFamily },
        textSize        = if (own.textSize   > 0f) own.textSize   else parent.textSize,
        borderSize      = if (own.borderSize > 0f) own.borderSize else parent.borderSize,
        borderType      = own.borderType.ifEmpty      { parent.borderType },
        borderColor     = own.borderColor.ifEmpty     { parent.borderColor }
    )
}

// ===== OPEN_QUESTION =====

@Composable
fun OpenQuestionRender(
    q: FormElement.OpenQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    val respuestas = LocalRespuestas.current
    val contador   = LocalContadorPreguntas.current
    val idx = remember { contador.value.also { contador.value++ } }

    var answer by remember { mutableStateOf("") }

    // Sincronizar respuesta al estado compartido
    LaunchedEffect(answer) { respuestas[idx] = answer }

    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)        else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.heightIn(min = resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (q.label.isNotEmpty()) {
            Text(
                text = q.label,
                color = style.textColor(),
                fontSize = style.textSizeSp(),
                fontFamily = style.fontFamilyCompose()
            )
        }
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                respuestas[idx] = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Escribe tu respuesta...", fontSize = 13.sp, color = AppColors.FormTextSecondary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Accent,
                unfocusedBorderColor = Color(0xFF9E9E9E),
                focusedTextColor = AppColors.FormText,
                unfocusedTextColor = AppColors.FormText
            )
        )
    }
}

// ===== DROP_QUESTION =====

@Composable
fun DropQuestionRender(
    q: FormElement.DropQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    val respuestas = LocalRespuestas.current
    val contador   = LocalContadorPreguntas.current
    val idx = remember { contador.value.also { contador.value++ } }

    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)           else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.heightIn(min = resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (q.label.isNotEmpty()) {
            Text(
                text = q.label,
                color = style.textColor(),
                fontSize = style.textSizeSp(),
                fontFamily = style.fontFamilyCompose()
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected ?: "Selecciona una opcion",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Accent,
                    unfocusedBorderColor = Color(0xFF9E9E9E),
                    focusedTextColor = AppColors.FormText,
                    unfocusedTextColor = AppColors.FormTextSecondary
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                q.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = AppColors.FormText) },
                        onClick = {
                            selected = option
                            respuestas[idx] = option
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ===== SELECT_QUESTION =====

@Composable
fun SelectQuestionRender(
    q: FormElement.SelectQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val respuestas = LocalRespuestas.current
    val contador   = LocalContadorPreguntas.current
    val idx = remember { contador.value.also { contador.value++ } }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)           else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.heightIn(min = resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (q.label.isNotBlank()) {
            Text(
                text       = q.label,
                color      = style.textColor(),
                fontSize   = style.textSizeSp(),
                fontFamily = style.fontFamilyCompose(),
                fontWeight = FontWeight.Medium
            )
        }
        q.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        respuestas[idx] = index
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.Accent)
                )
                Text(
                    text = option,
                    color = style.textColor(),
                    fontSize = style.textSizeSp(),
                    fontFamily = style.fontFamilyCompose(),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

// ===== MULTIPLE_QUESTION =====

@Composable
fun MultipleQuestionRender(
    q: FormElement.MultipleQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    val respuestas = LocalRespuestas.current
    val contador   = LocalContadorPreguntas.current
    val idx = remember { contador.value.also { contador.value++ } }

    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)           else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.heightIn(min = resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (q.label.isNotBlank()) {
            Text(
                text       = q.label,
                color      = style.textColor(),
                fontSize   = style.textSizeSp(),
                fontFamily = style.fontFamilyCompose(),
                fontWeight = FontWeight.Medium
            )
        }
        q.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = checked[index] == true,
                    onCheckedChange = { isChecked ->
                        checked[index] = isChecked
                        // Guardar lista de indices seleccionados
                        val seleccionados = checked.entries
                            .filter { it.value }
                            .map { it.key }
                            .sorted()
                        respuestas[idx] = seleccionados
                    },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Accent)
                )
                Text(
                    text = option,
                    color = style.textColor(),
                    fontSize = style.textSizeSp(),
                    fontFamily = style.fontFamilyCompose(),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}


// ===== TABLE =====

@Composable
fun TableRender(
    table: FormElement.Table,
    inheritedWidth:    Int?    = null,
    inheritedHeight:   Int?    = null,
    useAbsoluteOffset: Boolean = false
) {
    val resolvedWidth  = table.width  ?: inheritedWidth
    val resolvedHeight = table.height ?: inheritedHeight
    val style = table.style
    val offsetModifier = if (useAbsoluteOffset && table.pointX != null && table.pointY != null) {
        Modifier.absoluteOffset(table.pointX.dp, table.pointY.dp)
    } else Modifier
    Column(
        modifier = offsetModifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        table.rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        if (cell != null) {
                            FormElementRender(
                                element            = cell,
                                inheritedStyle     = style,
                                useAbsoluteOffset  = true
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== TEXT_ELEMENT =====

@Composable
fun TextElementRender(
    t: FormElement.TextElement,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    val resolvedWidth  = t.width  ?: inheritedWidth
    val resolvedHeight = t.height ?: inheritedHeight
    val style = mergeStyle(t.style, inheritedStyle)
    Box(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)           else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.heightIn(min = resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(8.dp)
    ) {
        Text(
            text = t.content,
            color = style.textColor(),
            fontSize = style.textSizeSp(),
            fontFamily = style.fontFamilyCompose()
        )
    }
}

// ===== UI auxiliar =====

@Composable
private fun FormViewActionBar(
    onBackToEditor: () -> Unit,
    onSend: () -> Unit,
    modoContestacion: Boolean = false
) {
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
        if (modoContestacion) {
            Button(
                onClick  = onSend,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = AppColors.SendButton)
            ) {
                Text("Send", fontSize = 13.sp, color = Color.White)
            }
        }
    }
}