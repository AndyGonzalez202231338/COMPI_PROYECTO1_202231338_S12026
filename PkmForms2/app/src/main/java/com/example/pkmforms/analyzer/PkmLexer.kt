package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.generated.LexerPRO1
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType

class PkmLexer {

    fun getErrors(code: String): List<ErrorToken> {
        // Limpia errores previos antes de analizar
        LexerPRO1.errores.clear()

        return try {
            val reader = java.io.StringReader(code)
            val lexer = LexerPRO1(reader)

            // Recorre todos los tokens para que el lexer registre los errores
            var token = lexer.next_token()
            while (token.sym != com.example.pkmforms.analyzer.generated.sym.EOF) {
                token = lexer.next_token()
            }

            // Convierte los errores del lexer al modelo de la app
            LexerPRO1.errores.map { error ->
                ErrorToken(
                    lexeme    = error[0],
                    line      = error[1].toIntOrNull() ?: 0,
                    column    = error[2].toIntOrNull() ?: 0,
                    type      = ErrorType.LEXICO,
                    description = error[4]
                )
            }

        } catch (e: Exception) {
            listOf(
                ErrorToken(
                    lexeme      = "",
                    line        = 0,
                    column      = 0,
                    type        = ErrorType.LEXICO,
                    description = e.message ?: "Error inesperado en el analizador lexico"
                )
            )
        }
    }
}