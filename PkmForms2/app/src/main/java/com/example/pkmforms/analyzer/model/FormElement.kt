package com.example.pkmforms.analyzer.model

sealed class FormElement {

    data class OpenQuestion(
        val label  : String  = "",
        val width  : Int?    = null,
        val height : Int?    = null
    ) : FormElement()

    data class DropQuestion(
        val label   : String       = "",
        val options : List<String> = emptyList(),
        val correct : Int          = -1,
        val width   : Int?         = null,
        val height  : Int?         = null
    ) : FormElement()

    data class SelectQuestion(
        val options : List<String> = emptyList(),
        val correct : Int          = -1,
        val width   : Int?         = null,
        val height  : Int?         = null
    ) : FormElement()

    data class MultipleQuestion(
        val options : List<String> = emptyList(),
        val correct : List<Int>    = emptyList(),
        val width   : Int?         = null,
        val height  : Int?         = null
    ) : FormElement()

    data class TextElement(
        val content : String = "",
        val width   : Int?   = null,
        val height  : Int?   = null
    ) : FormElement()
}