@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pkmforms.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pkmforms.ui.theme.components.TopBar
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.analyzer.model.StyleData


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

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val androidColor = android.graphics.Color.HSVToColor(
        floatArrayOf(h, s, l)
    )

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
    val bw = style.borderWidthDp()
    if (bw > 0.dp) {
        val shape = RoundedCornerShape(4.dp)
        m = m.border(bw, style.borderColor2(), shape)
    }
    return m
}

/* =========    PANTALA PRINCIPAL ========== */

@Composable
fun FormViewScreen(
    elements: List<FormElement> = emptyList(),
    onBackToEditor: () -> Unit,
    onNavigateOptions: () -> Unit
) {
    var correctAnswerMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        TopBar(
            title = "PKM_FORMS",
            onMenuClick = onNavigateOptions
        )

        correctAnswerMessage?.let { msg ->
            CorrectAnswerBanner(message = msg, onDismiss = { correctAnswerMessage = null })
        }

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
                elements.forEach { element ->
                    FormElementRender(element = element)
                }
            }
        }

        FormViewActionBar(
            onBackToEditor = onBackToEditor,
            onSend = { /* TODO: validacion de respuestas */ }
        )
    }
}

// inheritedWidth / inheritedHeight: dimensiones del padre para aplicar herencia
// cuando el hijo no define las suyas propias

@Composable
fun FormElementRender(
    element: FormElement,
    inheritedWidth:  Int? = null,
    inheritedHeight: Int? = null,
    inheritedStyle:  StyleData? = null
) {
    when (element) {
        is FormElement.Section          -> SectionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.OpenQuestion     -> OpenQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.DropQuestion     -> DropQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.SelectQuestion   -> SelectQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.MultipleQuestion -> MultipleQuestionRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.TextElement      -> TextElementRender(element, inheritedWidth, inheritedHeight, inheritedStyle)
        is FormElement.Table            -> TableRender(element, inheritedWidth, inheritedHeight)
    }
}

/* ========= seccion ========== */

@Composable
fun SectionRender(
    section: FormElement.Section,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    // Resolver dimensiones: propias heredadas fillMax
    val resolvedWidth  = section.width  ?: inheritedWidth
    val resolvedHeight = section.height ?: inheritedHeight

    // Resolver estilo: el propio sobreescribe al heredado campo por campo
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

    // Si tiene pointX/pointY se posiciona de forma absoluta dentro de su padre
    val offsetModifier = if (section.pointX != null && section.pointY != null) {
        Modifier.absoluteOffset(section.pointX.dp, section.pointY.dp)
    } else Modifier

    val sizeModifier = offsetModifier
        .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
        .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier.wrapContentHeight())
        .applyStyle(style)

    // Calcular dimensiones para pasar a hijos
    // En HORIZONTAL cada hijo recibe width = resolvedWidth / cantidad de hijos
    // En VERTICAL cada hijo recibe el mismo width que el padre
    val childCount = section.elements.size.coerceAtLeast(1)
    val childInheritedWidth = when {
        section.orientation == "HORIZONTAL" && resolvedWidth != null -> resolvedWidth / childCount
        else -> resolvedWidth
    }
    val childInheritedHeight = when {
        section.orientation == "VERTICAL" && resolvedHeight != null -> resolvedHeight / childCount
        else -> resolvedHeight
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

/* ========= open questions ========== */

@Composable
fun OpenQuestionRender(
    q: FormElement.OpenQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    var answer by remember { mutableStateOf("") }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
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
            onValueChange = { answer = it },
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


/* ========= drop questions ========== */
@Composable
fun DropQuestionRender(
    q: FormElement.DropQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
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
                        onClick = { selected = option; expanded = false }
                    )
                }
            }
        }
    }
}

/* ========= select questions ========== */

@Composable
fun SelectQuestionRender(
    q: FormElement.SelectQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        q.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(
                    selected = selected == index,
                    onClick = { selected = index },
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

/* ========= multiple questions ========== */

@Composable
fun MultipleQuestionRender(
    q: FormElement.MultipleQuestion,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null,
    inheritedStyle:  StyleData? = null
) {
    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val resolvedWidth  = q.width  ?: inheritedWidth
    val resolvedHeight = q.height ?: inheritedHeight
    val style = mergeStyle(q.style, inheritedStyle)

    Column(
        modifier = Modifier
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
            .applyStyle(style)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        q.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = checked[index] == true,
                    onCheckedChange = { checked[index] = it },
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


/* ========= table ========== */

@Composable
fun TableRender(
    table: FormElement.Table,
    inheritedWidth:  Int?       = null,
    inheritedHeight: Int?       = null
) {
    val resolvedWidth  = table.width  ?: inheritedWidth
    val resolvedHeight = table.height ?: inheritedHeight
    val style = table.style
    val offsetModifier = if (table.pointX != null && table.pointY != null) {
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
                                element         = cell,
                                inheritedStyle  = style
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ========= text element ========== */

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
            .then(if (resolvedWidth  != null) Modifier.width(resolvedWidth.dp)   else Modifier.fillMaxWidth())
            .then(if (resolvedHeight != null) Modifier.height(resolvedHeight.dp) else Modifier)
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

/* ========= UI auxiliar ========== */
@Composable
private fun CorrectAnswerBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Accent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = message, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
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
            onClick = onBackToEditor,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.FormText),
            border = BorderStroke(1.dp, Color(0xFF9E9E9E))
        ) {
            Text("Back to edit", fontSize = 13.sp)
        }
        Button(
            onClick = onSend,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.SendButton)
        ) {
            Text("Send", fontSize = 13.sp, color = Color.White)
        }
    }
}