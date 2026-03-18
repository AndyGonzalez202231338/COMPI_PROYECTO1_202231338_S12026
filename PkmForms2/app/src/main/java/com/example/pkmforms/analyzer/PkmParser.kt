package com.example.pkmforms.analyzer

import com.example.pkmforms.analyzer.generated.DropQuestionNode
import com.example.pkmforms.analyzer.generated.LexerPRO1
import com.example.pkmforms.analyzer.generated.MultipleQuestionNode
import com.example.pkmforms.analyzer.generated.OpenQuestionNode
import com.example.pkmforms.analyzer.generated.ParserPRO1
import com.example.pkmforms.analyzer.generated.SelectQuestionNode
import com.example.pkmforms.analyzer.generated.SectionNode
import com.example.pkmforms.analyzer.generated.StyleNode
import com.example.pkmforms.analyzer.generated.TableNode
import com.example.pkmforms.analyzer.generated.TextNode
import com.example.pkmforms.analyzer.generated.SpecialNode
import com.example.pkmforms.analyzer.code.IfNode
import com.example.pkmforms.analyzer.code.WhileNode
import com.example.pkmforms.analyzer.code.DoWhileNode
import com.example.pkmforms.analyzer.code.ForNode
import com.example.pkmforms.analyzer.model.ErrorToken
import com.example.pkmforms.analyzer.model.ErrorType
import com.example.pkmforms.analyzer.model.FormElement
import com.example.pkmforms.analyzer.model.StyleData

data class ParseResult(
    val elements : List<FormElement>,
    val errors   : List<ErrorToken>
)

@Suppress("UNCHECKED_CAST")
class PkmParser {

    private val simbolos           = mutableMapOf<String, Any>()
    private val tiposDecl          = mutableMapOf<String, String>()
    private val speciales          = mutableMapOf<String, SpecialNode>()
    private val advertenciasSelect = mutableListOf<ErrorToken>()
    private val erroresLogica      = mutableListOf<ErrorToken>()

    private fun evaluarExpr(v: Any?): Double? {
        return when (v) {
            is Number -> v.toDouble()
            is String -> {
                val enSimbolos = simbolos[v]
                when (enSimbolos) {
                    is Number -> enSimbolos.toDouble()
                    null      -> v.toDoubleOrNull()
                    else      -> null
                }
            }
            is Array<*> -> {
                val op = v[0] as? String ?: return null
                val a  = evaluarExpr(v.getOrNull(1))
                val b  = if (v.size > 2) evaluarExpr(v.getOrNull(2)) else null
                when (op) {
                    "op+"  -> if (a != null && b != null) a + b else null
                    "op-"  -> if (a != null && b != null) a - b else null
                    "op*"  -> if (a != null && b != null) a * b else null
                    "op/"  -> if (a != null && b != null && b != 0.0) a / b else null
                    "op%"  -> if (a != null && b != null && b != 0.0) a % b else null
                    "op^"  -> if (a != null && b != null) Math.pow(a, b) else null
                    "op_neg" -> if (a != null) -a else null
                    "op>"  -> if (a != null && b != null) if (a > b)  1.0 else 0.0 else null
                    "op>=" -> if (a != null && b != null) if (a >= b) 1.0 else 0.0 else null
                    "op<"  -> if (a != null && b != null) if (a < b)  1.0 else 0.0 else null
                    "op<=" -> if (a != null && b != null) if (a <= b) 1.0 else 0.0 else null
                    "op==" -> if (a != null && b != null) if (a == b) 1.0 else 0.0 else null
                    "op!=" -> if (a != null && b != null) if (a != b) 1.0 else 0.0 else null
                    "op||" -> if ((a ?: 0.0) >= 1.0 || (b ?: 0.0) >= 1.0) 1.0 else 0.0
                    "op&&" -> if ((a ?: 0.0) >= 1.0 && (b ?: 0.0) >= 1.0) 1.0 else 0.0
                    "op!"  -> if ((a ?: 0.0) >= 1.0) 0.0 else 1.0
                    else   -> null
                }
            }
            else -> null
        }
    }

    private fun evaluarCondicion(v: Any?): Boolean = (evaluarExpr(v) ?: 0.0) >= 1.0

    // Detecta si una expresion mezcla || y && al mismo nivel
    private fun tieneMezclaLogica(v: Any?): Boolean {
        if (v !is Array<*>) return false
        val op = v[0] as? String ?: return false
        if (op != "op||" && op != "op&&") return false
        return buscarOperadorContrario(v, op)
    }

    private fun buscarOperadorContrario(v: Any?, opPadre: String): Boolean {
        if (v !is Array<*>) return false
        val op = v[0] as? String ?: return false
        val contrario = if (opPadre == "op||") "op&&" else "op||"
        if (op == contrario) return true
        // Solo buscar en hijos si tienen el mismo operador padre (mismo nivel)
        if (op == opPadre) {
            return buscarOperadorContrario(v.getOrNull(1), opPadre) ||
                    buscarOperadorContrario(v.getOrNull(2), opPadre)
        }
        return false
    }

    private fun validarMezclaLogica(v: Any?, linea: Int, allErrors: MutableList<ErrorToken>) {
        if (tieneMezclaLogica(v)) {
            allErrors.add(ErrorToken(
                lexeme      = "expresion",
                line        = linea,
                column      = 0,
                type        = ErrorType.SEMANTICO,
                description = "No se pueden mezclar operadores logicos '||' y '&&' en la misma expresion."
            ))
        }
    }

    private fun resolverNum(v: Any?): Double? = evaluarExpr(v)

    private fun resolverStr(v: Any?): String? = when (v) {
        is String -> simbolos[v] as? String ?: v
        else      -> v?.toString()
    }

    fun parse(code: String): ParseResult {
        LexerPRO1.errores.clear()
        ParserPRO1.errores.clear()
        ParserPRO1.resultado.clear()
        simbolos.clear()
        tiposDecl.clear()
        speciales.clear()
        advertenciasSelect.clear()
        erroresLogica.clear()

        return try {
            val reader = java.io.StringReader(code)
            val lexer  = LexerPRO1(reader)
            val parser = ParserPRO1(lexer)
            parser.parse()

            val allErrors = mutableListOf<ErrorToken>()

            LexerPRO1.errores.forEach { e ->
                allErrors.add(ErrorToken(lexeme = e[0], line = e[1].toIntOrNull() ?: 0,
                    column = e[2].toIntOrNull() ?: 0, type = ErrorType.LEXICO, description = e[4]))
            }
            ParserPRO1.errores.forEach { e ->
                allErrors.add(ErrorToken(lexeme = e[0], line = e[1].toIntOrNull() ?: 0,
                    column = e[2].toIntOrNull() ?: 0, type = ErrorType.SINTACTICO, description = e[4]))
            }

            // Primera pasada: variables y speciales
            ParserPRO1.resultado.forEach { entry ->
                if (entry is Array<*> && entry.size == 4) {
                    val nombre = entry[0] as? String ?: return@forEach
                    val valor  = entry[1]
                    val tipo   = entry[2] as? String ?: return@forEach
                    val linea  = (entry[3] as? Int) ?: 0
                    when (tipo) {
                        "number" -> {
                            if (tiposDecl.containsKey(nombre)) {
                                allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                    type = ErrorType.SEMANTICO, description = "Variable '$nombre' ya fue declarada."))
                            } else {
                                // Detectar asignacion de cadena literal a variable number
                                val esLiteralCadena = valor is String && simbolos[valor] == null
                                if (esLiteralCadena) {
                                    tiposDecl[nombre] = "number"
                                    allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                        type = ErrorType.SEMANTICO,
                                        description = "Tipo incorrecto: '$nombre' es number pero se le asigno un string."))
                                } else {
                                    tiposDecl[nombre] = "number"
                                    evaluarExpr(valor)?.let { simbolos[nombre] = it }
                                }
                            }
                        }
                        "number_type_error" -> {
                            tiposDecl.getOrPut(nombre) { "number" }
                            allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                type = ErrorType.SEMANTICO,
                                description = "Tipo incorrecto: '$nombre' es number pero se le asigno un string."))
                        }
                        "string" -> {
                            if (tiposDecl.containsKey(nombre)) {
                                allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                    type = ErrorType.SEMANTICO, description = "Variable '$nombre' ya fue declarada."))
                            } else {
                                tiposDecl[nombre] = "string"
                                if (valor is String && simbolos[valor] == null) simbolos[nombre] = valor
                                else evaluarExpr(valor)?.let { simbolos[nombre] = it.toString() }
                            }
                        }
                        "string_type_error" -> {
                            tiposDecl.getOrPut(nombre) { "string" }
                            allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                type = ErrorType.SEMANTICO,
                                description = "Tipo incorrecto: '$nombre' es string pero se le asigno un numero."))
                        }
                        "special" -> {
                            val node = valor as? SpecialNode ?: return@forEach
                            if (speciales.containsKey(nombre)) {
                                allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                    type = ErrorType.SEMANTICO, description = "Variable special '$nombre' ya fue declarada."))
                            } else {
                                speciales[nombre] = node
                            }
                        }
                        "assign" -> {
                            val tipoDeclarado = tiposDecl[nombre]
                            if (tipoDeclarado == null) {
                                allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                    type = ErrorType.SEMANTICO, description = "Variable '$nombre' no ha sido declarada."))
                            } else {
                                val esLiteralCadena = valor is String && simbolos[valor] == null
                                if (tipoDeclarado == "number" && esLiteralCadena) {
                                    allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                        type = ErrorType.SEMANTICO,
                                        description = "Tipo incorrecto: '$nombre' es number pero se le asigno un string."))
                                } else if (tipoDeclarado == "string" && valor is Number) {
                                    allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                        type = ErrorType.SEMANTICO,
                                        description = "Tipo incorrecto: '$nombre' es string pero se le asigno un numero."))
                                } else {
                                    val vr = evaluarExpr(valor)
                                    if (vr != null) simbolos[nombre] = vr
                                    else if (esLiteralCadena) simbolos[nombre] = valor as String
                                }
                            }
                        }
                    }
                }
            }

            // Segunda pasada: convertir a FormElement
            val elementos = mutableListOf<FormElement>()

            // Pre-validacion: recorrer todos los nodos buscando mezcla logica
            // antes de interpretar, para no depender de si el bloque se ejecuta
            fun validarMezclaEnNodo(nodo: Any?) {
                when (nodo) {
                    is IfNode -> {
                        for (ramaObj in nodo.ramas) {
                            val rama      = ramaObj as? Array<*> ?: continue
                            val condicion = rama[0] ?: continue
                            if (tieneMezclaLogica(condicion)) {
                                allErrors.add(ErrorToken(
                                    lexeme      = "IF",
                                    line        = nodo.linea,
                                    column      = 0,
                                    type        = ErrorType.SEMANTICO,
                                    description = "No se pueden mezclar operadores logicos '||' y '&&' en la misma expresion."
                                ))
                            }
                            val cuerpo = rama[1] as? java.util.ArrayList<*> ?: continue
                            cuerpo.forEach { validarMezclaEnNodo(it) }
                        }
                    }
                    is WhileNode -> {
                        if (tieneMezclaLogica(nodo.condicion)) {
                            allErrors.add(ErrorToken(
                                lexeme      = "WHILE",
                                line        = nodo.linea,
                                column      = 0,
                                type        = ErrorType.SEMANTICO,
                                description = "No se pueden mezclar operadores logicos '||' y '&&' en la misma expresion."
                            ))
                        }
                        (nodo.cuerpo as? java.util.ArrayList<*>)?.forEach { validarMezclaEnNodo(it) }
                    }
                    is DoWhileNode -> {
                        if (tieneMezclaLogica(nodo.condicion)) {
                            allErrors.add(ErrorToken(
                                lexeme      = "DO_WHILE",
                                line        = nodo.linea,
                                column      = 0,
                                type        = ErrorType.SEMANTICO,
                                description = "No se pueden mezclar operadores logicos '||' y '&&' en la misma expresion."
                            ))
                        }
                        (nodo.cuerpo as? java.util.ArrayList<*>)?.forEach { validarMezclaEnNodo(it) }
                    }
                    is ForNode -> {
                        if (tieneMezclaLogica(nodo.condicion)) {
                            allErrors.add(ErrorToken(
                                lexeme      = "FOR",
                                line        = nodo.linea,
                                column      = 0,
                                type        = ErrorType.SEMANTICO,
                                description = "No se pueden mezclar operadores logicos '||' y '&&' en la misma expresion."
                            ))
                        }
                        (nodo.cuerpo as? java.util.ArrayList<*>)?.forEach { validarMezclaEnNodo(it) }
                    }
                }
            }
            ParserPRO1.resultado.forEach { validarMezclaEnNodo(it) }

            ParserPRO1.resultado.forEach { node ->
                when {
                    node is Array<*> && node.size >= 3 && node[0] == "draw" -> {
                        val nombre  = node[1] as? String ?: return@forEach
                        val args    = node[2] as? java.util.ArrayList<*> ?: java.util.ArrayList<Any>()
                        val linea   = (node.getOrNull(3) as? Int) ?: 0
                        val special = speciales[nombre]
                        if (special == null) {
                            allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                type = ErrorType.SEMANTICO,
                                description = "Variable special '$nombre' no ha sido declarada."))
                        } else if (args.size != special.comodines) {
                            allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                type = ErrorType.SEMANTICO,
                                description = "draw() de '$nombre' recibio ${args.size} argumento(s) pero la variable tiene ${special.comodines} comodin(es)."))
                        } else {
                            convertirSpecial(special, args.mapNotNull { resolverNum(it) })?.let { elementos.add(it) }
                        }
                    }
                    node is Array<*>  -> { }
                    node is IfNode      -> elementos.addAll(interpretarIf(node))
                    node is WhileNode   -> elementos.addAll(interpretarWhile(node))
                    node is DoWhileNode -> elementos.addAll(interpretarDoWhile(node))
                    node is ForNode     -> elementos.addAll(interpretarFor(node))
                    else -> convertirNodo(node)?.let { elementos.add(it) }
                }
            }

            // Agregar advertencias de SELECT con mas de 5 opciones
            allErrors.addAll(advertenciasSelect)
            // Agregar errores de operadores logicos mezclados
            allErrors.addAll(erroresLogica)

            ParseResult(elements = elementos, errors = allErrors)

        } catch (e: Exception) {
            ParseResult(elements = emptyList(), errors = listOf(ErrorToken(
                lexeme = "-", line = 0, column = 0, type = ErrorType.SINTACTICO,
                description = e.message ?: "Error inesperado")))
        }
    }

    private fun convertirNodo(obj: Any?): FormElement? {
        return when (obj) {
            is OpenQuestionNode -> FormElement.OpenQuestion(
                label = resolverStr(obj.label) ?: obj.label,
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                style = convertirEstilo(obj.style))
            is DropQuestionNode -> {
                val opciones = obj.options.filterIsInstance<String>()
                val correct  = resolverNum(obj.correctVal)?.toInt() ?: -1
                // Solo validar rango si hay opciones estaticas si esta vacio
                // significa que vienen de who_is_that_pokemon (dinamicas)
                if (obj.correctVal != null && opciones.isNotEmpty() && (correct < 0 || correct >= opciones.size)) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "DROP_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "DROP_QUESTION: correct=$correct esta fuera del rango de opciones (0..${opciones.size - 1})."
                    ))
                }
                FormElement.DropQuestion(
                    label   = resolverStr(obj.label) ?: obj.label,
                    options = opciones,
                    correct = correct,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            is SelectQuestionNode -> {
                val opciones = obj.options.filterIsInstance<String>()
                val correct  = resolverNum(obj.correctVal)?.toInt() ?: -1
                if (opciones.size > 5) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "SELECT_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.ADVERTENCIA,
                        description = "SELECT_QUESTION tiene ${opciones.size} opciones (mas de 5). Se agregara al formulario de todas formas."
                    ))
                }
                if (obj.correctVal != null && opciones.isNotEmpty() && (correct < 0 || correct >= opciones.size)) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "SELECT_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "SELECT_QUESTION: correct=$correct esta fuera del rango de opciones (0..${opciones.size - 1})."
                    ))
                }
                FormElement.SelectQuestion(
                    options = opciones,
                    correct = correct,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            is MultipleQuestionNode -> FormElement.MultipleQuestion(
                label   = "",
                options = obj.options.filterIsInstance<String>(),
                correct = obj.correct.mapNotNull { resolverNum(it)?.toInt() },
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                style = convertirEstilo(obj.style))
            is SectionNode -> FormElement.Section(
                orientation = obj.orientation,
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                pointX = resolverNum(obj.pointX)?.toInt(), pointY = resolverNum(obj.pointY)?.toInt(),
                elements = obj.elements.mapNotNull { convertirNodo(it) },
                style = convertirEstilo(obj.style))
            is TableNode -> FormElement.Table(
                rows = obj.rows.map { row -> row.map { cell -> convertirNodo(cell) } },
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                pointX = resolverNum(obj.pointX)?.toInt(), pointY = resolverNum(obj.pointY)?.toInt(),
                style = convertirEstilo(obj.style))
            is TextNode -> FormElement.TextElement(
                content = obj.content,
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                style = convertirEstilo(obj.style))
            else -> null
        }
    }

    private fun resolverComodin(v: Any?, args: List<Double>, idx: Int): Double? {
        return when {
            v == "?" -> args.getOrNull(idx)
            v is Array<*> && v.size == 2 -> {
                val op = v[0] as? String ?: return null
                val base = resolverNum(v[1]) ?: return null
                val arg  = args.getOrNull(idx) ?: return null
                when (op) {
                    "expr+?" -> base + arg; "expr-?" -> base - arg
                    "expr*?" -> base * arg; "expr/?" -> if (arg != 0.0) base / arg else null
                    "?+expr" -> arg + base; "?-expr" -> arg - base
                    "?*expr" -> arg * base; "?/expr" -> if (base != 0.0) arg / base else null
                    else -> null
                }
            }
            else -> resolverNum(v)
        }
    }

    private fun contarComodines(v: Any?): Int = when {
        v == "?" -> 1
        v is Array<*> && v.size == 2 && (v[0] as? String)?.contains("?") == true -> 1
        else -> 0
    }

    private fun convertirSpecial(node: SpecialNode, args: List<Double>): FormElement? {
        var idx = 0
        val resolvedWidth   = if (contarComodines(node.width)   > 0) resolverComodin(node.width,   args, idx++)?.toInt() else resolverNum(node.width)?.toInt()
        val resolvedHeight  = if (contarComodines(node.height)  > 0) resolverComodin(node.height,  args, idx++)?.toInt() else resolverNum(node.height)?.toInt()
        val resolvedCorrect = if (contarComodines(node.correct) > 0) resolverComodin(node.correct, args, idx++)?.toInt() else resolverNum(node.correct)?.toInt() ?: -1
        val options = node.options.filterIsInstance<String>()
        return when (node.tipo) {
            "OPEN"     -> FormElement.OpenQuestion(label = node.label, width = resolvedWidth, height = resolvedHeight, style = convertirEstilo(node.style))
            "DROP"     -> FormElement.DropQuestion(label = node.label, options = options, correct = resolvedCorrect ?: -1, width = resolvedWidth, height = resolvedHeight, style = convertirEstilo(node.style))
            "SELECT"   -> FormElement.SelectQuestion(options = options, correct = resolvedCorrect ?: -1, width = resolvedWidth, height = resolvedHeight, style = convertirEstilo(node.style))
            "MULTIPLE" -> FormElement.MultipleQuestion(label = "", options = options, correct = emptyList(), width = resolvedWidth, height = resolvedHeight, style = convertirEstilo(node.style))
            else -> null
        }
    }

    // INTERPRETE DE BLOQUES
    private fun interpretarCuerpo(cuerpo: java.util.ArrayList<*>): List<FormElement> {
        val resultado = mutableListOf<FormElement>()
        for (sentencia in cuerpo) {
            when {
                sentencia is Array<*> && sentencia.size == 4 -> {
                    val nombre = sentencia[0] as? String ?: continue
                    val valor  = sentencia[1]
                    val tipo   = sentencia[2] as? String ?: continue
                    when (tipo) {
                        "number" -> {
                            if (!tiposDecl.containsKey(nombre)) tiposDecl[nombre] = "number"
                            evaluarExpr(valor)?.let { simbolos[nombre] = it }
                        }
                        "string" -> {
                            if (!tiposDecl.containsKey(nombre)) tiposDecl[nombre] = "string"
                            if (valor is String && simbolos[valor] == null) simbolos[nombre] = valor
                            else evaluarExpr(valor)?.let { simbolos[nombre] = it.toString() }
                        }
                        "assign" -> {
                            val vr = evaluarExpr(valor)
                            if (vr != null) simbolos[nombre] = vr
                            else if (valor is String && simbolos[valor] == null) simbolos[nombre] = valor
                        }
                    }
                }
                sentencia is IfNode      -> resultado.addAll(interpretarIf(sentencia))
                sentencia is WhileNode   -> resultado.addAll(interpretarWhile(sentencia))
                sentencia is DoWhileNode -> resultado.addAll(interpretarDoWhile(sentencia))
                sentencia is ForNode     -> resultado.addAll(interpretarFor(sentencia))
                sentencia != null        -> convertirNodo(sentencia)?.let { resultado.add(it) }
            }
        }
        return resultado
    }

    private fun interpretarIf(node: IfNode): List<FormElement> {
        for (ramaObj in node.ramas) {
            val rama      = ramaObj as? Array<*> ?: continue
            val condicion = rama[0]
            val cuerpo    = rama[1] as? java.util.ArrayList<*> ?: continue
            if (condicion != null) validarMezclaLogica(condicion, node.linea, erroresLogica)
            if (condicion == null || evaluarCondicion(condicion)) {
                return interpretarCuerpo(cuerpo)
            }
        }
        return emptyList()
    }

    private fun interpretarWhile(node: WhileNode): List<FormElement> {
        val resultado = mutableListOf<FormElement>()
        val cuerpo    = node.cuerpo as? java.util.ArrayList<*> ?: return emptyList()
        validarMezclaLogica(node.condicion, node.linea, erroresLogica)
        var iter = 0
        while (evaluarCondicion(node.condicion)) {
            resultado.addAll(interpretarCuerpo(cuerpo))
            if (++iter > 1000) break
        }
        return resultado
    }

    private fun interpretarDoWhile(node: DoWhileNode): List<FormElement> {
        val resultado = mutableListOf<FormElement>()
        val cuerpo    = node.cuerpo as? java.util.ArrayList<*> ?: return emptyList()
        validarMezclaLogica(node.condicion, node.linea, erroresLogica)
        var iter = 0
        do {
            resultado.addAll(interpretarCuerpo(cuerpo))
            if (++iter > 1000) break
        } while (evaluarCondicion(node.condicion))
        return resultado
    }

    private fun interpretarFor(node: ForNode): List<FormElement> {
        val resultado = mutableListOf<FormElement>()
        val cuerpo    = node.cuerpo as? java.util.ArrayList<*> ?: return emptyList()
        if (node.esRango) {
            val inicio = evaluarExpr(node.inicio)?.toInt() ?: return emptyList()
            val fin    = evaluarExpr(node.fin)?.toInt()    ?: return emptyList()
            if (tiposDecl.containsKey(node.variable) && tiposDecl[node.variable] != "number") {
                erroresLogica.add(ErrorToken(
                    lexeme      = node.variable,
                    line        = node.linea,
                    column      = 0,
                    type        = ErrorType.SEMANTICO,
                    description = "FOR: la variable '${node.variable}' ya fue declarada como '${tiposDecl[node.variable]}', se esperaba number."
                ))
                return emptyList()
            }
            tiposDecl[node.variable] = "number"
            val rango = if (inicio <= fin) inicio..fin else inicio downTo fin
            for (i in rango) {
                simbolos[node.variable] = i.toDouble()
                resultado.addAll(interpretarCuerpo(cuerpo))
            }
        } else {
            if (tiposDecl.containsKey(node.variable) && tiposDecl[node.variable] != "number") {
                erroresLogica.add(ErrorToken(
                    lexeme      = node.variable,
                    line        = node.linea,
                    column      = 0,
                    type        = ErrorType.SEMANTICO,
                    description = "FOR: la variable '${node.variable}' ya fue declarada como '${tiposDecl[node.variable]}', se esperaba number."
                ))
                return emptyList()
            }
            tiposDecl[node.variable] = "number"
            simbolos[node.variable]  = evaluarExpr(node.inicio) ?: return emptyList()
            validarMezclaLogica(node.condicion, node.linea, erroresLogica)
            var iter = 0
            while (evaluarCondicion(node.condicion)) {
                resultado.addAll(interpretarCuerpo(cuerpo))
                val pasoVal = evaluarExpr(node.paso) ?: break
                simbolos[node.varPaso] = pasoVal
                if (++iter > 1000) break
            }
        }
        return resultado
    }

    private fun convertirEstilo(s: StyleNode?): StyleData {
        if (s == null) return StyleData()
        return StyleData(
            color = s.color, backgroundColor = s.backgroundColor,
            fontFamily = s.fontFamily,
            textSize   = resolverNum(s.textSize)?.toFloat()   ?: 0f,
            borderSize = resolverNum(s.borderSize)?.toFloat() ?: 0f,
            borderType = s.borderType, borderColor = s.borderColor
        )
    }
}