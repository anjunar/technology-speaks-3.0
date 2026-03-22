package jFx2.router

import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.capabilities.UiScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.modals.Viewport
import jFx2.state.JobRegistry
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@JfxComponentBuilder(classes = ["window-router"])
class WindowRouter(override val node: HTMLDivElement, val routes: List<Route>) : Component<HTMLDivElement>() {

    context(scope: NodeScope)
    fun afterBuild() {

        fun addRouteToWindows() {

            JobRegistry.instance.launch("Router", "Router") {
                val pathname = window.location.pathname
                val resolveRoutes = resolveRoutes(routes, pathname)
                val routeMatch = resolveRoutes.matches.last()
                val component = routeMatch.route.factory!!(routeMatch.params)
                val page = component as PageInfo
                Viewport.addWindow(
                    Viewport.Companion.WindowConf(
                        page.name,
                        { component },
                        onClick = {
                            window.history.pushState(null, "", pathname)
                        },
                        resizable = page.resizable
                    )
                )
            }

        }

        window.addEventListener("popstate", {
            addRouteToWindows()
        })

        addRouteToWindows()


    }

}