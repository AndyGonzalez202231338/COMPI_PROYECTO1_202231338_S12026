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

    // Extrae los metadatos del bloque ### ... ### y construye un encabezado visual
    private fun extraerEncabezado(raw: String): FormElement.Section? {
        val match = Regex("""###([\s\S]*?)###""").find(raw) ?: return null
        val bloque = match.groupValues[1]

        val campos = mutableMapOf<String, String>()
        bloque.lines().forEach { linea ->
            val partes = linea.trim().split(":", limit = 2)
            if (partes.size == 2) campos[partes[0].trim()] = partes[1].trim()
        }
        if (campos.isEmpty()) return null

        val elementos = mutableListOf<FormElement>()

        // Titulo: Descripcion o nombre del formulario
        val descripcion = campos["Descripcion"] ?: campos["Nombre"] ?: ""
        if (descripcion.isNotBlank()) {
            elementos.add(FormElement.TextElement(
                content = descripcion,
                style = StyleData(
                    color = "#FFFFFF",
                    backgroundColor = "#1F4E79",
                    textSize = 18f,
                    fontFamily = "SANS_SERIF"
                )
            ))
        }

        // Fila de metadatos: Autor y Fecha
        val autor = campos["Author"] ?: campos["Autor"] ?: ""
        val fecha = campos["Fecha"] ?: ""
        if (autor.isNotBlank() || fecha.isNotBlank()) {
            val meta = listOf(autor, fecha).filter { it.isNotBlank() }.joinToString("   |   ")
            elementos.add(FormElement.TextElement(
                content = meta,
                style = StyleData(color = "#AAAAAA", textSize = 13f)
            ))
        }

        // Estadisticas: secciones y preguntas
        val secciones  = campos["Total de Secciones"]  ?: ""
        val preguntas  = campos["Total de Preguntas"]   ?: ""
        if (secciones.isNotBlank() || preguntas.isNotBlank()) {
            val stats = buildString {
                if (secciones.isNotBlank())  append("Secciones: $secciones")
                if (preguntas.isNotBlank()) {
                    if (isNotBlank()) append("   |   ")
                    append("Preguntas: $preguntas")
                }
            }
            elementos.add(FormElement.TextElement(
                content = stats,
                style = StyleData(color = "#FFFFFF", backgroundColor = "#2E75B6", textSize = 14f)
            ))
        }

        if (elementos.isEmpty()) return null

        return FormElement.Section(
            orientation = "VERTICAL",
            width = null,
            height = null,
            pointX = null,
            pointY = null,
            elements = elementos,
            style = StyleData(
                backgroundColor = "#1A1A2E",
                borderSize = 3f,
                borderType = "LINE",
                borderColor = "#2E75B6"
            )
        )
    }

    private fun preprocesar(raw: String): String {
        // Eliminar bloques de metadatos ### ... ###
        var s = raw.replace(Regex("""###[\s\S]*?###"""), "")

        // Redondear TODOS los decimales a enteros en colores y tamaños 158.33333 -> 158, 120.667 -> 121, 118.5 -> 119, etc.
        s = s.replace(Regex("""(\d+\.\d+)""")) { mr ->
            mr.groupValues[1].toDoubleOrNull()?.let {
                Math.round(it).toString()
            } ?: mr.value
        }

        // Quitar espacios dentro de colores RGB (201, 101, 150) -> (201,101,150)
        s = s.replace(Regex("""\((\d+),\s*(\d+),\s*(\d+)\)""")) { mr ->
            "(${mr.groupValues[1]},${mr.groupValues[2]},${mr.groupValues[3]})"
        }

        // Quitar espacios dentro de colores HSL <20, 127, 199> -> <20,127,199>
        s = s.replace(Regex("""<(\d+),\s*(\d+),\s*(\d+)>""")) { mr ->
            "<${mr.groupValues[1]},${mr.groupValues[2]},${mr.groupValues[3]}>"
        }

        // Normalizar @[:star-N:] -> @[:star:N:] para convertirEmoji
        s = s.replace(Regex("""@\[:star-(\d+):\]""")) { mr ->
            "@[:star:" + mr.groupValues[1] + ":]"
        }

        // 6. Reemplazar -1 por 0 en contexto de tags (width/height -1 = sin dimension)
        //    <open=-1,-1,"..."> -> <open=0,0,"...">
        //    Solo reemplazar -1 que van precedidos de = o , (no dentro de cadenas)
        s = s.replace(Regex("""(?<=[=,])-1(?=[,>])"""), "0")

        return s
    }

    fun importar(contenido: String): ResultadoImport {
        return try {
            ParserPKM.resultado.clear()

            // Preprocesar el contenido antes de parsear
            val contenidoLimpio = preprocesar(contenido)
            val reader = java.io.StringReader(contenidoLimpio)
            val lexer  = LexerPKM(reader)
            val parser = ParserPKM(lexer)
            parser.parse()

            android.util.Log.d("PkmImporter", "resultado size: ${ParserPKM.resultado.size}")
            ParserPKM.resultado.forEachIndexed { i, obj ->
                android.util.Log.d("PkmImporter", "[$i] ${obj?.javaClass?.simpleName}: $obj")
            }

            val elementosParseados = ParserPKM.resultado.mapNotNull { convertirNodo(it) }
            // Construir encabezado a partir de los metadatos ### ... ###
            val encabezado = extraerEncabezado(contenido)
            val elementos = if (encabezado != null) listOf(encabezado) + elementosParseados
            else elementosParseados
            ResultadoImport(elementos)
        } catch (e: Exception) {
            android.util.Log.e("PkmImporter", "Exception tipo: ${e.javaClass.name}", e)
            android.util.Log.e("PkmImporter", "Exception mensaje: ${e.message}")
            android.util.Log.e("PkmImporter", "Stack: ${e.stackTraceToString().take(800)}")
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