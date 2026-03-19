package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.analyzer.model.StyleData
import com.example.pkmforms.analyzer.pkm.LexerPKM
import com.example.pkmforms.analyzer.pkm.ParserPKM
import com.example.pkmforms.analyzer.pkm.PkmStyleNode
import com.example.pkmforms.analyzer.pkm.PkmSectionNode
import com.example.pkmforms.analyzer.pkm.PkmTableNode
import com.example.pkmforms.analyzer.pkm.PkmOpenNode
import com.example.pkmforms.analyzer.pkm.PkmDropNode
import com.example.pkmforms.analyzer.pkm.PkmSelectNode
import com.example.pkmforms.analyzer.pkm.PkmMultipleNode
import com.example.pkmforms.analyzer.pkm.PkmTextNode

object PkmImporter {

    data class ResultadoImport(
        val elementos: List<FormElement>,
        val error:     String? = null
    )

    fun importar(contenido: String): ResultadoImport {
        return try {
            ParserPKM.resultado.clear()

            val reader = java.io.StringReader(contenido)
            val lexer  = LexerPKM(reader)
            val parser = ParserPKM(lexer)
            parser.parse()

            android.util.Log.d("PkmImporter", "resultado size: ${ParserPKM.resultado.size}")
            ParserPKM.resultado.forEachIndexed { i, obj ->
                android.util.Log.d("PkmImporter", "[$i] ${obj?.javaClass?.simpleName}: $obj")
            }

            val elementos = ParserPKM.resultado.mapNotNull { convertirNodo(it) }
            android.util.Log.d("PkmImporter", "elementos convertidos: ${elementos.size}")
            ResultadoImport(elementos)
        } catch (e: Exception) {
            android.util.Log.e("PkmImporter", "Exception: ${e.message}", e)
            ResultadoImport(emptyList(), "Error al leer el archivo: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertirNodo(obj: Any?): FormElement? {
        fun Int.nullIfZero(): Int? = if (this == 0) null else this

        return when (obj) {
            is PkmSectionNode -> FormElement.Section(
                width       = obj.width.nullIfZero(),
                height      = obj.height.nullIfZero(),
                pointX      = obj.pointX,
                pointY      = obj.pointY,
                orientation = obj.orientation,
                elements    = obj.elementos.mapNotNull { convertirNodo(it) },
                style       = convertirEstilo(obj.style)
            )
            is PkmTableNode -> FormElement.Table(
                width  = obj.width.nullIfZero(),
                height = obj.height.nullIfZero(),
                pointX = obj.pointX,
                pointY = obj.pointY,
                rows   = (obj.filas as List<List<Any?>>).map { fila ->
                    fila.map { celda -> convertirNodo(celda) }
                },
                style  = convertirEstilo(obj.style)
            )
            is PkmOpenNode -> FormElement.OpenQuestion(
                label  = obj.label,
                width  = obj.width.nullIfZero(),
                height = obj.height.nullIfZero(),
                style  = convertirEstilo(obj.style)
            )
            is PkmDropNode -> FormElement.DropQuestion(
                label   = obj.label,
                options = obj.opciones.filterIsInstance<String>(),
                correct = obj.correct,
                width   = obj.width.nullIfZero(),
                height  = obj.height.nullIfZero(),
                style   = convertirEstilo(obj.style)
            )
            is PkmSelectNode -> FormElement.SelectQuestion(
                label   = obj.label,
                options = obj.opciones.filterIsInstance<String>(),
                correct = obj.correct,
                width   = obj.width.nullIfZero(),
                height  = obj.height.nullIfZero(),
                style   = convertirEstilo(obj.style)
            )
            is PkmMultipleNode -> FormElement.MultipleQuestion(
                label   = obj.label,
                options = obj.opciones.filterIsInstance<String>(),
                correct = obj.correctos.filterIsInstance<Int>(),
                width   = obj.width.nullIfZero(),
                height  = obj.height.nullIfZero(),
                style   = convertirEstilo(obj.style)
            )
            is PkmTextNode -> FormElement.TextElement(
                content = obj.content,
                width   = obj.width.nullIfZero(),
                height  = obj.height.nullIfZero(),
                style   = convertirEstilo(obj.style)
            )
            else -> null
        }
    }

    private fun convertirEstilo(s: PkmStyleNode?): StyleData {
        if (s == null) return StyleData()
        return StyleData(
            color           = s.color,
            backgroundColor = s.backgroundColor,
            fontFamily      = s.fontFamily,
            textSize        = s.textSize,
            borderSize      = s.borderSize,
            borderType      = s.borderType,
            borderColor     = s.borderColor
        )
    }
}