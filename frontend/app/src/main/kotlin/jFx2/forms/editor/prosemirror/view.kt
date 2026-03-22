@file:JsModule("prosemirror-view")
@file:JsNonModule

package jFx2.forms.editor.prosemirror

import org.w3c.dom.Element

external interface EditorViewMount {
    var mount: Element
}

external interface EditorViewPlaceFunction {
    operator fun invoke(dom: Element)
}

external class EditorView {
    constructor(place: Element, props: DirectEditorProps = definedExternally)
    constructor(place: EditorViewMount, props: DirectEditorProps = definedExternally)
    constructor(place: EditorViewPlaceFunction, props: DirectEditorProps = definedExternally)

    val dom: Element
    val state: EditorState

    fun dispatch(tr: Transaction)

    fun updateState(state: EditorState)
    fun focus()
    fun destroy()
}

external interface EditorProps {
    var state: EditorState?
    var dispatchTransaction: ((tr: Transaction) -> Unit)?
}

external interface DirectEditorProps : EditorProps {
    override var state: EditorState?
    override var dispatchTransaction: ((tr: Transaction) -> Unit)?
}