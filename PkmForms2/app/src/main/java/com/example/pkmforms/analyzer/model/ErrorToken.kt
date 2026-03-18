package com.example.pkmforms.analyzer.model

data class ErrorToken(
    val lexeme: String,
    val line: Int,
    val column: Int,
    val type: ErrorType,
    val description: String
)
enum class ErrorType {
    LEXICO,
    SINTACTICO,
    SEMANTICO,
    ADVERTENCIA
}
