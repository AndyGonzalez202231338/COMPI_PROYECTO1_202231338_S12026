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
import com.example.pkmforms.api.PokemonApi

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
                    "group" -> a  // parentesis: desenvuelve el valor
                    else   -> null
                }
            }
            else -> null
        }
    }

    private fun evaluarCondicion(v: Any?): Boolean = (evaluarExpr(v) ?: 0.0) >= 1.0

    /* Detecta si una expresion mezcla || y && al mismo nivel logico.
    * Los parentesis agrupan sub-expresiones y NO cuentan como mezcla:
    *   (a && b) || c  -> valido, el && esta dentro de un grupo
    *   a && b || c    -> invalido, mezcla al mismo nivel
    * El CUP hace los parentesis transparentes (RESULT = v), por lo que
    * necesitamos rastrear si el operador contrario aparece como hijo
    * DIRECTO de la cadena del mismo operador, no dentro de otro operador.
    */
    private fun tieneMezclaLogica(v: Any?): Boolean {
        if (v !is Array<*>) return false
        val op = v[0] as? String ?: return false
        if (op != "op||" && op != "op&&") return false
        return hayContrarioEnCadena(v, op)
    }

    /* Recorre la cadena de nodos con el mismo operador (ej: a && b && c)
    * y verifica si alguno de los operandos directos es el operador contrario.
    * Si un operando es otro tipo de operador (comparacion, aritmetico, negacion)
    * se detiene y NO entra, porque eso seria una sub-expresion independiente.
     */
    private fun hayContrarioEnCadena(v: Any?, opCadena: String): Boolean {
        if (v !is Array<*>) return false
        val op = v[0] as? String ?: return false
        val contrario = if (opCadena == "op||") "op&&" else "op||"

        return when (op) {
            "group" -> false  // parentesis: es una barrera, no entrar
            opCadena -> {
                // Mismo operador: seguir recorriendo la cadena
                val izq = v.getOrNull(1)
                val der = v.getOrNull(2)
                // Si alguno de los hijos directos ES el contrario Y no esta
                // protegido por parentesis, hay mezcla
                val izqEsContrario = (izq is Array<*> && izq.getOrNull(0) == contrario)
                val derEsContrario = (der is Array<*> && der.getOrNull(0) == contrario)
                if (izqEsContrario || derEsContrario) true
                else hayContrarioEnCadena(izq, opCadena) || hayContrarioEnCadena(der, opCadena)
            }
            // Cualquier otro operador (comparacion, aritmetico, negacion)
            // representa una sub-expresion: no entrar
            else -> false
        }
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

    // Convierte un valor a indice entero para 'correct'.
    // Retorna null si el valor es un decimal con parte fraccionaria distinta de cero (error semantico).
    // Si termina en .0 se acepta y trunca (ej: 1.0 -> 1).
    private fun resolverCorrect(v: Any?): Pair<Int?, Boolean> {
        val d = resolverNum(v) ?: return Pair(null, false)
        return if (d % 1.0 != 0.0) {
            Pair(null, true) // true = es error decimal
        } else {
            Pair(d.toInt(), false)
        }
    }

    private fun resolverStr(v: Any?): String? = when (v) {
        is String -> {
            if (v == "__COMODIN__") "__COMODIN__"  // marcador: se reemplaza en convertirSpecial
            else {
                val enSimbolos = simbolos[v]
                when (enSimbolos) {
                    is String -> enSimbolos
                    is Double -> if (enSimbolos % 1.0 == 0.0) enSimbolos.toInt().toString() else enSimbolos.toString()
                    is Number -> enSimbolos.toString()
                    null      -> v  // es un string literal como "Hola"
                    else      -> enSimbolos.toString()
                }
            }
        }
        is Array<*> -> {
            val op = v.getOrNull(0) as? String
            if (op == "group") {
                // parentesis: desenvuelve y resuelve el contenido
                resolverStr(v.getOrNull(1))
            } else {
                val num = evaluarExpr(v)
                if (num != null) {
                    if (num % 1.0 == 0.0) num.toInt().toString() else num.toString()
                } else {
                    if (op == "op+") {
                        val izq = resolverStr(v.getOrNull(1))
                        val der = resolverStr(v.getOrNull(2))
                        if (izq != null && der != null) izq + der else null
                    } else null
                }
            }
        }
        else -> v?.toString()
    }

    // Valida que width, height, pointX, pointY no sean negativos.
    // Retorna true si el valor es invalido (negativo).
    private fun esNegativo(v: Any?): Boolean {
        val d = resolverNum(v) ?: return false
        return d < 0
    }

    private fun validarDimension(v: Any?, nombre: String, etiqueta: String, linea: Int) {
        if (v != null && esNegativo(v)) {
            erroresLogica.add(ErrorToken(
                lexeme      = etiqueta,
                line        = linea,
                column      = 0,
                type        = ErrorType.SEMANTICO,
                description = "$etiqueta: el valor de '$nombre' no puede ser negativo."
            ))
        }
    }

    suspend fun parse(code: String): ParseResult {
        LexerPRO1.errores.clear()
        ParserPRO1.errores.clear()
        ParserPRO1.resultado.clear()
        simbolos.clear()
        tiposDecl.clear()
        speciales.clear()
        advertenciasSelect.clear()
        erroresLogica.clear()

        return try {
            val reader = java.io.StringReader(PkmLexer.normalizarColoresAngulares(code))
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

            /* Pre-validacion: recorrer todos los nodos buscando mezcla logica
             antes de interpretar, para no depender de si el bloque se ejecuta*/
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
                        } else {
                            val totalComodines = totalComodinesSpecial(special)
                            if (args.size != totalComodines) {
                                allErrors.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                    type = ErrorType.SEMANTICO,
                                    description = "draw() de '$nombre' recibio ${args.size} argumento(s) pero la variable tiene $totalComodines comodin(es)."))
                            } else {
                                convertirSpecial(special, args.map { resolverArg(it) })?.let { elementos.add(it) }
                            }
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

    /**
     * Resuelve las opciones de un DropQuestion o SelectQuestion.
     * Si la lista tiene el marcador "__pokemon__", llama a la PokéAPI.
     * De lo contrario, filtra los strings normales.
     */
    private suspend fun resolverOpciones(rawOptions: java.util.ArrayList<*>): List<String> {
        return resolverOpcionesConArgs(rawOptions, emptyList(), 0).first
    }

    private suspend fun resolverOpcionesConArgs(
        rawOptions: java.util.ArrayList<*>,
        args: List<Any?>,
        startIdx: Int
    ): Pair<List<String>, Int> {
        if (rawOptions.firstOrNull() == "__pokemon__") {
            var idx = startIdx
            val minRaw = rawOptions.getOrNull(1)
            val maxRaw = rawOptions.getOrNull(2)
            val min = if (minRaw == "__COMODIN__") { val v = (args.getOrNull(idx) as? Double)?.toInt() ?: 1; idx++; v }
            else (minRaw as? Double)?.toInt() ?: 1
            val max = if (maxRaw == "__COMODIN__") { val v = (args.getOrNull(idx) as? Double)?.toInt() ?: 10; idx++; v }
            else (maxRaw as? Double)?.toInt() ?: 10
            return Pair(PokemonApi.obtenerPokemones(min, max), idx)
        }
        // Iterar opciones resolviendo cada item (puede ser String, COMODIN, expresion)
        var idx = startIdx
        val opciones = mutableListOf<String>()
        for (item in rawOptions) {
            val comodinesEnItem = contarComodines(item)
            if (comodinesEnItem > 0) {
                val (valor, nuevoIdx) = resolverExprConArgs(item, args, idx)
                opciones.add(valor ?: "")
                idx = nuevoIdx
            } else {
                val str = resolverStr(item) ?: resolverNum(item)?.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                } ?: item?.toString() ?: ""
                if (str.isNotEmpty()) opciones.add(str)
            }
        }
        return Pair(opciones, idx)
    }

    private suspend fun convertirNodoODraw(obj: Any?): FormElement? {
        if (obj is Array<*> && obj.size >= 3 && obj[0] == "draw") {
            val nombre  = obj[1] as? String ?: return null
            val args    = obj[2] as? java.util.ArrayList<*> ?: java.util.ArrayList<Any>()
            val special = speciales[nombre] ?: return null
            val totalComodines = totalComodinesSpecial(special)
            // Si el conteo no coincide, intentar de todas formas con los args disponibles
            // para no silenciar el elemento por diferencias de conteo
            if (args.size < totalComodines) {
                erroresLogica.add(ErrorToken(lexeme = nombre, line = 0, column = 0,
                    type = ErrorType.SEMANTICO,
                    description = "draw() de '$nombre' recibio ${args.size} argumento(s) pero la variable tiene $totalComodines comodin(es)."))
                return null
            }
            return convertirSpecial(special, args.map { resolverArg(it) })
        }
        return convertirNodo(obj)
    }

    private suspend fun convertirNodo(obj: Any?): FormElement? {
        return when (obj) {
            is OpenQuestionNode -> FormElement.OpenQuestion(
                label = resolverStr(obj.label) ?: obj.label?.toString() ?: "",
                width = resolverNum(obj.width)?.toInt(), height = resolverNum(obj.height)?.toInt(),
                style = convertirEstilo(obj.style))
            is DropQuestionNode -> {
                val rawOpts  = obj.options
                val esPokemon = rawOpts.firstOrNull() == "__pokemon__"
                val opciones = resolverOpciones(rawOpts)
                val (correctVal, esDecimalInvalido) = if (obj.correctVal != null) resolverCorrect(obj.correctVal) else Pair(-1, false)
                val correct = correctVal ?: -1
                if (obj.correctVal != null && esDecimalInvalido) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "DROP_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "DROP_QUESTION: el valor de correct debe ser un entero. Use un numero sin decimales (ej: 1 en lugar de 1.5)."
                    ))
                }
                if (obj.correctVal != null && !esDecimalInvalido && opciones.isNotEmpty() && (correct < 0 || correct >= opciones.size)) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "DROP_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "DROP_QUESTION: correct=$correct esta fuera del rango de opciones (0..${opciones.size - 1})."
                    ))
                }
                FormElement.DropQuestion(
                    label   = resolverStr(obj.label) ?: obj.label?.toString() ?: "",
                    options = opciones,
                    correct = correct,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            is SelectQuestionNode -> {
                val opciones = obj.options.filterIsInstance<String>()
                val (correctVal, esDecimalInvalido) = if (obj.correctVal != null) resolverCorrect(obj.correctVal) else Pair(-1, false)
                val correct = correctVal ?: -1
                if (opciones.size > 5) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "SELECT_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.ADVERTENCIA,
                        description = "SELECT_QUESTION tiene ${opciones.size} opciones (mas de 5). Se agregara al formulario de todas formas."
                    ))
                }
                if (obj.correctVal != null && esDecimalInvalido) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "SELECT_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "SELECT_QUESTION: el valor de correct debe ser un entero. Use un numero sin decimales (ej: 1 en lugar de 1.5)."
                    ))
                }
                if (obj.correctVal != null && !esDecimalInvalido && opciones.isNotEmpty() && (correct < 0 || correct >= opciones.size)) {
                    advertenciasSelect.add(ErrorToken(
                        lexeme      = "SELECT_QUESTION",
                        line        = obj.linea,
                        column      = 0,
                        type        = ErrorType.SEMANTICO,
                        description = "SELECT_QUESTION: correct=$correct esta fuera del rango de opciones (0..${opciones.size - 1})."
                    ))
                }
                FormElement.SelectQuestion(
                    label   = resolverStr(obj.label) ?: obj.label?.toString() ?: "",
                    options = opciones,
                    correct = correct,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            is MultipleQuestionNode -> {
                val opciones = obj.options.filterIsInstance<String>()
                val correctList = mutableListOf<Int>()
                for (item in obj.correct) {
                    val (cv, esDecimal) = resolverCorrect(item)
                    if (esDecimal) {
                        advertenciasSelect.add(ErrorToken(
                            lexeme      = "MULTIPLE_QUESTION",
                            line        = obj.linea,
                            column      = 0,
                            type        = ErrorType.SEMANTICO,
                            description = "MULTIPLE_QUESTION: el valor de correct debe ser un entero. Use un numero sin decimales."
                        ))
                    } else if (cv != null) {
                        if (opciones.isNotEmpty() && (cv < 0 || cv >= opciones.size)) {
                            advertenciasSelect.add(ErrorToken(
                                lexeme      = "MULTIPLE_QUESTION",
                                line        = obj.linea,
                                column      = 0,
                                type        = ErrorType.SEMANTICO,
                                description = "MULTIPLE_QUESTION: correct=$cv esta fuera del rango de opciones (0..${opciones.size - 1})."
                            ))
                        } else {
                            correctList.add(cv)
                        }
                    }
                }
                FormElement.MultipleQuestion(
                    label   = resolverStr(obj.label) ?: obj.label?.toString() ?: "",
                    options = opciones,
                    correct = correctList,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            is SectionNode -> {
                validarDimension(obj.width,  "width",  "SECTION", obj.linea)
                validarDimension(obj.height, "height", "SECTION", obj.linea)
                validarDimension(obj.pointX, "pointX", "SECTION", obj.linea)
                validarDimension(obj.pointY, "pointY", "SECTION", obj.linea)
                FormElement.Section(
                    orientation = obj.orientation,
                    width  = resolverNum(obj.width)?.toInt(),
                    height = resolverNum(obj.height)?.toInt(),
                    pointX = resolverNum(obj.pointX)?.toInt(),
                    pointY = resolverNum(obj.pointY)?.toInt(),
                    elements = obj.elements.mapNotNull { convertirNodoODraw(it) },
                    style = convertirEstilo(obj.style))
            }
            is TableNode -> {
                validarDimension(obj.width,  "width",  "TABLE", obj.linea)
                validarDimension(obj.height, "height", "TABLE", obj.linea)
                validarDimension(obj.pointX, "pointX", "TABLE", obj.linea)
                validarDimension(obj.pointY, "pointY", "TABLE", obj.linea)
                FormElement.Table(
                    rows = obj.rows.map { row -> row.mapNotNull { cell -> convertirNodoODraw(cell) } },
                    width  = resolverNum(obj.width)?.toInt(),
                    height = resolverNum(obj.height)?.toInt(),
                    pointX = resolverNum(obj.pointX)?.toInt(),
                    pointY = resolverNum(obj.pointY)?.toInt(),
                    style  = convertirEstilo(obj.style))
            }
            is TextNode -> {
                validarDimension(obj.width,  "width",  "TEXT", obj.linea)
                validarDimension(obj.height, "height", "TEXT", obj.linea)
                FormElement.TextElement(
                    content = resolverStr(obj.contentExpr) ?: obj.content,
                    width   = resolverNum(obj.width)?.toInt(),
                    height  = resolverNum(obj.height)?.toInt(),
                    style   = convertirEstilo(obj.style))
            }
            else -> null
        }
    }

    private fun resolverComodin(v: Any?, args: List<Any?>, idx: Int): Double? {
        return when {
            v == "__COMODIN__" -> (args.getOrNull(idx) as? Double) ?: resolverNum(args.getOrNull(idx))
            v is Array<*> && v.size == 2 -> {
                val op = v[0] as? String ?: return null
                val base = resolverNum(v[1]) ?: return null
                val arg  = (args.getOrNull(idx) as? Double) ?: resolverNum(args.getOrNull(idx)) ?: return null
                when (op) {
                    "__expr+?__" -> base + arg; "__expr-?__" -> base - arg
                    "__expr*?__" -> base * arg; "__expr/?__" -> if (arg != 0.0) base / arg else null
                    "__?+expr__" -> arg + base; "__?-expr__" -> arg - base
                    "__?*expr__" -> arg * base; "__?/expr__" -> if (base != 0.0) arg / base else null
                    else -> null
                }
            }
            else -> resolverNum(v)
        }
    }

    private fun contarComodines(v: Any?): Int = when {
        v == "__COMODIN__" -> 1
        v is Array<*> -> {
            val op = v.getOrNull(0) as? String ?: ""
            // Los operadores con comodin embebido como __expr+?__ ya tienen el ? implícito
            if (op.startsWith("__") && op.contains("?") && op.endsWith("__")) 1
            else v.drop(1).sumOf { contarComodines(it) }
        }
        else -> 0
    }

    private fun totalComodinesSpecial(special: com.example.pkmforms.analyzer.generated.SpecialNode): Int {
        return special.comodines +
                contarComodinesEstilo(special.style) +
                contarComodinesOpciones(special.options)
    }

    // Convierte Object[] de Java a List para poder procesarlo en Kotlin
    private fun javaArrayToList(v: Any?): List<Any?>? {
        if (v == null) return null
        if (!v.javaClass.isArray) return null
        val len = java.lang.reflect.Array.getLength(v)
        return (0 until len).map { java.lang.reflect.Array.get(v, it) }
    }

    private fun contarComodinesObj(v: Any?): Int {
        if (v == null) return 0
        if (v == "__COMODIN__") return 1
        if (v is String) return v.count { it == '?' }
        // Array de Kotlin
        if (v is Array<*>) {
            val op = v.getOrNull(0) as? String ?: ""
            if (op.startsWith("__") && op.contains("?") && op.endsWith("__")) return 1
            return v.drop(1).sumOf { contarComodinesObj(it) }
        }
        // Object[] de Java — usar reflexion
        if (v.javaClass.isArray) {
            val lista = javaArrayToList(v) ?: return 0
            val op = lista.getOrNull(0) as? String ?: ""
            if (op.startsWith("__") && op.contains("?") && op.endsWith("__")) return 1
            return lista.drop(1).sumOf { contarComodinesObj(it) }
        }
        return 0
    }

    private fun contarComodinesEstilo(s: StyleNode?): Int {
        if (s == null) return 0
        return contarComodinesColor(s.color) +
                contarComodinesColor(s.backgroundColor) +
                contarComodinesColor(s.borderColor) +
                contarComodinesObj(s.textSize) +
                contarComodinesObj(s.borderSize)
    }


    private fun contarComodinesOpciones(rawOptions: java.util.ArrayList<*>): Int {
        if (rawOptions.firstOrNull() == "__pokemon__") {
            var count = 0
            if (rawOptions.getOrNull(1) == "__COMODIN__") count++
            if (rawOptions.getOrNull(2) == "__COMODIN__") count++
            return count
        }
        // Contar comodines en listas normales de opciones
        return rawOptions.sumOf { contarComodines(it) }
    }

    private fun resolverArg(v: Any?): Any? {
        val num = resolverNum(v)
        if (num != null) return num
        return resolverStr(v)
    }

    // Resuelve una expresion que puede contener comodines usando args
    private fun resolverExprConArgs(v: Any?, args: List<Any?>, startIdx: Int): Pair<String?, Int> {
        var idx = startIdx
        fun resolverNum2(expr: Any?): Double? {
            return when {
                expr == "__COMODIN__" -> {
                    val d = (args.getOrNull(idx) as? Double) ?: resolverNum(args.getOrNull(idx))
                    idx++
                    d
                }
                expr is Array<*> -> {
                    val op = expr.getOrNull(0) as? String ?: return null
                    when {
                        op.startsWith("__") && op.contains("?") -> {
                            val base = resolverNum(expr.getOrNull(1)) ?: 0.0
                            val arg = (args.getOrNull(idx) as? Double) ?: resolverNum(args.getOrNull(idx)) ?: 0.0
                            idx++
                            when (op) {
                                "__expr+?__" -> base + arg; "__expr-?__" -> base - arg
                                "__expr*?__" -> base * arg; "__expr/?__" -> if (arg != 0.0) base / arg else null
                                "__?+expr__" -> arg + base; "__?-expr__" -> arg - base
                                "__?*expr__" -> arg * base; "__?/expr__" -> if (base != 0.0) arg / base else null
                                else -> null
                            }
                        }
                        op == "op+" -> { val a = resolverNum2(expr.getOrNull(1)); val b = resolverNum2(expr.getOrNull(2)); if (a != null && b != null) a + b else null }
                        op == "op-" -> { val a = resolverNum2(expr.getOrNull(1)); val b = resolverNum2(expr.getOrNull(2)); if (a != null && b != null) a - b else null }
                        op == "op*" -> { val a = resolverNum2(expr.getOrNull(1)); val b = resolverNum2(expr.getOrNull(2)); if (a != null && b != null) a * b else null }
                        op == "op/" -> { val a = resolverNum2(expr.getOrNull(1)); val b = resolverNum2(expr.getOrNull(2)); if (a != null && b != null && b != 0.0) a / b else null }
                        op == "op^" -> { val a = resolverNum2(expr.getOrNull(1)); val b = resolverNum2(expr.getOrNull(2)); if (a != null && b != null) Math.pow(a, b) else null }
                        op == "group" -> resolverNum2(expr.getOrNull(1))
                        else -> resolverNum(expr)
                    }
                }
                else -> resolverNum(expr)
            }
        }
        fun resolver(expr: Any?): String? {
            return when {
                expr == "__COMODIN__" -> {
                    val r = formatearArg(args.getOrNull(idx)); idx++; r
                }
                expr is String -> resolverStr(expr)
                expr is Array<*> -> {
                    val op = expr.getOrNull(0) as? String ?: return null
                    when (op) {
                        "op+" -> {
                            val left = expr.getOrNull(1)
                            val right = expr.getOrNull(2)
                            // Si alguno de los operandos es string, concatenar directamente
                            // sin intentar resolver como numero (evita consumir idx prematuramente)
                            val leftIsString = left is String && left != "__COMODIN__"
                            val rightIsString = right is String && right != "__COMODIN__"
                            if (leftIsString || rightIsString) {
                                val izq = resolver(left)
                                val der = resolver(right)
                                if (izq != null && der != null) izq + der else izq ?: der
                            } else {
                                // Guardar idx antes de intentar como numero
                                val savedIdx = idx
                                val numVal = resolverNum2(expr)
                                if (numVal != null) formatearArg(numVal)
                                else {
                                    // Restaurar idx y hacer concatenacion string
                                    idx = savedIdx
                                    val izq = resolver(left)
                                    val der = resolver(right)
                                    if (izq != null && der != null) izq + der else izq ?: der
                                }
                            }
                        }
                        "group" -> resolver(expr.getOrNull(1))
                        else -> {
                            val numVal = resolverNum2(expr)
                            if (numVal != null) formatearArg(numVal)
                            else resolverStr(expr)
                        }
                    }
                }
                expr is Number -> formatearArg(expr.toDouble())
                else -> expr?.toString()
            }
        }
        val resultado = resolver(v)
        return Pair(resultado, idx)
    }

    // Formatea un argumento de draw para mostrar en label u opcion
    private fun formatearArg(rawArg: Any?): String = when {
        rawArg is Double && rawArg % 1.0 == 0.0 -> rawArg.toInt().toString()
        rawArg is Double -> rawArg.toString()
        rawArg is String -> rawArg
        rawArg != null -> {
            val d = resolverNum(rawArg)
            if (d != null && d % 1.0 == 0.0) d.toInt().toString()
            else rawArg.toString()
        }
        else -> ""
    }

    private suspend fun convertirSpecial(node: SpecialNode, args: List<Any?>): FormElement? {
        var idx = 0
        // El orden de consumo de args sigue el orden de declaracion en el special:
        // width -> height -> opciones -> correct -> label -> estilo
        val widthComodines = contarComodines(node.width)
        val resolvedWidth = if (widthComodines > 0) {
            val v = resolverComodin(node.width, args, idx)?.toInt()
            idx += widthComodines
            v
        } else resolverNum(node.width)?.toInt()

        val heightComodines = contarComodines(node.height)
        val resolvedHeight = if (heightComodines > 0) {
            val v = resolverComodin(node.height, args, idx)?.toInt()
            idx += heightComodines
            v
        } else resolverNum(node.height)?.toInt()

        val (resolvedOpciones, idxDespuesOpciones) = resolverOpcionesConArgs(node.options, args, idx)
        idx = idxDespuesOpciones

        val correctComodines = contarComodines(node.correct)
        val resolvedCorrect = if (correctComodines > 0) {
            val v = resolverComodin(node.correct, args, idx)?.toInt()
            idx += correctComodines
            v ?: -1
        } else resolverNum(node.correct)?.toInt() ?: -1

        val resolvedCorrectList: List<Int> = if (node.correct is java.util.ArrayList<*>) {
            (node.correct as java.util.ArrayList<*>).mapNotNull { item ->
                if (item == "__COMODIN__") {
                    val v = resolverComodin("__COMODIN__", args, idx)?.toInt()
                    idx++
                    v
                } else resolverNum(item)?.toInt()
            }
        } else emptyList()

        val labelComodines = contarComodines(node.label)
        val resolvedLabel = if (labelComodines > 0) {
            val (labelStr, newIdx) = resolverExprConArgs(node.label, args, idx)
            idx = newIdx
            labelStr ?: node.label?.toString() ?: ""
        } else resolverStr(node.label) ?: node.label?.toString() ?: ""

        val (resolvedStyle, _) = convertirEstiloConComodines(node.style, args, idx)
        return when (node.tipo) {
            "OPEN"     -> FormElement.OpenQuestion(label = resolvedLabel, width = resolvedWidth, height = resolvedHeight, style = resolvedStyle)
            "DROP"     -> FormElement.DropQuestion(label = resolvedLabel, options = resolvedOpciones, correct = resolvedCorrect ?: -1, width = resolvedWidth, height = resolvedHeight, style = resolvedStyle)
            "SELECT"   -> FormElement.SelectQuestion(label = resolvedLabel, options = resolvedOpciones, correct = resolvedCorrect ?: -1, width = resolvedWidth, height = resolvedHeight, style = resolvedStyle)
            "MULTIPLE" -> FormElement.MultipleQuestion(label = resolvedLabel, options = resolvedOpciones, correct = resolvedCorrectList, width = resolvedWidth, height = resolvedHeight, style = resolvedStyle)
            else -> null
        }
    }

    // INTERPRETE DE BLOQUES
    private suspend fun interpretarCuerpo(cuerpo: java.util.ArrayList<*>): List<FormElement> {
        val resultado = mutableListOf<FormElement>()
        for (sentencia in cuerpo) {
            when {
                sentencia is Array<*> && sentencia.size >= 3 && sentencia[0] == "draw" -> {
                    val nombre = sentencia[1] as? String ?: continue
                    val args   = sentencia[2] as? java.util.ArrayList<*> ?: java.util.ArrayList<Any>()
                    val linea  = (sentencia.getOrNull(3) as? Int) ?: 0
                    val special = speciales[nombre]
                    if (special == null) {
                        erroresLogica.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                            type = ErrorType.SEMANTICO,
                            description = "Variable special '$nombre' no ha sido declarada."))
                    } else {
                        val totalComodines = totalComodinesSpecial(special)
                        if (args.size != totalComodines) {
                            erroresLogica.add(ErrorToken(lexeme = nombre, line = linea, column = 0,
                                type = ErrorType.SEMANTICO,
                                description = "draw() de '$nombre' recibio ${args.size} argumento(s) pero la variable tiene $totalComodines comodin(es)."))
                        } else {
                            convertirSpecial(special, args.map { resolverArg(it) })?.let { resultado.add(it) }
                        }
                    }
                }
                sentencia is IfNode      -> resultado.addAll(interpretarIf(sentencia))
                sentencia is WhileNode   -> resultado.addAll(interpretarWhile(sentencia))
                sentencia is DoWhileNode -> resultado.addAll(interpretarDoWhile(sentencia))
                sentencia is ForNode     -> resultado.addAll(interpretarFor(sentencia))
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
                sentencia != null        -> convertirNodoODraw(sentencia)?.let { resultado.add(it) }
            }
        }
        return resultado
    }

    private suspend fun interpretarIf(node: IfNode): List<FormElement> {
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

    private suspend fun interpretarWhile(node: WhileNode): List<FormElement> {
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

    private suspend fun interpretarDoWhile(node: DoWhileNode): List<FormElement> {
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

    private suspend fun interpretarFor(node: ForNode): List<FormElement> {
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

    private fun resolverColor(v: Any?, args: List<Any?> = emptyList(), startIdx: Int = 0): String {
        return resolverColorConArgs(v, args, startIdx).first
    }

    // Evalua un canal de color serializado como "50", "?", "50+?", "?*5", "colorBase+(?*5)" etc
    private fun evaluarCanalColor(canal: String, args: List<Any?>, idx: Int): Pair<Int, Int> {
        var i = idx
        if (canal == "?") {
            val d = (args.getOrNull(i) as? Double) ?: resolverNum(args.getOrNull(i)) ?: 0.0
            i++
            return Pair(d.toInt().coerceIn(0, 255), i)
        }
        if (!canal.contains("?")) {
            // Sin comodin: intentar como numero o como expresion con variables
            val num = canal.toDoubleOrNull() ?: run {
                // Puede ser una variable como "colorBase"
                val v = simbolos[canal]
                if (v is Double) v else null
            }
            return Pair(num?.toInt()?.coerceIn(0, 255) ?: 0, i)
        }
        // Canal con expresion: "50+?", "?*5", "colorBase+(?*5)"
        // Reemplazar ? con el valor del arg
        val arg = (args.getOrNull(i) as? Double) ?: resolverNum(args.getOrNull(i)) ?: 0.0
        i++
        // Sustituir ? por el valor numerico y evaluar
        val expr = canal.replace("?", arg.toInt().toString())
        // Evaluacion simple de expresiones aritmeticas basicas
        val resultado = evaluarExprString(expr)
        return Pair(resultado?.toInt()?.coerceIn(0, 255) ?: 0, i)
    }

    // Evalua una expresion aritmetica simple como string "50+10", "200-5", "10*3"
    private fun evaluarExprString(expr: String): Double? {
        return try {
            // Intentar como numero directo primero
            expr.toDoubleOrNull() ?: run {
                // Buscar operadores de izquierda a derecha
                val e = expr.trim()
                // Suma
                val plusIdx = e.lastIndexOf('+')
                if (plusIdx > 0) {
                    val left = evaluarExprString(e.substring(0, plusIdx))
                    val right = evaluarExprString(e.substring(plusIdx + 1))
                    if (left != null && right != null) return@run left + right
                }
                // Resta (no al inicio que seria negativo)
                val minusIdx = e.lastIndexOf('-')
                if (minusIdx > 0) {
                    val left = evaluarExprString(e.substring(0, minusIdx))
                    val right = evaluarExprString(e.substring(minusIdx + 1))
                    if (left != null && right != null) return@run left - right
                }
                // Multiplicacion
                val multIdx = e.lastIndexOf('*')
                if (multIdx > 0) {
                    val left = evaluarExprString(e.substring(0, multIdx))
                    val right = evaluarExprString(e.substring(multIdx + 1))
                    if (left != null && right != null) return@run left * right
                }
                // Variable
                val varVal = simbolos[e]
                if (varVal is Double) varVal else null
            }
        } catch (ex: Exception) { null }
    }

    private fun resolverColorConArgs(v: Any?, args: List<Any?>, startIdx: Int): Pair<String, Int> {
        var idx = startIdx
        if (v !is String) return Pair("", idx)

        if (v.startsWith("rgb_expr:")) {
            val partes = v.removePrefix("rgb_expr:").split(":")
            val canales = partes.map { canal ->
                val (valor, newIdx) = evaluarCanalColor(canal, args, idx)
                idx = newIdx
                valor
            }
            val r = canales.getOrElse(0) { 0 }
            val g = canales.getOrElse(1) { 0 }
            val b = canales.getOrElse(2) { 0 }
            return Pair("($r,$g,$b)", idx)
        }

        if (v.startsWith("hsl_expr:")) {
            val partes = v.removePrefix("hsl_expr:").split(":")
            val canales = partes.map { canal ->
                val (valor, newIdx) = evaluarCanalColor(canal, args, idx)
                idx = newIdx
                valor
            }
            val h = canales.getOrElse(0) { 0 }
            val s = canales.getOrElse(1) { 0 }
            val l = canales.getOrElse(2) { 0 }
            return Pair("<$h,$s,$l>", idx)
        }

        if (v.contains("?")) {
            return resolverColorComodin(v, args, idx)
        }

        return Pair(v, idx)
    }

    private fun convertirEstilo(s: StyleNode?): StyleData {
        if (s == null) return StyleData()
        return StyleData(
            color = resolverColor(s.color), backgroundColor = resolverColor(s.backgroundColor),
            fontFamily = s.fontFamily,
            textSize   = resolverNum(s.textSize)?.toFloat()   ?: 0f,
            borderSize = resolverNum(s.borderSize)?.toFloat() ?: 0f,
            borderType = s.borderType, borderColor = resolverColor(s.borderColor)
        )
    }

    // Resuelve los comodines ? en un color string usando los argumentos del draw
    // Ejemplo: "(?,100,255)" con args=[200] -> "(200,100,255)"
    private fun resolverColorComodin(color: String, args: List<Any?>, idx: Int): Pair<String, Int> {
        var i = idx
        val resultado = color.replace(Regex("[?]")) {
            val d = args.getOrNull(i) as? Double ?: resolverNum(args.getOrNull(i))
            val valor = d?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: args.getOrNull(i)?.toString() ?: "0"
            i++
            valor
        }
        return Pair(resultado, i)
    }

    // Cuenta cuantos ? hay en un color
    private fun contarComodinesColor(color: Any?): Int {
        if (color !is String) return 0
        return color.count { it == '?' }
    }

    private fun convertirEstiloConComodines(s: StyleNode?, args: List<Any?>, startIdx: Int): Pair<StyleData, Int> {
        if (s == null) return Pair(StyleData(), startIdx)
        var idx = startIdx
        val (resolvedColor, idx2) = if (contarComodinesColor(s.color) > 0)
            resolverColorConArgs(s.color, args, idx) else Pair(resolverColor(s.color), idx)
        idx = idx2
        val (resolvedBg, idx3) = if (contarComodinesColor(s.backgroundColor) > 0)
            resolverColorConArgs(s.backgroundColor, args, idx) else Pair(resolverColor(s.backgroundColor), idx)
        idx = idx3
        val (resolvedBorderColor, idx4) = if (contarComodinesColor(s.borderColor) > 0)
            resolverColorConArgs(s.borderColor, args, idx) else Pair(resolverColor(s.borderColor), idx)
        idx = idx4
        return Pair(StyleData(
            color = resolvedColor,
            backgroundColor = resolvedBg,
            fontFamily = s.fontFamily,
            textSize   = resolverNum(s.textSize)?.toFloat()   ?: 0f,
            borderSize = resolverNum(s.borderSize)?.toFloat() ?: 0f,
            borderType = s.borderType,
            borderColor = resolvedBorderColor
        ), idx)
    }
}