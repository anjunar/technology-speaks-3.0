package jFx2.core.dom

import org.w3c.dom.Comment
import org.w3c.dom.Node

interface DomInsertPoint {
    val parent: Node
    fun insert(node: Node)
    fun clear()
    fun dispose()
}

class ElementInsertPoint(
    override val parent: Node
) : DomInsertPoint {
    override fun insert(node: Node) {
        parent.appendChild(node)
    }

    override fun clear() {
        while (parent.firstChild != null) {
            parent.removeChild(parent.firstChild!!)
        }
    }

    override fun dispose() {
        clear()
    }
}

class RangeInsertPoint(
    val start: Comment,
    val end: Comment
) : DomInsertPoint {
    override val parent: Node
        get() = start.parentNode ?: error("RangeInsertPoint: start has no parentNode")

    override fun insert(node: Node) {
        parent.insertBefore(node, end)
    }

    override fun clear() {
        var n = start.nextSibling
        while (n != null && n !== end) {
            val next = n.nextSibling
            n.parentNode?.removeChild(n)
            n = next
        }
    }

    override fun dispose() {
        clear()
        start.parentNode?.removeChild(start)
        end.parentNode?.removeChild(end)
    }
}

fun moveRangeInclusive(parent: Node, start: Node, end: Node, before: Node?) {
    val toMove = ArrayList<Node>()
    var n: Node? = start
    while (n != null) {
        toMove.add(n)
        if (n === end) break
        n = n.nextSibling
    }

    for (node in toMove) {
        if (before == null) parent.appendChild(node) else parent.insertBefore(node, before)
    }
}