/*
 * PKM_FORMS - Analizador Lexico
 * Organizacion de Lenguajes y Compiladores 1
 * Primer Semestre 2026
 *
 */

import java_cup.runtime.*;
import java.util.ArrayList;

%%

%class  LexerPRO1
%public                 /* clase publica, sin main() propio                     */
%unicode                /* soporte de caracteres unicode                         */
%cup                    /* genera next_token() compatible con CUP                */
%line                   /* habilita yyline (base 0) para reporte de errores      */
%column                 /* habilita yycolumn (base 0) para reporte de errores    */

%{
    /*
     * Lista estatica para acumular errores lexicos durante el analisis.
     * Cada entrada es: { lexema, linea, columna, tipo, descripcion }
     * Al terminar el analisis se puede recorrer para mostrarsela al usuario.
     */
    public static ArrayList<String[]> errores = new ArrayList<>();

    /* Crea un token SIN valor semantico */
    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    /* Crea un token CON valor semantico (numero, string, etc.) */
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

    /* Registra un error lexico; el scanner sigue corriendo para reportar mas errores */
    private void registrarError(String lexema) {
        errores.add(new String[]{
            lexema,
            String.valueOf(yyline + 1),
            String.valueOf(yycolumn + 1),
            "Lexico",
            "Simbolo no reconocido en el lenguaje: '" + lexema + "'"
        });
    }

    /*
     * Convierte las notaciones de emoji @[...] dentro de una cadena
     * a sus caracteres Unicode correspondientes.
     * Se llama despues de extraer el contenido de la cadena (sin comillas).
     */
    private String resolverEmojis(String texto) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < texto.length()) {
            // Buscar inicio de notacion emoji @[
            if (i + 1 < texto.length() && texto.charAt(i) == '@' && texto.charAt(i+1) == '[') {
                int fin = texto.indexOf(']', i + 2);
                if (fin == -1) {
                    // No hay cierre, agregar tal cual
                    sb.append(texto.charAt(i));
                    i++;
                    continue;
                }
                String contenido = texto.substring(i + 2, fin); // lo de adentro de @[...]
                String emoji = convertirEmoji(contenido);
                if (emoji != null) {
                    sb.append(emoji);
                } else {
                    // No reconocido, dejar tal cual
                    sb.append(texto, i, fin + 1);
                }
                i = fin + 1;
            } else {
                sb.append(texto.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /*
     * Convierte el contenido entre @[ y ] al emoji Unicode.
     * Retorna null si no reconoce el patron.
     */
    private String convertirEmoji(String contenido) {
        // Nombres directos
        if (contenido.equals(":smile:"))   return "\uD83D\uDE00"; // feliz
        if (contenido.equals(":sad:"))     return "\uD83E\uDD72"; // triste
        if (contenido.equals(":serious:")) return "\uD83D\uDE10"; // serio
        if (contenido.equals(":heart:"))   return "\u2764\uFE0F"; // corazon
        if (contenido.equals(":star:"))    return "\u2B50";        // estrella
        if (contenido.equals(":cat:") || contenido.equals(":^^:")) return "\uD83D\uDE3A"; // gato

        // :) :)) :))))) — dos puntos + uno o mas parentesis abre
        if (contenido.matches(":\\)+"))    return "\uD83D\uDE00"; // feliz
        // :( :(( — dos puntos + uno o mas parentesis cierra
        if (contenido.matches(":\\(+"))    return "\uD83E\uDD72"; // triste
        // :| :|| — dos puntos + uno o mas pipes
        if (contenido.matches(":\\|+"))    return "\uD83D\uDE10"; // serio

        // Corazon: <3, <<3, <<<33, <<<<33333 — uno o mas < seguido de uno o mas 3
        if (contenido.matches("<+3+"))          return "\u2764\uFE0F"; 
        // Formato largo: :<<<<33333: — : + uno o mas < + uno o mas 3 + : opcional
        if (contenido.matches(":<+3+:?"))       return "\u2764\uFE0F"; 

        // :star:N: o :star-N: — N estrellas
        java.util.regex.Matcher mColon = java.util.regex.Pattern
            .compile(":star:(\\d+):?").matcher(contenido);
        if (mColon.matches()) {
            int n = Math.min(Integer.parseInt(mColon.group(1)), 50);
            StringBuilder stars = new StringBuilder();
            for (int k = 0; k < n; k++) stars.append("\u2B50");
            return stars.toString();
        }
        java.util.regex.Matcher mGuion = java.util.regex.Pattern
            .compile(":star-(\\d+):?").matcher(contenido);
        if (mGuion.matches()) {
            int n = Math.min(Integer.parseInt(mGuion.group(1)), 50);
            StringBuilder stars = new StringBuilder();
            for (int k = 0; k < n; k++) stars.append("\u2B50");
            return stars.toString();
        }

        return null; // no reconocido
    }
%}

/*====== MACROS ====== */

/* --- Basicos --- */
letra           = [a-zA-Z_]
digito          = [0-9]
espacio = [ \t\r\n\u00A0\u200B\u200C\u200D\uFEFF]+
sp      = [ \t]*

/* --- Identificadores --- */
identificador   = {letra}({letra}|{digito})*

/* --- Numericos --- */
entero          = {digito}+
decimal         = {digito}+"."{digito}+

/* --- Cadenas --- */
cadena          = \"([^\"\n])*\"

/* --- Colores ---
   rgb: parentesis con tres numeros separados por coma
   hsl: <> con tres numeros separados por coma                      */
hex_color       = "#"[A-Fa-f0-9]{6}
valor_color     = ({digito}+|[?])
rgb_color       = "("{valor_color}(","{valor_color}){2}")"
hsl_color       = "<"{sp}{valor_color}({sp}","{sp}{valor_color}){2}{sp}">"

/* --- Comentarios ---
   Linea: desde $ hasta fin de linea ($ no es operador en este lenguaje).
   Bloque: la tecnica ([^*]|"*"[^/])* evita cerrar antes de tiempo
   ante secuencias como  **** /  o  /* texto * texto */              */
comentario_linea    = "$"[^\n]*
comentario_bloque   = "/*"([^*]|"*"[^/])*"*/"

/*
   NOTA SOBRE EMOJIS:
   Los emojis SOLO son validos dentro de cadenas de texto (entre
   comillas). NO son tokens independientes del lenguaje.

   La regla {cadena} los absorbe como texto plano junto con el
   resto del contenido de la cadena. Una fase posterior
   (interpretacion/render del formulario) se encarga de expandir
   la notacion @[...] al caracter Unicode correspondiente.

   Consecuencia: si el usuario escribe  @[:smile:]  fuera de una
   cadena, el @ caera en el catch-all como error lexico.

   Ejemplo correcto:    label: "Nombre @[:smile:]"
   Ejemplo incorrecto:  label: @[:smile:] '@' genera error lexico
*/

%%

/* ============================================================
   REGLAS LEXICAS

   Orden de prioridad en JFlex:
     1. Si dos reglas coinciden con el mismo texto, gana la MAS LARGA.
     2. Si tienen la misma longitud, gana la que aparece PRIMERO.

   Orden en este archivo:
     1. Comentarios  ($ no debe confundirse con ningun otro patron)
     2. Espacios     (se descartan)
     3. Palabras reservadas  (antes que identificadores)
     4. Colores
     5. Literales numericos  (decimal antes que entero)
     6. Cadenas              (absorbe emojis como texto plano)
     7. Identificadores
     8. Operadores de 2 chars  (antes que los de 1 char)
     9. Operadores de 1 char
    10. Delimitadores
    11. EOF
    12. Catch-all para errores lexicos
   ============================================================ */

/* ---- 1. Comentarios ---- */
{comentario_linea}          { /* ignorar comentario de linea  */ }
{comentario_bloque}         { /* ignorar comentario de bloque */ }

/* ---- 2. Espacios ---- */
{espacio}                   { /* ignorar */ }

/* ---- 3. PALABRAS RESERVADAS ----
   Case-sensitive "number" es tipo, "NUMBER"*/

/* -- Tipos de variables -- */
"number"                    { return symbol(sym.NUMBER_TYPE); }
"string"                    { return symbol(sym.STRING_TYPE); }
"special"                   { return symbol(sym.SPECIAL_TYPE); }

/* -- Elementos del formulario -- */
"SECTION"                   { return symbol(sym.SECTION); }
"TABLE"                     { return symbol(sym.TABLE); }
"TEXT"                      { return symbol(sym.TEXT); }
"OPEN_QUESTION"             { return symbol(sym.OPEN_QUESTION); }
"DROP_QUESTION"             { return symbol(sym.DROP_QUESTION); }
"SELECT_QUESTION"           { return symbol(sym.SELECT_QUESTION); }
"MULTIPLE_QUESTION"         { return symbol(sym.MULTIPLE_QUESTION); }

/* -- Atributos de elementos --
   Se tokeniza cada atributo por separado para que CUP pueda
   validar que no se repitan y que todos los obligatorios existan. */
"width"                     { return symbol(sym.WIDTH); }
"height"                    { return symbol(sym.HEIGHT); }
"pointX"                    { return symbol(sym.POINT_X); }
"pointY"                    { return symbol(sym.POINT_Y); }
"orientation"               { return symbol(sym.ORIENTATION); }
"elements"                  { return symbol(sym.ELEMENTS); }
"styles"                    { return symbol(sym.STYLES); }
"content"                   { return symbol(sym.CONTENT); }
"label"                     { return symbol(sym.LABEL); }
"options"                   { return symbol(sym.OPTIONS); }
"correct"                   { return symbol(sym.CORRECT); }
"with"                      { return symbol(sym.WITH); }

/* -- Valores de orientacion -- */
"VERTICAL"                  { return symbol(sym.VERTICAL); }
"HORIZONTAL"                { return symbol(sym.HORIZONTAL); }

/* -- Familias de fuente -- */
"MONO"                      { return symbol(sym.MONO); }
"SANS_SERIF"                { return symbol(sym.SANS_SERIF); }
"CURSIVE"                   { return symbol(sym.CURSIVE); }

/* -- Tipos de borde -- */
"LINE"                      { return symbol(sym.LINE); }
"DOTTED"                    { return symbol(sym.DOTTED); }
"DOUBLE"                    { return symbol(sym.DOUBLE_BORDER); }

/* -- Colores predefinidos --  */
"RED"                       { return symbol(sym.COLOR_PREDEFINIDO, "RED"); }
"BLUE"                      { return symbol(sym.COLOR_PREDEFINIDO, "BLUE"); }
"GREEN"                     { return symbol(sym.COLOR_PREDEFINIDO, "GREEN"); }
"PURPLE"                    { return symbol(sym.COLOR_PREDEFINIDO, "PURPLE"); }
"SKY"                       { return symbol(sym.COLOR_PREDEFINIDO, "SKY"); }
"YELLOW"                    { return symbol(sym.COLOR_PREDEFINIDO, "YELLOW"); }
"BLACK"                     { return symbol(sym.COLOR_PREDEFINIDO, "BLACK"); }
"WHITE"                     { return symbol(sym.COLOR_PREDEFINIDO, "WHITE"); }

/* -- Bloques de codigo -- */
"IF"                        { return symbol(sym.IF); }
"ELSE"                      { return symbol(sym.ELSE); }
"WHILE"                     { return symbol(sym.WHILE); }
"DO"                        { return symbol(sym.DO); }
"FOR"                       { return symbol(sym.FOR); }
"in"                        { return symbol(sym.IN); }

/* -- PokeAPI -- */
"who_is_that_pokemon"       { return symbol(sym.WHO_IS_THAT_POKEMON); }
"hsl"                       { return symbol(sym.HSL_FUNC); }
"NUMBER"                    { return symbol(sym.NUMBER_CONST); }

/* -- Metodo de variable special -- */
"draw"                      { return symbol(sym.DRAW); }

/* NOTA: Los emojis no generan tokens propios.
   Son absorbidos por {cadena} como texto plano. */

/* 4. COLORES POR FORMATO */
{hex_color}                 { return symbol(sym.COLOR_HEX, yytext()); }
{rgb_color}                 { return symbol(sym.COLOR_RGB, yytext().replaceAll(" ", "").replaceAll("\t", "")); }
{hsl_color}                 { return symbol(sym.COLOR_HSL, yytext().replaceAll(" ", "").replaceAll("\t", "")); }

/* 5. LITERALES NUMERICOS */
{decimal}                   { return symbol(sym.NUMERO, Double.parseDouble(yytext())); }
{entero}                    { return symbol(sym.NUMERO, Double.parseDouble(yytext())); }

/* 6. CADENAS */
{cadena}                    {
                                String raw   = yytext();
                                String valor = raw.substring(1, raw.length() - 1);
                                // Convertir notaciones @[...] a emojis Unicode
                                valor = resolverEmojis(valor);
                                return symbol(sym.CADENA, valor);
                            }

/* 7. IDENTIFICADORES */
{identificador}             { return symbol(sym.IDENTIFICADOR, yytext()); }

/* 
   8 y 9. OPERADORES
   Los de 2 caracteres ANTES que los de 1 caracter.
   Sin esta regla, ">=" seria reconocido como ">" + "=".  */

/* -- Comparacion -- */
">="                        { return symbol(sym.MAYOR_IGUAL); }
"<="                        { return symbol(sym.MENOR_IGUAL); }
"=="                        { return symbol(sym.IGUAL_IGUAL); }
"!!"                        { return symbol(sym.DIFERENTE); }
">"                         { return symbol(sym.MAYOR); }
"<"                         { return symbol(sym.MENOR); }

/* -- Logicos -- */
"||"                        { return symbol(sym.OR); }
"&&"                        { return symbol(sym.AND); }
"~"                         { return symbol(sym.NOT); }

/* -- Aritmeticos --
   "-" actua como resta Y como unario negativo.
   CUP los distingue con precedencia. */
"+"                         { return symbol(sym.MAS); }
"-"                         { return symbol(sym.MENOS); }
"*"                         { return symbol(sym.POR); }
"/"                         { return symbol(sym.DIV); }
"^"                         { return symbol(sym.POT); }
"%"                         { return symbol(sym.MOD); }

/* -- Asignacion: despues de == para que = solo no sea confundido -- */
"="                         { return symbol(sym.ASIGNACION); }

/* 
   10. DELIMITADORES Y PUNTUACION
   ".." ANTES que "." para que el rango del FOR sea un token
   completo y no dos puntos separados. */
".."                        { return symbol(sym.DOTDOT); }
"."                         { return symbol(sym.PUNTO); }
";"                         { return symbol(sym.PUNTO_COMA); }
":"                         { return symbol(sym.DOS_PUNTOS); }
","                         { return symbol(sym.COMA); }
"?"                         { return symbol(sym.COMODIN); }
"["                         { return symbol(sym.CORCH_ABRE); }
"]"                         { return symbol(sym.CORCH_CIERRA); }
"{"                         { return symbol(sym.LLAVE_ABRE); }
"}"                         { return symbol(sym.LLAVE_CIERRA); }
"("                         { return symbol(sym.PAREN_ABRE); }
")"                         { return symbol(sym.PAREN_CIERRA); }

/* 11. EOF
   Con %cup es obligatorio retornar sym.EOF explicitamente.
   Sin esta regla CUP puede recibir null al final del archivo
   y lanzar una NullPointerException en el parser. */
<<EOF>>                     { return symbol(sym.EOF); }

/* 12. CATCH-ALL — Error lexico
   El "." en JFlex coincide con cualquier caracter excepto \n.
   Todo lo que llegue aqui no fue reconocido por ninguna regla
   anterior. Se registra el error y el scanner continua para
   poder reportar TODOS los errores en una sola pasada en lugar
   de detenerse en el primero.*/
.                           { registrarError(yytext()); }
