package com.example.pkmforms.analyzer.model

data class StyleData(
    val color:           String = "",
    val backgroundColor: String = "",
    val fontFamily:      String = "",
    val textSize:        Float  = 0f,
    val borderSize:      Float  = 0f,
    val borderType:      String = "",
    val borderColor:     String = ""
)

sealed class FormElement {
    data class OpenQuestion(
        val label:  String    = "",
        val width:  Int?      = null,
        val height: Int?      = null,
        val style:  StyleData = StyleData()
    ) : FormElement()

    data class DropQuestion(
        val label:   String      = "",
        val options: List<String> = emptyList(),
        val correct: Int         = -1,
        val width:   Int?        = null,
        val height:  Int?        = null,
        val style:   StyleData   = StyleData()
    ) : FormElement()

    data class SelectQuestion(
        val options: List<String> = emptyList(),
        val correct: Int         = -1,
        val width:   Int?        = null,
        val height:  Int?        = null,
        val style:   StyleData   = StyleData()
    ) : FormElement()

    data class MultipleQuestion(
        val options: List<String> = emptyList(),
        val correct: List<Int>   = emptyList(),
        val width:   Int?        = null,
        val height:  Int?        = null,
        val style:   StyleData   = StyleData()
    ) : FormElement()

    data class TextElement(
        val content: String    = "",
        val width:   Int?      = null,
        val height:  Int?      = null,
        val style:   StyleData = StyleData()
    ) : FormElement()

    data class Section(
        val orientation: String          = "VERTICAL",
        val width:       Int?            = null,
        val height:      Int?            = null,
        val pointX:      Int?            = null,   // posicion absoluta X (esquina superior izquierda)
        val pointY:      Int?            = null,   // posicion absoluta Y
        val elements:    List<FormElement> = emptyList(),
        val style:       StyleData       = StyleData()
    ) : FormElement()

    data class Table(
        val rows:   List<List<FormElement?>> = emptyList(),
        val width:  Int?                     = null,
        val height: Int?                     = null,
        val pointX: Int?                     = null,
        val pointY: Int?                     = null,
        val style:  StyleData                = StyleData()
    ) : FormElement()
}