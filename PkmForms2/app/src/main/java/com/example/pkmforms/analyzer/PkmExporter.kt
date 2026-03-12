package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.analyzer.model.StyleData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PkmExporter {

    fun exportar(
        elementos: List<FormElement>,
        author: String,
        description: String
    ): String {
        val sb = StringBuilder()

        // Calcular estadisticas para los metadatos
        val stats = calcularEstadisticas(elementos)
        val ahora = Date()
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora)
        val hora  = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora)

        sb.appendLine("###")
        sb.appendLine("Author: $author")
        sb.appendLine("Fecha: $fecha")
        sb.appendLine("Hora: $hora")
        sb.appendLine("Description: $description")
        sb.appendLine("Total de Secciones: ${stats.secciones}")
        sb.appendLine("Total de Preguntas: ${stats.totalPreguntas}")
        sb.appendLine("    Abiertas: ${stats.abiertas}")
        sb.appendLine("    Desplegables: ${stats.desplegables}")
        sb.appendLine("    Seleccion: ${stats.seleccion}")
        sb.appendLine("    Multiples: ${stats.multiples}")
        sb.appendLine("###")
        sb.appendLine()

        for (elemento in elementos) {
            sb.append(exportarElemento(elemento, 0))
            sb.appendLine()
        }

        return sb.toString()
    }


    // EXPORTACION RECURSIVA DE ELEMENTOS
    private fun exportarElemento(elemento: FormElement, nivel: Int): String {
        val ind = "    ".repeat(nivel)
        return when (elemento) {
            is FormElement.Section    -> exportarSection(elemento, nivel)
            is FormElement.Table      -> exportarTable(elemento, nivel)
            is FormElement.TextElement -> exportarText(elemento, nivel)
            is FormElement.OpenQuestion    -> exportarOpen(elemento, nivel)
            is FormElement.DropQuestion    -> exportarDrop(elemento, nivel)
            is FormElement.SelectQuestion  -> exportarSelect(elemento, nivel)
            is FormElement.MultipleQuestion -> exportarMultiple(elemento, nivel)
        }
    }

    // ============ SECTION ============
    private fun exportarSection(s: FormElement.Section, nivel: Int): String {
        val ind = "    ".repeat(nivel)
        val w   = s.width  ?: 0
        val h   = s.height ?: 0
        val px  = s.pointX ?: 0
        val py  = s.pointY ?: 0
        val ori = s.orientation.ifBlank { "VERTICAL" }
        val sb  = StringBuilder()

        sb.appendLine("${ind}<section=$w,$h,$px,$py,$ori>")

        if (!s.style.estaVacio()) {
            sb.append(exportarEstilo(s.style, nivel + 1))
        }

        if (s.elements.isNotEmpty()) {
            sb.appendLine("${ind}    <content>")
            for (hijo in s.elements) {
                sb.append(exportarElemento(hijo, nivel + 2))
            }
            sb.appendLine("${ind}    </content>")
        }

        sb.append("${ind}</section>")
        return sb.toString()
    }

    // ============ TABLE ============
    private fun exportarTable(t: FormElement.Table, nivel: Int): String {
        val ind = "    ".repeat(nivel)
        val w   = t.width  ?: 0
        val h   = t.height ?: 0
        val px  = t.pointX ?: 0
        val py  = t.pointY ?: 0
        val sb  = StringBuilder()

        sb.appendLine("${ind}<table=$w,$h,$px,$py>")

        if (!t.style.estaVacio()) {
            sb.append(exportarEstilo(t.style, nivel + 1))
        }

        if (t.rows.isNotEmpty()) {
            sb.appendLine("${ind}    <content>")
            for (fila in t.rows) {
                sb.appendLine("${ind}        <line>")
                for (celda in fila) {
                    sb.appendLine("${ind}            <element>")
                    if (celda != null) {
                        sb.append(exportarElemento(celda, nivel + 4))
                        sb.appendLine()
                    }
                    sb.appendLine("${ind}            </element>")
                }
                sb.appendLine("${ind}        </line>")
            }
            sb.appendLine("${ind}    </content>")
        }

        sb.append("${ind}</table>")
        return sb.toString()
    }

    // ============ TEXT ============
    private fun exportarText(t: FormElement.TextElement, nivel: Int): String {
        val ind  = "    ".repeat(nivel)
        val w    = t.width  ?: 0
        val h    = t.height ?: 0
        val cont = escaparTexto(t.content)

        return if (t.style.estaVacio()) {
            "${ind}<text=$w,$h,\"$cont\"/>\n"
        } else {
            val sb = StringBuilder()
            sb.appendLine("${ind}<text=$w,$h,\"$cont\">")
            sb.append(exportarEstilo(t.style, nivel + 1))
            sb.append("${ind}</text>")
            sb.toString()
        }
    }

    // ============ OPEN QUESTION ============
    private fun exportarOpen(q: FormElement.OpenQuestion, nivel: Int): String {
        val ind   = "    ".repeat(nivel)
        val w     = q.width  ?: 0
        val h     = q.height ?: 0
        val label = escaparTexto(q.label)

        return if (q.style.estaVacio()) {
            "${ind}<open=$w,$h,\"$label\"/>\n"
        } else {
            val sb = StringBuilder()
            sb.appendLine("${ind}<open=$w,$h,\"$label\">")
            sb.append(exportarEstilo(q.style, nivel + 1))
            sb.append("${ind}</open>")
            sb.toString()
        }
    }

    // ============ DROP QUESTION ============
    private fun exportarDrop(q: FormElement.DropQuestion, nivel: Int): String {
        val ind     = "    ".repeat(nivel)
        val w       = q.width  ?: 0
        val h       = q.height ?: 0
        val label   = escaparTexto(q.label)
        val options = q.options.joinToString(", ") { "\"${escaparTexto(it)}\"" }
        val correct = q.correct

        return if (q.style.estaVacio()) {
            "${ind}<drop=$w,$h,\"$label\",{$options},$correct/>\n"
        } else {
            val sb = StringBuilder()
            sb.appendLine("${ind}<drop=$w,$h,\"$label\",{$options},$correct>")
            sb.append(exportarEstilo(q.style, nivel + 1))
            sb.append("${ind}</drop>")
            sb.toString()
        }
    }

    // ============ SELECT QUESTION ============
    private fun exportarSelect(q: FormElement.SelectQuestion, nivel: Int): String {
        val ind     = "    ".repeat(nivel)
        val w       = q.width  ?: 0
        val h       = q.height ?: 0
        val options = q.options.joinToString(", ") { "\"${escaparTexto(it)}\"" }
        val correct = q.correct

        return if (q.style.estaVacio()) {
            "${ind}<select=$w,$h,{$options},$correct/>\n"
        } else {
            val sb = StringBuilder()
            sb.appendLine("${ind}<select=$w,$h,{$options},$correct>")
            sb.append(exportarEstilo(q.style, nivel + 1))
            sb.append("${ind}</select>")
            sb.toString()
        }
    }

    // ============ MULTIPLE QUESTION ============
    private fun exportarMultiple(q: FormElement.MultipleQuestion, nivel: Int): String {
        val ind     = "    ".repeat(nivel)
        val w       = q.width  ?: 0
        val h       = q.height ?: 0
        val options = q.options.joinToString(", ") { "\"${escaparTexto(it)}\"" }
        val correct = q.correct.joinToString(", ")

        return if (q.style.estaVacio()) {
            "${ind}<multiple=$w,$h,{$options},{$correct}/>\n"
        } else {
            val sb = StringBuilder()
            sb.appendLine("${ind}<multiple=$w,$h,{$options},{$correct}>")
            sb.append(exportarEstilo(q.style, nivel + 1))
            sb.append("${ind}</multiple>")
            sb.toString()
        }
    }

    // ============ ESTILOS ============
    private fun exportarEstilo(s: StyleData, nivel: Int): String {
        val ind = "    ".repeat(nivel)
        val sb  = StringBuilder()
        sb.appendLine("${ind}<style>")
        if (s.color.isNotBlank())
            sb.appendLine("${ind}    <color=${s.color}/>")
        if (s.backgroundColor.isNotBlank())
            sb.appendLine("${ind}    <background color=${s.backgroundColor}/>")
        if (s.fontFamily.isNotBlank())
            sb.appendLine("${ind}    <font family=${s.fontFamily}/>")
        if (s.textSize > 0f)
            sb.appendLine("${ind}    <text size=${s.textSize.toInt()}>")
        if (s.borderSize > 0f && s.borderType.isNotBlank() && s.borderColor.isNotBlank())
            sb.appendLine("${ind}    <border,${s.borderSize.toInt()},${s.borderType},color=${s.borderColor}/>")
        sb.append("${ind}</style>\n")
        return sb.toString()
    }

    // ESTADISTICAS RECURSIVAS

    data class Estadisticas(
        val secciones:     Int = 0,
        val abiertas:      Int = 0,
        val desplegables:  Int = 0,
        val seleccion:     Int = 0,
        val multiples:     Int = 0
    ) {
        val totalPreguntas get() = abiertas + desplegables + seleccion + multiples

        operator fun plus(otro: Estadisticas) = Estadisticas(
            secciones    = secciones    + otro.secciones,
            abiertas     = abiertas     + otro.abiertas,
            desplegables = desplegables + otro.desplegables,
            seleccion    = seleccion    + otro.seleccion,
            multiples    = multiples    + otro.multiples
        )
    }

    private fun calcularEstadisticas(elementos: List<FormElement>): Estadisticas {
        var stats = Estadisticas()
        for (el in elementos) {
            stats = stats + estadisticasDeElemento(el)
        }
        return stats
    }

    private fun estadisticasDeElemento(el: FormElement): Estadisticas {
        return when (el) {
            is FormElement.OpenQuestion     -> Estadisticas(abiertas = 1)
            is FormElement.DropQuestion     -> Estadisticas(desplegables = 1)
            is FormElement.SelectQuestion   -> Estadisticas(seleccion = 1)
            is FormElement.MultipleQuestion -> Estadisticas(multiples = 1)
            is FormElement.TextElement      -> Estadisticas()
            is FormElement.Section -> {
                val hijoStats = calcularEstadisticas(el.elements)
                Estadisticas(secciones = 1) + hijoStats
            }
            is FormElement.Table -> {
                var t = Estadisticas()
                for (fila in el.rows)
                    for (celda in fila)
                        if (celda != null) t = t + estadisticasDeElemento(celda)
                t
            }
        }
    }


    // Los emojis ya vienen como texto plano desde FormElement,
    // no se guardan como tal sino con la notacion @[...]
    // Por ahora se guarda el texto tal cual (sin emojis Unicode)
    private fun escaparTexto(texto: String): String = texto

    private fun StyleData.estaVacio(): Boolean =
        color.isBlank() && backgroundColor.isBlank() && fontFamily.isBlank() &&
                textSize == 0f && borderSize == 0f && borderType.isBlank() && borderColor.isBlank()
}