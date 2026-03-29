package app

import app.components.security.LoggedInUser.loggedInUser
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.{button, buttonType, buttonType_=, onClick}
import jfx.control.Link.link
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.layout.Viewport.viewport
import jfx.router.WindowRouter.windowRouter
import jfx.statement.ForEach.forEach
import jfx.statement.ObserveRender.observeRender

class AppShell extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = "app-shell"

    withDslContext {

      val service = inject[ApplicationService]

      vbox {

        viewport {

          style {
            display = "flex"
            flexDirection = "column"
          }

          hbox {
            classes = "app-header-bar glass"

            loggedInUser {
              classes += "app-header-user"
            }
          }

          div {
            classes = "app-shell-body"

            div {
              addDisposable(Viewport.windows.observe(windows => {
                if (windows.nonEmpty) {
                  classes = Seq("glass", "app-shell-nav", "app-shell-nav-left")
                } else {
                  classes = Seq("glass", "app-shell-nav", "app-shell-nav-center")
                }
              }))

              link("/") {
                classes = "app-shell-nav-link"

                vbox {
                  classes = "app-shell-nav-item"

                  style {
                    alignItems = "center"
                  }

                  span {
                    classes = Seq("material-icons", "icon")
                    text = "home"
                  }

                  span {
                    classes = "app-shell-nav-text"
                    text = "Home"
                  }
                }
              }

              observeRender(service.app) { app =>
                app.links.foreach { currentLink =>
                  link(currentLink.url) {
                    classes = "app-shell-nav-link"

                    vbox {
                      classes = "app-shell-nav-item"

                      style {
                        alignItems = "center"
                      }

                      span {
                        classes = Seq("material-icons", "icon")
                        text = currentLink.icon
                      }

                      span {
                        classes = "app-shell-nav-text"
                        text = currentLink.name
                      }
                    }
                  }
                }

              }

              windowRouter(Routes.routes) {}
            }
          }

          hbox {
            classes = "app-footer-bar glass"

            hbox {
              classes = "app-footer-tabs"

              forEach(Viewport.windows) { window =>
                button(window.title) {
                  buttonType = "button"
                  classes =
                    if (Viewport.isActive(window)) Seq("app-footer-window", "is-active")
                    else Seq("app-footer-window", "is-inactive")
                  onClick { _ =>
                    Viewport.touchWindow(window)
                  }
                }
              }
            }

            hbox {
              classes = "app-footer-actions"
              style {
                justifyContent = "flex-end"
                flex = "1"
              }

              button("dark_mode") {
                classes = Seq("material-icons", "app-footer-control")
                onClick { _ =>
                  service.darkMode.set(!service.darkMode.get)
                }
              }
            }
          }
        }

      }
    }
  }
}

object AppShell {
  def appShell()(using Scope): AppShell =
    CompositeSupport.buildComposite(new AppShell())({})
}
