@file:JsModule("prosemirror-schema-list")
@file:JsNonModule

package jFx2.forms.editor.prosemirror

external fun addListNodes(nodes: Any, itemContent: String, listGroup: String? = definedExternally): Any

external fun wrapInList(listType: NodeType, attrs: Any? = definedExternally): Command
external fun splitListItem(itemType: NodeType): Command
external fun liftListItem(itemType: NodeType): Command
external fun sinkListItem(itemType: NodeType): Command

external val orderedList: dynamic
external val bulletList: dynamic
external val listItem: dynamic
