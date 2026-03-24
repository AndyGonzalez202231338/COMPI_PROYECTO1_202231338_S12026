/*
 * PKM_FORMS - Analizador Lexico para archivos .pkm
 */
package com.example.pkmforms.analyzer.pkm;
import java_cup.runtime.*;
import java.util.ArrayList;

%%

%class  LexerPKM
%public
%unicode
%cup
%line
%column
%ignorecase
%cupsym symPKM

%state META
%state CADENA_STATE
%state EMOJI_STATE

%{
    public static ArrayList<String[]> errores = new ArrayList<>();

    /* Buffer para construir el valor de la cadena actual */
    private StringBuilder cadenaActual = new StringBuilder();

    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1);
    }
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

    /* Convierte el contenido de @[...] a Unicode */
    private String convertirEmoji(String contenido) {
        if (contenido.equals(":smile:"))   return "\uD83D\uDE00"; // feliz
        if (contenido.equals(":sad:"))     return "\uD83E\uDD72"; // triste
        if (contenido.equals(":serious:")) return "\uD83D\uDE10"; // serio
        if (contenido.equals(":heart:"))   return "\u2764\uFE0F"; // corazon
        if (contenido.equals(":star:"))    return "\u2B50";        // estrella
        if (contenido.equals(":cat:")  ||
            contenido.equals(":^^:"))      return "\uD83D\uDE3A"; // gato

        // :) :)) :)))))
        if (contenido.matches(":\\)+"))    return "\uD83D\uDE00";
        // :( :((
        if (contenido.matches(":\\(+"))    return "\uD83E\uDD72";
        // :| :||
        if (contenido.matches(":\\|+"))    return "\uD83D\uDE10";
        // <3, <<3, :<3, :<<<<33333:
        if (contenido.matches("<+3+"))     return "\u2764\uFE0F";
        if (contenido.matches(":<+3+:?")) return "\u2764\uFE0F";

        // :star:N: o :star:N
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile(":star:(\\d+):?").matcher(contenido);
        if (m.matches()) {
            int n = Math.min(Integer.parseInt(m.group(1)), 50);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < n; k++) sb.append("\u2B50");
            return sb.toString();
        }

        return null; // no reconocido
    }
%}

/* ====== MACROS ====== */
digito   = [0-9]
entero   = {digito}+
decimal  = {digito}+"."{digito}+
espacio  = [ \t\r\n]+
hex_color = "#"[A-Fa-f0-9]{6}

%%

<YYINITIAL> {
    {espacio}    { /* ignorar */ }

    /* Inicio bloque metadatos */
    "###"        { yybegin(META); }

    /* Tags de apertura */
    "<section="          { return symbol(symPKM.TAG_SECTION_OPEN); }
    "<table="            { return symbol(symPKM.TAG_TABLE_OPEN); }
    "<open="             { return symbol(symPKM.TAG_OPEN_OPEN); }
    "<drop="             { return symbol(symPKM.TAG_DROP_OPEN); }
    "<select="           { return symbol(symPKM.TAG_SELECT_OPEN); }
    "<multiple="         { return symbol(symPKM.TAG_MULTIPLE_OPEN); }
    "<text="             { return symbol(symPKM.TAG_TEXT_OPEN); }
    "<style>"            { return symbol(symPKM.TAG_STYLE_OPEN); }
    "<content>"          { return symbol(symPKM.TAG_CONTENT_OPEN); }
    "<line>"             { return symbol(symPKM.TAG_LINE_OPEN); }
    "<element>"          { return symbol(symPKM.TAG_ELEMENT_OPEN); }
    "<color="            { return symbol(symPKM.STYLE_COLOR); }
    "<background color=" { return symbol(symPKM.STYLE_BG); }
    "<font family="      { return symbol(symPKM.STYLE_FONT); }
    "<text size="        { return symbol(symPKM.STYLE_SIZE); }
    "<border,"           { return symbol(symPKM.STYLE_BORDER); }

    /* Tags de cierre */
    "</section>"  { return symbol(symPKM.TAG_SECTION_CLOSE); }
    "</table>"    { return symbol(symPKM.TAG_TABLE_CLOSE); }
    "</open>"     { return symbol(symPKM.TAG_OPEN_CLOSE); }
    "</drop>"     { return symbol(symPKM.TAG_DROP_CLOSE); }
    "</select>"   { return symbol(symPKM.TAG_SELECT_CLOSE); }
    "</multiple>" { return symbol(symPKM.TAG_MULTIPLE_CLOSE); }
    "</text>"     { return symbol(symPKM.TAG_TEXT_CLOSE); }
    "</style>"    { return symbol(symPKM.TAG_STYLE_CLOSE); }
    "</content>"  { return symbol(symPKM.TAG_CONTENT_CLOSE); }
    "</line>"     { return symbol(symPKM.TAG_LINE_CLOSE); }
    "</element>"  { return symbol(symPKM.TAG_ELEMENT_CLOSE); }

    /* Fuentes */
    "MONO"       { return symbol(symPKM.FONT_VAL, "MONO"); }
    "SANS_SERIF" { return symbol(symPKM.FONT_VAL, "SANS_SERIF"); }
    "CURSIVE"    { return symbol(symPKM.FONT_VAL, "CURSIVE"); }

    /* Tipos de borde */
    "LINE"       { return symbol(symPKM.BORDER_TYPE, "LINE"); }
    "DOTTED"     { return symbol(symPKM.BORDER_TYPE, "DOTTED"); }
    "DOUBLE"     { return symbol(symPKM.BORDER_TYPE, "DOUBLE"); }

    /* Orientacion */
    "VERTICAL"   { return symbol(symPKM.ORIENTATION, "VERTICAL"); }
    "HORIZONTAL" { return symbol(symPKM.ORIENTATION, "HORIZONTAL"); }

    /* color= dentro de border — ignorar */
    "color="     { /* ignorar */ }

    /* Colores predefinidos */
    "RED"        { return symbol(symPKM.COLOR, "RED"); }
    "BLUE"       { return symbol(symPKM.COLOR, "BLUE"); }
    "GREEN"      { return symbol(symPKM.COLOR, "GREEN"); }
    "PURPLE"     { return symbol(symPKM.COLOR, "PURPLE"); }
    "SKY"        { return symbol(symPKM.COLOR, "SKY"); }
    "YELLOW"     { return symbol(symPKM.COLOR, "YELLOW"); }
    "BLACK"      { return symbol(symPKM.COLOR, "BLACK"); }
    "WHITE"      { return symbol(symPKM.COLOR, "WHITE"); }

    /* Colores por formato */
    {hex_color}                         { return symbol(symPKM.COLOR, yytext()); }
    "("[0-9]+","[0-9]+","[0-9]+")"     { return symbol(symPKM.COLOR, yytext()); }
    "<"[0-9]+","[0-9]+","[0-9]+">"    { return symbol(symPKM.COLOR, yytext()); }

    /* Numeros */
    {decimal}    { return symbol(symPKM.NUMERO, Double.parseDouble(yytext())); }
    {entero}     { return symbol(symPKM.NUMERO, Double.parseDouble(yytext())); }

    /* Inicio de cadena - entra al estado CADENA_STATE */
    \"           {
                     cadenaActual = new StringBuilder();
                     yybegin(CADENA_STATE);
                 }

    /* Delimitadores */
    ","          { return symbol(symPKM.COMA); }
    "/>"         { return symbol(symPKM.SLASH_GT); }
    "/"          { return symbol(symPKM.SLASH); }
    ">"          { return symbol(symPKM.GT); }
    "{"          { return symbol(symPKM.LLAVE_ABRE); }
    "}"          { return symbol(symPKM.LLAVE_CIERRA); }
    "-"          { return symbol(symPKM.GUION); }

    <<EOF>>      { return symbol(symPKM.EOF); }
    .            { /* ignorar */ }
}

/* ---- Estado CADENA_STATE: acumula contenido ---- */
<CADENA_STATE> {
    /* Cierre de cadena — devuelve el token CADENA con valor acumulado */
    \"           {
                     yybegin(YYINITIAL);
                     return symbol(symPKM.CADENA, cadenaActual.toString());
                 }

    /* Inicio de emoji @[ — entra a EMOJI_STATE */
    "@["         {
                     yybegin(EMOJI_STATE);
                 }

    /* Cualquier otro caracter (incluyendo Unicode/emojis residuales) */
    [^\"\n@]+    { cadenaActual.append(yytext()); }
    "@"          { cadenaActual.append(yytext()); }

    /* Fin de linea o EOF sin cerrar — devuelve lo acumulado */
    \n           {
                     yybegin(YYINITIAL);
                     return symbol(symPKM.CADENA, cadenaActual.toString());
                 }
    <<EOF>>      {
                     yybegin(YYINITIAL);
                     return symbol(symPKM.CADENA, cadenaActual.toString());
                 }
}


/* ---- Estado EMOJI_STATE: lee contenido de @[...] ---- */
<EMOJI_STATE> {
    /* Cierre del emoji ] */
    [^\]]*"]"    {
                     /* yytext() es el contenido + "]", quitamos el ] final */
                     String contenido = yytext();
                     contenido = contenido.substring(0, contenido.length() - 1);
                     String emoji = convertirEmoji(contenido);
                     if (emoji != null) {
                         cadenaActual.append(emoji);
                     } else {
                         /* No reconocido - agregar la notacion original */
                         cadenaActual.append("@[").append(contenido).append("]");
                     }
                     yybegin(CADENA_STATE);
                 }

    /* EOF sin cerrar */
    <<EOF>>      { yybegin(YYINITIAL); return symbol(symPKM.EOF); }
}


/* ---- Estado META: ignorar bloque de metadatos ---- */
<META> {
    "###"        { yybegin(YYINITIAL); }
    [^#]+        { /* ignorar */ }
    "#"          { /* ignorar */ }
    <<EOF>>      { return symbol(symPKM.EOF); }
}
