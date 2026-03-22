@file:JsModule("prosemirror-transform")
@file:JsNonModule

package jFx2.forms.editor.prosemirror

open external class Transform(doc: Node) {
    val doc: Node
    fun replace(from: Int, to: Int, slice: Slice = definedExternally): Transform
}

external class Step

external class StepResult {
    val doc: Node?
    val failed: String?
}

external interface Mappable {
    fun map(pos: Int, assoc: Int = definedExternally): Int
}
