package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.generated.LexerPRO1
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType

class PkmLexer {

    fun getErrors(code: String): List<ErrorToken> {
        LexerPRO1.errores.clear()

        return try {
            val reader = java.io.StringReader(normalizarColoresAngulares(code))
            val lexer = LexerPRO1(reader)

            var token = lexer.next_token()
            while (token.sym != com.example.pkmforms.analyzer.generated.sym.EOF) {
                token = lexer.next_token()
            }

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

    companion object {
        /**
         * Convierte la notacion de color con angulos <r, g, b> a la notacion
         * con parentesis (r, g, b) que acepta el parser, sin tocar strings
         * literales ni comentarios.
         *
         * Casos cubiertos:
         *   <100, 200, 50>          ->  (100, 200, 50)
         *   <120+?, 80, 200-?>      ->  (120+?, 80, 200-?)
         *   <cont*20, 100, 200>     ->  (cont*20, 100, 200)
         *
         * El < solo se convierte cuando contiene exactamente 2 comas antes
         * del > de cierre y el contenido no empieza con un digito seguido de
         * operador de comparacion (ej: condiciones IF/WHILE no se tocan porque
         * estan dentro de parentesis, no de corchetes/estilos).
         *
         * La transformacion preserva numeros de linea porque reemplaza
         * caracter por caracter con el mismo largo.
         */
        fun normalizarColoresAngulares(code: String): String {
            val sb = StringBuilder(code.length)
            var i = 0
            while (i < code.length) {
                // Saltar comentario de bloque /* ... */
                if (i + 1 < code.length && code[i] == '/' && code[i + 1] == '*') {
                    val end = code.indexOf("*/", i + 2).let { if (it == -1) code.length - 2 else it }
                    sb.append(code, i, end + 2)
                    i = end + 2
                    continue
                }
                // Saltar comentario de linea $ ...
                if (code[i] == '$') {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    sb.append(code, i, end)
                    i = end
                    continue
                }
                // Saltar strings "..."
                if (code[i] == '"') {
                    val end = code.indexOf('"', i + 1).let { if (it == -1) code.length - 1 else it }
                    sb.append(code, i, end + 1)
                    i = end + 1
                    continue
                }
                // Candidato a color angular: <
                if (code[i] == '<') {
                    val cierreIdx = encontrarCierreColor(code, i + 1)
                    if (cierreIdx != -1) {
                        // Reemplazar < por ( y > por ), conservando el interior
                        sb.append('(')
                        sb.append(code, i + 1, cierreIdx)
                        sb.append(')')
                        i = cierreIdx + 1
                        continue
                    }
                }
                sb.append(code[i])
                i++
            }
            return sb.toString()
        }

        /**
         * Desde la posicion [start] (inmediatamente despues del <) busca
         * el > de cierre de un triplete de color.
         * Devuelve el indice del > si el contenido tiene exactamente 2 comas
         * al nivel superior (sin parentesis anidados) y no hay saltos de linea
         * que rompan la expresion de color.
         * Devuelve -1 si no corresponde a un color.
         */
        private fun encontrarCierreColor(code: String, start: Int): Int {
            var depth = 0
            var comas = 0
            var j = start
            while (j < code.length) {
                when (code[j]) {
                    '(' -> depth++
                    ')' -> { if (depth == 0) return -1; depth-- }
                    ',' -> { if (depth == 0) comas++ }
                    '>' -> {
                        if (depth == 0 && comas == 2) return j
                        if (depth == 0) return -1
                    }
                    '\n' -> return -1   // color angular no cruza lineas
                    '"'   -> return -1   // no entrar en strings
                }
                j++
            }
            return -1
        }
    }
}