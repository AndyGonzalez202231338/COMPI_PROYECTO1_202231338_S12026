package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.generated.DropQuestionNode
import com.example.pkmforms.analyzer.generated.LexerPRO1
import com.example.pkmforms.analyzer.generated.MultipleQuestionNode
import com.example.pkmforms.analyzer.generated.OpenQuestionNode
import com.example.pkmforms.analyzer.generated.ParserPRO1
import com.example.pkmforms.analyzer.generated.SelectQuestionNode
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType
import com.example.pkmforms.analyzer.model.FormElement

data class ParseResult(
    val elements : List<FormElement>,
    val errors   : List<ErrorToken>
)

class PkmParser {

    fun parse(code: String): ParseResult {
        LexerPRO1.errores.clear()
        ParserPRO1.errores.clear()
        ParserPRO1.resultado.clear()

        return try {
            val reader = java.io.StringReader(code)
            val lexer  = LexerPRO1(reader)
            val parser = ParserPRO1(lexer)
            parser.parse()

            val allErrors = mutableListOf<ErrorToken>()

            LexerPRO1.errores.forEach { e ->
                allErrors.add(ErrorToken(
                    lexeme      = e[0],
                    line        = e[1].toIntOrNull() ?: 0,
                    column      = e[2].toIntOrNull() ?: 0,
                    type        = ErrorType.LEXICO,
                    description = e[4]
                ))
            }

            ParserPRO1.errores.forEach { e ->
                allErrors.add(ErrorToken(
                    lexeme      = e[0],
                    line        = e[1].toIntOrNull() ?: 0,
                    column      = e[2].toIntOrNull() ?: 0,
                    type        = ErrorType.SINTACTICO,
                    description = e[4]
                ))
            }

            val elements = ParserPRO1.resultado.mapNotNull { node ->
                convertirNodo(node)
            }

            ParseResult(elements = elements, errors = allErrors)

        } catch (e: Exception) {
            ParseResult(
                elements = emptyList(),
                errors   = listOf(ErrorToken(
                    lexeme      = "",
                    line        = 0,
                    column      = 0,
                    type        = ErrorType.SINTACTICO,
                    description = e.message ?: "Error inesperado"
                ))
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertirNodo(node: Any): FormElement? = when (node) {

        is OpenQuestionNode ->{
        FormElement.OpenQuestion(
            label = node.label,
            width = if (node.width > 0.0) node.width.toInt() else null,
            height = if (node.height > 0.0) node.height.toInt() else null
        )
    }

        is DropQuestionNode -> FormElement.DropQuestion(
            label   = node.label,
            options = node.options.toList(),
            correct = node.correct,
            width   = if (node.width  > 0.0) node.width.toInt()  else null,
            height  = if (node.height > 0.0) node.height.toInt() else null
        )

        is SelectQuestionNode -> FormElement.SelectQuestion(
            options = node.options.toList(),
            correct = node.correct,
            width   = if (node.width  > 0.0) node.width.toInt()  else null,
            height  = if (node.height > 0.0) node.height.toInt() else null
        )

        is MultipleQuestionNode -> FormElement.MultipleQuestion(
            options = node.options.toList(),
            correct = node.correct.map { it },
            width   = if (node.width  > 0.0) node.width.toInt()  else null,
            height  = if (node.height > 0.0) node.height.toInt() else null
        )

        else -> null
    }
}