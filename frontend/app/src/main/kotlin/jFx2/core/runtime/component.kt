package jFx2.core.runtime

import jFx2.core.Component
import jFx2.core.Ctx
import jFx2.core.capabilities.*
import jFx2.core.dom.RangeInsertPoint
import jFx2.core.dsl.renderFields
import kotlinx.browser.document
import org.w3c.dom.Comment
import org.w3c.dom.Node

private class RootComponent(override val node: Node) : Component<Node>() {

    context(scope: NodeScope)
    fun afterBuild() {

        renderFields(*this@RootComponent.children.toTypedArray())

    }

}

class ComponentMount(
    val ui: UiScope,
    val scope: NodeScope,
    private val disposeScope: DisposeScope
) {

    fun dispose() = disposeScope.dispose()
}

fun component(
    root: Node,
    owner: Component<*>? = null,
    ui: UiScope = UiScope(),
    ctx: Ctx = Ctx(),
    block: context(NodeScope) () -> Unit
): ComponentMount {
    val rootOwner = owner ?: RootComponent(root)

    val dispose = DisposeScope()
    val scope = NodeScope(
        ui = ui,
        parent = root,
        owner = rootOwner,
        ctx = ctx,
        dispose = dispose
    )

    with(scope) {
        ui.build.afterBuild {
            if (rootOwner is RootComponent) rootOwner.afterBuild()
        }
    }

    block(scope)
    ui.build.flush()

    return ComponentMount(ui, scope, dispose)
}

/**
 * Mounts into an already existing scope.
 *
 * - Uses the provided scope's insertPoint (ElementRoot or RangeRoot).
 * - The returned mount's dispose is exactly scope.dispose.dispose().
 *
 * This is the primitive needed for "fragment mounting" without requiring a host element.
 */
fun componentWithScope(
    scope: NodeScope,
    block: context(NodeScope) () -> Unit
): ComponentMount {
    block(scope)
    scope.ui.build.flush()
    return ComponentMount(scope.ui, scope, scope.dispose)
}

/**
 * Creates a comment range inside parentScope's insertPoint and mounts block into that range.
 *
 * Dispose:
 * - disposes child scope (thus all registered disposables)
 * - clears range content
 * - removes markers
 */
fun componentRange(
    parentScope: NodeScope,
    owner: Component<*>? = null,
    ctx: Ctx = parentScope.ctx.fork(),
    startLabel: String = "jFx2:range",
    endLabel: String = "jFx2:/range",
    block: context(NodeScope) () -> Unit
): ComponentMount {
    val start: Comment = document.createComment(startLabel)
    val end: Comment = document.createComment(endLabel)

    // Insert markers at the parent's insert point location
    parentScope.insertPoint.insert(start)
    parentScope.insertPoint.insert(end)

    val rangeInsertPoint = RangeInsertPoint(start, end)

    val rangeOwner = owner ?: RootComponent(start) // off-DOM owner node doesn't matter much; must be stable
    val childScope = parentScope.fork(
        parent = rangeInsertPoint.parent,
        owner = rangeOwner,
        ctx = ctx,
        insertPoint = rangeInsertPoint
    )

    // Ensure range is removed even if user forgets
    childScope.dispose.register {
        rangeInsertPoint.dispose()
    }

    return componentWithScope(childScope, block)
}