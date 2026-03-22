package jFx2.forms.editor

external interface EditorNode {
    val type : String
    val content : List<EditorNode>?
    val attrs : Map<String, EditorNode?>?
    val text : String?
    val marks : List<EditorNode>?
}