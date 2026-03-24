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
import jfx.layout.Viewport.{viewport, windows}
import jfx.layout.Viewport
import jfx.router.WindowRouter.windowRouter
import jfx.statement.ForEach.forEach
import jfx.statement.ObserveRender.observeRender

class AppShell extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = "app-shell"

    withDslContext {
      vbox {
        hbox {
          classes = "app-header-bar"

          loggedInUser {
            style {
              marginRight = "10px"
            }
          }
        }

        div {
          classes = "app-shell-body"

          viewport {
            div {
              addDisposable(Viewport.windows.observe(windows => {
                if (windows.nonEmpty) {
                  classes = Seq("glass", "app-shell-nav", "app-shell-nav-left")
                } else {
                  classes = Seq("glass", "app-shell-nav", "app-shell-nav-center")
                }
              }))

              observeRender(ApplicationService.app) { app =>
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
                        classes = "app-shell-nav-text"
                        text = currentLink.name
                      }
                    }
                  }
                }
              }
              
            }

            windowRouter(Routes.routes) {}
          }
        }

        hbox {
          classes = "app-footer-bar"

          hbox {
            forEach(Viewport.windows) { window =>
              button(window.title) {
                buttonType = "button"
                classes =
                  if (Viewport.isActive(window)) "btn-secondary"
                  else "btn-primary"
                onClick { _ =>
                  Viewport.touchWindow(window)
                }
              }
            }
          }

          hbox {
            style {
              justifyContent = "flex-end"
              flex = "1"
            }

            button("dark_mode") {
              classes = "material-icons"
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
