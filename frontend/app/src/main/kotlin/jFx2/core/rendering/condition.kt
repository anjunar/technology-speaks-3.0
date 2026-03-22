package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.dom.RangeInsertPoint
import jFx2.core.dsl.renderFields
import jFx2.core.runtime.ComponentMount
import jFx2.core.runtime.componentWithScope
import jFx2.state.Disposable
import jFx2.state.Property
import kotlinx.browser.document
import org.w3c.dom.Comment
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Element

class ConditionOwner(override val node: Element) : Component<Element>() {

    context(scope: NodeScope)
    fun afterBuild() {
        renderFields(*this@ConditionOwner.children.toTypedArray())
    }

}

class ConditionBuilder internal constructor() {
    internal var thenBlock: (context(NodeScope) () -> Unit)? = null
    internal var elseBlock: (context(NodeScope) () -> Unit)? = null

    fun then(block: context(NodeScope) () -> Unit) { thenBlock = block }
    fun elseDo(block: context(NodeScope) () -> Unit) { elseBlock = block }
}

private class ConditionComponent(
    override val node: DocumentFragment,
    private val start: Comment,
    private val end: Comment,
    private val builder: ConditionBuilder,
    private val readFlag: (() -> Boolean)?,
    private val subscribe: (((Boolean) -> Unit) -> Disposable)? // null => polling
) : Component<DocumentFragment>() {

    private var disposed = false

    private var committedRange: RangeInsertPoint? = null
    private val ownerEl: Element = document.createElement("div").unsafeCast<Element>()
    private val owner = ConditionOwner(ownerEl)

    private var current: Boolean? = null
    private var currentMount: ComponentMount? = null

    private var baseScope: NodeScope? = null

    private var subscription: Disposable? = null

    override fun mount() {
        with(baseScope!!) {
            if (readFlag == null) {
                onDispose(subscribe?.invoke { rebuild(it) }!!)
            } else {
                rebuild( readFlag())
            }

        }
    }

    context(scope: NodeScope)
    fun init() {
        baseScope = scope

        subscribe?.let { sub ->
            val d = sub { v -> scheduleRebuild(v) }
            onDispose(d)
        } ?: run {
            schedulePoll()
        }
    }

    private fun ensureRangeCommitted(): RangeInsertPoint? {
        if (committedRange != null) return committedRange

        return RangeInsertPoint(start, end).also { committedRange = it }
    }

    private fun scheduleRebuild(v: Boolean) {
        val scope = baseScope ?: return
        if (disposed) return

        scope.ui.build.afterBuild {
            if (disposed) return@afterBuild
            with(scope) {
                rebuild(v)
            }
        }
    }

    context(scope: NodeScope)
    fun rebuild(v: Boolean) {
        if (disposed) return
        if (current == v) return
        current = v

        val range = ensureRangeCommitted() ?: return

        currentMount?.dispose()
        currentMount = null
        range.clear()

        val chosen = if (v) builder.thenBlock else builder.elseBlock ?: return

        val childScope = scope.fork(
            parent = range.parent,
            owner = owner,
            ctx = scope.ctx.fork(),
            insertPoint = range
        )

        currentMount = componentWithScope(childScope, chosen!!)

        with(childScope) { this@ConditionComponent.owner.afterBuild() }
    }

    private fun schedulePoll() {
        val scope = baseScope ?: return
        if (disposed) return
        if (readFlag == null) return

        scope.ui.build.afterBuild {
            if (disposed) return@afterBuild
            scope.ui.build.dirty {
                if (disposed) return@dirty
                with(scope) { rebuild(readFlag()) }
            }
            schedulePoll()
        }
    }

    override fun dispose() {
        disposed = true

        runCatching { currentMount?.dispose() }
        currentMount = null

        runCatching { committedRange?.dispose() }
        committedRange = null

        runCatching { end.parentNode?.removeChild(end) }

        super.dispose()
    }
}


context(scope: NodeScope)
fun condition(flag: Property<Boolean>, build: ConditionBuilder.() -> Unit) {
    val builder = ConditionBuilder().apply(build)

    val start: Comment = document.createComment("jFx2:condition")
    val end: Comment = document.createComment("jFx2:/condition")

    val fragment = document.createDocumentFragment()
    fragment.appendChild(start)
    fragment.appendChild(end)

    val comp = ConditionComponent(
        node = fragment,
        start = start,
        end = end,
        builder = builder,
        readFlag = null,
        subscribe = { cb -> flag.observe(cb) }
    )

    scope.attach(comp)

    comp.init()

}


context(scope: NodeScope)
fun condition(flag: () -> Boolean, build: ConditionBuilder.() -> Unit) {
    val builder = ConditionBuilder().apply(build)

    val start: Comment = document.createComment("jFx2:condition")
    val end: Comment = document.createComment("jFx2:/condition")

    val fragment = document.createDocumentFragment()
    fragment.appendChild(start)
    fragment.appendChild(end)

    val comp = ConditionComponent(
        node = fragment,
        start = start,
        end = end,
        builder = builder,
        readFlag = flag,
        subscribe = null
    )

    scope.attach(comp)

    comp.init()
}
