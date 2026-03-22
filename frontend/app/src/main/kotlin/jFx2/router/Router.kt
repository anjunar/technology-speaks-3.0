package jFx2.router

import app.domain.core.Link
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.rendering.dynamicOutlet
import jFx2.state.Property
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.dom.CustomEvent
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["router"])
class Router(override val node: HTMLDivElement, val ui: UiScope, val routes: List<Route>) :
    Component<HTMLDivElement>() {

    val content = Property<Component<*>?>(null)

    context(scope: NodeScope)
    fun afterBuild() {

        dynamicOutlet(content)

        fun extracted() {
            val resolveRoutes = resolveRoutes(routes, window.location.pathname)
            val routeMatch = resolveRoutes.matches.last()

            val scope = CoroutineScope(SupervisorJob())

            scope.launch {
                content.set(routeMatch.route.factory!!(routeMatch.params))
                ui.build.flush()
            }
        }

        window.addEventListener("popstate", {

            extracted()

        })

        extracted()
    }

}

fun renderByRel(rel : String, links : List<Link>, body : (Link) -> Unit) {
    val find = links.find { it.rel == rel }
    if (find != null) {
        body(find)
    }
}

fun navigateByRel(rel : String, links : List<Link>, body : (() -> Unit) -> Unit) {
    val find = links.find { it.rel == rel }
    if (find != null) {
        body { navigate(find.url) }
    }
}

fun navigate(url: String) {
    window.history.pushState(null, "", url)
    window.dispatchEvent(CustomEvent("popstate"))
}

