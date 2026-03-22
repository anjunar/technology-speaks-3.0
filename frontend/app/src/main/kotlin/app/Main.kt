@file:Suppress("UnsafeCastFromDynamic")

package app

import app.components.security.loggedInUser
import app.services.ApplicationService
import jFx2.controls.button
import jFx2.controls.link
import jFx2.controls.span
import jFx2.controls.text
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.rendering.foreach
import jFx2.core.rendering.observeRender
import jFx2.core.runtime.component
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.layout.vbox
import jFx2.modals.Viewport
import jFx2.modals.viewport
import jFx2.router.windowRouter
import jFx2.state.JobRegistry
import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise
import org.w3c.dom.HTMLDivElement

fun main() {
    val root = document.getElementById("root") as HTMLDivElement

    val jobs = JobRegistry.instance

    val initAppJob = jobs.launch(label = "ApplicationService.invoke", owner = "app") {
        ApplicationService.invoke()
    }

    val fontsLoaded = document.asDynamic().fonts.load("24px 'Material Icons'") as Promise<*>

    jobs.scope.launch {
        fontsLoaded.await()
        initAppJob.join()

        component(root) {
            vbox {

                hbox {
                    className { "app-header-bar" }

                    loggedInUser {
                        style {
                            marginRight = "10px"
                        }
                    }
                }

                div {
                    className { "app-shell-body" }

                    observeRender(ApplicationService.app) { app ->
                        div {

                            onDispose(Viewport.windows.observe { windows ->
                                className (if (windows.isNotEmpty()) "glass app-shell-nav app-shell-nav-left" else "glass app-shell-nav app-shell-nav-center"  )
                            })

                            foreach(app.links, { key -> key.id }) { link, index ->
                                link(link.url) {
                                    vbox {

                                        style { alignItems = "center" }

                                        span {
                                            className { "material-icons icon" }
                                            text { link.icon }
                                        }
                                        span {
                                            className { "app-shell-nav-text" }
                                            text {
                                                link.name
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }

                    viewport {
                        windowRouter(Routes.routes) { }
                    }
                }

                hbox {

                    className { "app-footer-bar" }

                    hbox {

                        foreach(Viewport.windows, { key -> key.id }) { window, index ->
                            div {

                                style {
                                    background = "var(--color-background-primary)"
                                    color = if (Viewport.isActive(window)) "var(--color-selected)" else "var(--color-text)"
                                    margin = "2px"
                                    padding = "2px"
                                    lineHeight = "24px"
                                    height = "24px"
                                    width = "200px"
                                    textAlign = "center"
                                }

                                text {
                                    window.title
                                }

                                onClick {
                                    Viewport.touchWindow(window)
                                }
                            }
                        }
                    }

                    hbox {
                        style {
                            justifyContent = "flex-end"
                        }

                        button("dark_mode") {
                            className { "material-icons" }
                            onClick {
                                ApplicationService.darkMode.set(!ApplicationService.darkMode.get())
                            }
                        }
                    }
                }


            }
        }
    }

}
