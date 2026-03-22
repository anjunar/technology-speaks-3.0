package app

import app.components.security.LoggedInUser.loggedInUser
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Link.link
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport.viewport
import jfx.layout.Viewport
import jfx.router.WindowRouter.windowRouter
import jfx.statement.ForEach.forEach

class AppShell extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("app-shell"))

    val app = ApplicationService.app.get

    withDslContext {
      vbox {
        hbox {
          classes = Seq("app-header-bar")

          loggedInUser {
            style {
              marginRight = "10px"
            }
          }
        }

        div {
          classes = Seq("app-shell-body")

          viewport {
            div {
              classes = Seq("glass", "app-shell-nav", "app-shell-nav-center")

              app.links.foreach { currentLink =>
                link(currentLink.url) {
                  vbox {
                    style {
                      alignItems = "center"
                    }

                    span {
                      classes = Seq("material-icons", "icon")
                      text = currentLink.icon
                    }

                    span {
                      classes = Seq("app-shell-nav-text")
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
          classes = Seq("app-footer-bar")

          hbox {
            forEach(Viewport.windows) { window =>
              button(window.title) {
                buttonType_=("button")
                classes =
                  if (Viewport.isActive(window)) Seq("btn-secondary")
                  else Seq("btn-primary")
                onClick { _ =>
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
              classes = Seq("material-icons")
              onClick { _ =>
                ApplicationService.darkMode.set(!ApplicationService.darkMode.get)
              }
            }
          }
        }
      }
    }
  }
}

object AppShell {
  def appShell(init: AppShell ?=> Unit = {}): AppShell =
    CompositeSupport.buildComposite(new AppShell)(init)
}
