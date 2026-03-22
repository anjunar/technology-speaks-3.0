@file:JsModule("prosemirror-state")
@file:JsNonModule

package jFx2.forms.editor.prosemirror

open external class Selection {
    val from: Int
    val to: Int
    val empty: Boolean
    @JsName("\$from")
    val _from: ResolvedPos
}

external class EditorState {
    val doc: Node
    val schema: Schema
    val selection: Selection
    val tr: Transaction

    val storedMarks: Array<Mark>?

    fun apply(tr: Transaction): EditorState

    companion object {
        fun create(config: EditorStateConfig = definedExternally): EditorState
    }
}

external class Transaction(state: EditorState) : Transform {
    val docChanged: Boolean
    fun setNodeMarkup(
        pos: Int,
        type: NodeType,
        attrs: dynamic = definedExternally,
        marks: Array<Mark> = definedExternally
    ): Transaction
    fun replaceSelectionWith(
        node: Node,
        inheritMarks: Boolean = definedExternally
    ): Transaction
    fun scrollIntoView(): Transaction
}

external interface EditorStateConfig {
    var schema: Schema?
    var doc: Node?
    var plugins: Array<Plugin<Any?>>?
}

external class Plugin<PluginState>(spec: PluginSpec<PluginState> = definedExternally) {
    val spec: PluginSpec<PluginState>
    fun getState(state: EditorState): PluginState?
}

external interface PluginView {
    fun update(view: EditorView, prevState: EditorState = definedExternally)
    fun destroy()
}

external interface PluginSpec<PluginState> {
    var key: PluginKey<PluginState>?
    var state: StateField<PluginState>?
    var props: dynamic

    var view: ((view: EditorView) -> PluginView)?
}

external class PluginKey<PluginState>(name: String = definedExternally) {
    fun get(state: EditorState): Plugin<PluginState>?
    fun getState(state: EditorState): PluginState?
}

external interface StateField<T> {
    fun init(config: EditorStateConfig, state: EditorState): T
    fun apply(tr: Transaction, value: T, oldState: EditorState, newState: EditorState): T
}

external class NodeSelection : Selection {
    val node: Node

    companion object {
        fun create(doc: Node, from: Int): NodeSelection
    }
}


