@file:JsModule("prosemirror-commands")
@file:JsNonModule

package jFx2.forms.editor.prosemirror


external fun deleteSelection(state: EditorState, dispatch: ((Transaction) -> Unit)? = definedExternally): Boolean
external fun joinBackward(state: EditorState, dispatch: ((Transaction) -> Unit)? = definedExternally): Boolean
external fun joinForward(state: EditorState, dispatch: ((Transaction) -> Unit)? = definedExternally): Boolean
external fun selectNodeBackward(
    state: EditorState,
    dispatch: ((Transaction) -> Unit)? = definedExternally
): Boolean

external fun selectNodeForward(
    state: EditorState,
    dispatch: ((Transaction) -> Unit)? = definedExternally
): Boolean

external fun toggleMark(markType: MarkType, attrs: Any? = definedExternally): Command

external fun setBlockType(nodeType: NodeType, attrs: Any? = definedExternally): Command
external fun wrapIn(nodeType: NodeType, attrs: Any? = definedExternally): Command
external fun lift(state: EditorState, dispatch: ((Transaction) -> Unit)? = definedExternally): Boolean

external val newlineInCode : Command
external val splitBlock : Command
external val exitCode : Command

external fun chainCommands(vararg commands: Command): Command
external fun exitCode(state: EditorState, dispatch: ((Transaction) -> Unit)? = definedExternally): Boolean

external val baseKeymap: Any

typealias Command = (state: EditorState, dispatch: ((Transaction) -> Unit)?, view: EditorView?) -> Boolean