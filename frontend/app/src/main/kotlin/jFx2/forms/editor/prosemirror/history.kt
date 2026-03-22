@file:JsModule("prosemirror-history")
@file:JsNonModule

package jFx2.forms.editor.prosemirror


external interface HistoryOptions {
    var depth: Int?
    var newGroupDelay: Int?
}

external fun history(options: HistoryOptions = definedExternally): Plugin<Any?>

external val undo : Command
external val redo: Command

external fun undoDepth(state: Any): Int
external fun redoDepth(state: Any): Int
