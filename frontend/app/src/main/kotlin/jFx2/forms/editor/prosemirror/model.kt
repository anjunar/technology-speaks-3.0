@file:JsModule("prosemirror-model")
@file:JsNonModule

package jFx2.forms.editor.prosemirror

import org.w3c.dom.Node as DomNode

external class Schema(spec: SchemaSpec = definedExternally) {
    var nodes: dynamic
    var marks: dynamic
    val spec: SchemaSpec
}

external interface SchemaSpec {
    var nodes: dynamic
    var marks: dynamic
    var topNode: String?
}

external class Node {
    val type: NodeType
    val attrs: dynamic

    val marks: Array<Mark>
    val content: Fragment

    fun nodeAt(pos: Int): Node?

    fun nodesBetween(
        from: Int,
        to: Int,
        f: (node: Node, pos: Int, parent: Node, index: Int) -> Boolean,
        startPos: Int = definedExternally,
    )
}

external class NodeType {
    val name: String
    fun create(
        attrs: dynamic = definedExternally,
        content: Fragment = definedExternally,
        marks: Array<Mark> = definedExternally
    ): Node
}

external class Mark {
    val type: MarkType
    val attrs: dynamic
}

external class MarkType {
    val name: String
    fun isInSet(set: Array<Mark>): Mark?
}

external class Fragment {
    companion object {
        fun from(node: Node): Fragment
    }
}

external class Slice(content: Fragment, openStart: Int, openEnd: Int)

external class ResolvedPos {
    val pos: Int
    val parent: Node
    fun marks(): Array<Mark>
}

external class DOMParser private constructor() {
    fun parse(dom: DomNode, options: ParseOptions = definedExternally): Node

    companion object {
        fun fromSchema(schema: Schema): DOMParser
    }
}

external interface ParseOptions {
    var preserveWhitespace: dynamic /* Boolean | "full" */
}

external class DOMSerializer private constructor() {
    fun serializeFragment(fragment: Fragment, options: dynamic = definedExternally): DomNode

    companion object {
        fun fromSchema(schema: Schema): DOMSerializer
    }
}

typealias DOMOutputSpec = Any

external interface ParseRule {
    var tag: String?
    var getAttrs: ((dom: DomNode) -> dynamic)?
}

external interface AttrSpec {
    var default: Any?
}

external interface NodeSpec {
    var inline: Boolean?
    var group: String?
    var draggable: Boolean?
    var attrs: dynamic
    var parseDOM: Array<ParseRule>?
    var toDOM: ((node: dynamic) -> DOMOutputSpec)?
}

external object TextSelection
