package jFx2.core.rendering

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.dom.RangeInsertPoint
import jFx2.core.runtime.ComponentMount
import jFx2.core.runtime.componentWithScope
import jFx2.state.Disposable
import jFx2.state.ReadOnlyProperty
import kotlinx.browser.document
import org.w3c.dom.Comment
import org.w3c.dom.DocumentFragment

private class ObserveRenderComponent<T>(
    override val node: DocumentFragment,
    private val start: Comment,
    private val end: Comment,
    private val source: ReadOnlyProperty<T>,
    private val block: context(NodeScope) (T) -> Unit
) : Component<DocumentFragment>() {

    private var disposed = false
    private var baseScope: NodeScope? = null

    private var committedRange: RangeInsertPoint? = null
    private var currentMount: ComponentMount? = null

    private val owner = RangeOwner(start)

    override fun mount() {
        with(baseScope!!) {
            rebuild(source.get())
        }
    }

    context(scope: NodeScope)
    fun init() {
        baseScope = scope

        val d: Disposable = source.observe { v ->
            scheduleRebuild(v)
        }
        onDispose(d)
    }

    private fun ensureRangeCommitted(): RangeInsertPoint? {
        committedRange?.let { return it }
        return RangeInsertPoint(start, end).also { committedRange = it }
    }

    private fun scheduleRebuild(v: T) {
        val scope = baseScope ?: return
        if (disposed) return

        with(scope) { rebuild(v) }
    }

    context(scope: NodeScope)
    private fun rebuild(value: T) {
        if (disposed) return

        val range = ensureRangeCommitted() ?: return

        currentMount?.dispose()
        currentMount = null
        range.clear()

        val childScope = scope.fork(
            parent = range.parent,
            owner = owner,
            ctx = scope.ctx.fork(),
            insertPoint = range
        )

        currentMount = componentWithScope(childScope) {
            block(value)
        }

        with(childScope) {
            this@ObserveRenderComponent.owner.afterBuild()
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
fun <T> observeRender(
    source: ReadOnlyProperty<T>,
    block: context(NodeScope) (T) -> Unit
) {
    val start: Comment = document.createComment("jFx2:observe")
    val end: Comment = document.createComment("jFx2:/observe")

    val fragment = document.createDocumentFragment()
    fragment.appendChild(start)
    fragment.appendChild(end)

    val comp = ObserveRenderComponent(
        node = fragment,
        start = start,
        end = end,
        source = source,
        block = block
    )

    scope.attach(comp)
    with(scope) { comp.init() }
}
