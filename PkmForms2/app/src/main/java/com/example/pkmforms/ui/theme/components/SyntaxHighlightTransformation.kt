package com.example.pkmforms.ui.theme.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping

private val ColorKeyword  = Color(0xFFBB86FC)
private val ColorString   = Color(0xFFFF9800)
private val ColorNumber   = Color(0xFF87CEEB)
private val ColorOperator = Color(0xFF4CAF50)
private val ColorBracket  = Color(0xFF4A90D9)
private val ColorEmoji    = Color(0xFFFFEB3B)
private val ColorComment  = Color(0xFF6A737D)

private val KEYWORDS = setOf(
    "SECTION", "TABLE", "TEXT", "OPEN_QUESTION", "DROP_QUESTION",
    "SELECT_QUESTION", "MULTIPLE_QUESTION", "IF", "ELSE", "WHILE",
    "DO", "FOR", "number", "string", "special",
    "VERTICAL", "HORIZONTAL", "MONO", "SANS_SERIF", "CURSIVE",
    "LINE", "DOTTED", "DOUBLE", "RED", "BLUE", "GREEN", "PURPLE",
    "SKY", "YELLOW", "BLACK", "WHITE", "width", "height",
    "pointX", "pointY", "orientation", "elements", "styles",
    "content", "label", "options", "correct", "color",
    "font", "family", "size", "border",
    "who_is_that_pokemon", "draw", "in"
)

class SyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text.text)
            applyHighlighting(text.text)
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.applyHighlighting(code: String) {
        var i = 0
        while (i < code.length) {

            // Comentario multilinea: /* ... */
            if (i + 1 < code.length && code[i] == '/' && code[i + 1] == '*') {
                val end = code.indexOf("*/", i + 2)
                    .let { if (it == -1) code.length else it + 2 }
                addStyle(SpanStyle(color = ColorComment), i, end)
                i = end
                continue
            }

            // Comentario de una linea: $ ...
            if (code[i] == '$') {
                val end = code.indexOf('\n', i)
                    .let { if (it == -1) code.length else it }
                addStyle(SpanStyle(color = ColorComment), i, end)
                i = end
                continue
            }

            // String literal: "..."
            if (code[i] == '"') {
                val end = code.indexOf('"', i + 1)
                    .let { if (it == -1) code.length else it + 1 }
                addStyle(SpanStyle(color = ColorString), i, end)
                i = end
                continue
            }

            // Especificacion de emojis: @[...]
            if (code[i] == '@' && i + 1 < code.length && code[i + 1] == '[') {
                val end = code.indexOf(']', i + 2)
                    .let { if (it == -1) code.length else it + 1 }
                addStyle(SpanStyle(color = ColorEmoji), i, end)
                i = end
                continue
            }

            // Numeros literales
            if (code[i].isDigit()) {
                var end = i + 1
                while (end < code.length && (code[end].isDigit() || code[end] == '.')) end++
                addStyle(SpanStyle(color = ColorNumber), i, end)
                i = end
                continue
            }

            // Operadores aritmeticos
            if (code[i] in setOf('+', '*', '/', '^', '%')) {
                addStyle(SpanStyle(color = ColorOperator), i, i + 1)
                i++
                continue
            }

            // Llaves, corchetes, parentesis
            if (code[i] in setOf('{', '}', '[', ']', '(', ')')) {
                addStyle(SpanStyle(color = ColorBracket), i, i + 1)
                i++
                continue
            }

            // Palabras reservadas
            if (code[i].isLetter() || code[i] == '_') {
                var end = i + 1
                while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_')) end++
                val word = code.substring(i, end)
                if (word in KEYWORDS) {
                    addStyle(SpanStyle(color = ColorKeyword), i, end)
                }
                i = end
                continue
            }

            i++
        }
    }
}