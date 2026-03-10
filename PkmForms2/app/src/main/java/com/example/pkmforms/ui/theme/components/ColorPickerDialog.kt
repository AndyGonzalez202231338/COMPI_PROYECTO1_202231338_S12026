package com.example.pkmforms.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

// Utilidades de conversion de color
private fun hsvToColor(hue: Float, sat: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, sat, value)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun colorToString(color: Color, format: ColorFormat): String {
    val argb = color.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8)  and 0xFF
    val b = argb           and 0xFF
    return when (format) {
        ColorFormat.HEX -> "#%02X%02X%02X".format(r, g, b)
        ColorFormat.RGB -> "($r,$g,$b)"
        ColorFormat.HSL -> {
            val hsl = FloatArray(3)
            ColorUtils.RGBToHSL(r, g, b, hsl)
            "<${hsl[0].roundToInt()},${(hsl[1] * 100).roundToInt()},${(hsl[2] * 100).roundToInt()}>"
        }
    }
}

enum class ColorFormat { HEX, RGB, HSL }

// Dialog principal
@Composable
fun ColorPickerDialog(
    onColorSelected : (String) -> Unit,
    onDismiss       : () -> Unit
) {
    var hue    by remember { mutableFloatStateOf(200f) }
    var sat    by remember { mutableFloatStateOf(0.8f) }
    var `val`  by remember { mutableFloatStateOf(0.9f) }
    var format by remember { mutableStateOf(ColorFormat.HEX) }

    val selectedColor = hsvToColor(hue, sat, `val`)
    val colorString   = colorToString(selectedColor, format)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape         = RoundedCornerShape(18.dp),
            color         = Color(0xFF1A1A2E),
            tonalElevation = 12.dp,
            modifier      = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text       = "Selector de Color",
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                // Panel sat/val
                SatValPanel(
                    hue      = hue,
                    sat      = sat,
                    value    = `val`,
                    onChange = { s, v -> sat = s; `val` = v }
                )

                Spacer(Modifier.height(10.dp))

                // Barra hue
                HueBar(hue = hue, onChange = { hue = it })

                Spacer(Modifier.height(14.dp))

                // Preview + texto del color
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedColor)
                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(8.dp))
                    )
                    Text(
                        text       = colorString,
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Tabs de formato
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorFormat.entries.forEach { f ->
                        val active = format == f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF6D28D9) else Color(0xFF252538))
                                .clickable { format = f }
                                .padding(vertical = 7.dp)
                        ) {
                            Text(
                                text       = f.name,
                                color      = if (active) Color.White else Color(0xFF8888AA),
                                fontSize   = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Colores predefinidos del lenguaje PKM
                Text(
                    text       = "Predefinidos",
                    color      = Color(0xFF8888AA),
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                val predefinidos = listOf(
                    "RED"    to Color(0xFFE53935),
                    "BLUE"   to Color(0xFF1E88E5),
                    "GREEN"  to Color(0xFF43A047),
                    "PURPLE" to Color(0xFF8E24AA),
                    "SKY"    to Color(0xFF29B6F6),
                    "YELLOW" to Color(0xFFFDD835),
                    "BLACK"  to Color(0xFF212121),
                    "WHITE"  to Color(0xFFEEEEEE)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    predefinidos.forEach { (nombre, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.White.copy(0.15f), CircleShape)
                                .clickable { onColorSelected(nombre) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botones de accion
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8888AA)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF3D3D55))
                    ) {
                        Text("Cancelar", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick  = { onColorSelected(colorString) },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                    ) {
                        Text("Insertar", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// Panel sat/val — cuadrado de seleccion
@Composable
private fun SatValPanel(
    hue      : Float,
    sat      : Float,
    value    : Float,
    onChange : (sat: Float, value: Float) -> Unit
) {
    val hueColor = hsvToColor(hue, 1f, 1f)
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(10.dp))
            .onSizeChanged { panelSize = it }
            .drawBehind {
                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            }
            .pointerInput(panelSize) {
                detectDragGestures { change, _ ->
                    if (panelSize.width > 0 && panelSize.height > 0) {
                        val s = (change.position.x / panelSize.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / panelSize.height).coerceIn(0f, 1f)
                        onChange(s, v)
                    }
                }
            }
            .pointerInput(panelSize) {
                detectTapGestures { offset ->
                    if (panelSize.width > 0 && panelSize.height > 0) {
                        val s = (offset.x / panelSize.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / panelSize.height).coerceIn(0f, 1f)
                        onChange(s, v)
                    }
                }
            }
    ) {
        if (panelSize.width > 0) {
            val density = LocalDensity.current
            val xDp = with(density) { (sat * panelSize.width - 10).toDp() }
            val yDp = with(density) { ((1f - value) * panelSize.height - 10).toDp() }

            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(hsvToColor(hue, sat, value))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

// Barra horizontal de hue
@Composable
private fun HueBar(
    hue      : Float,
    onChange : (Float) -> Unit
) {
    val hueColors = listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
        Color(0xFFFF0000)
    )
    var barSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .onSizeChanged { barSize = it }
            .drawBehind {
                drawRect(brush = Brush.horizontalGradient(hueColors))
            }
            .pointerInput(barSize) {
                detectDragGestures { change, _ ->
                    if (barSize.width > 0)
                        onChange((change.position.x / barSize.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(barSize) {
                detectTapGestures { offset ->
                    if (barSize.width > 0)
                        onChange((offset.x / barSize.width).coerceIn(0f, 1f) * 360f)
                }
            }
    ) {
        if (barSize.width > 0) {
            val density = LocalDensity.current
            val xDp = with(density) { (hue / 360f * barSize.width - 11).toDp() }
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = 1.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(hsvToColor(hue, 1f, 1f))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}