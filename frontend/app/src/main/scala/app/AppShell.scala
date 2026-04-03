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
import jfx.router.WindowRouter.{windowRouter, windowRouterLoading}
import jfx.statement.ForEach.forEach
import jfx.statement.ObserveRender.observeRender

class AppShell extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = "app-shell"

    withDslContext {

      val service = inject[ApplicationService]

      vbox {

        style {
          display = "flex"
          flexDirection = "column"
        }

        div {
          classes = "app-shell-body"

          viewport {}

          div {
            classes = Seq("glass", "app-shell-nav", "app-shell-nav-center")

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
          }

          div {
            classes = "app-shell-router-host"

            given router: jfx.router.WindowRouter = windowRouter(Routes.routes) {}

            observeRender(windowRouterLoading) { isLoading =>
              if (isLoading) {
                div {
                  classes = "glass-border"
                  style {
                    position = "fixed"
                    top = "24px"
                    left = "50%"
                    transform = "translateX(-50%)"
                    display = "flex"
                    zIndex = "1400"
                  }

                  vbox {
                    classes = "glass"
                    style {
                      alignItems = "center"
                      justifyContent = "center"
                      rowGap = "12px"
                      padding = "22px 26px"
                      borderRadius = "20px"
                      minWidth = "220px"
                    }

                    span {
                      classes = Seq("material-icons", "icon")
                      text = "hourglass_top"
                    }

                    span {
                      text = "Ansicht wird geladen..."
                    }
                  }
                }
              }
            }
          }
        }

        hbox {
          classes = "app-footer-bar glass"

          style {
            justifyContent = "space-between"
          }

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
              gap = "10px"
            }

            button("dark_mode") {
              classes = Seq("material-icons", "app-footer-control")
              onClick { _ =>
                service.darkMode.set(!service.darkMode.get)
              }
            }

            loggedInUser {
              classes += "app-header-user"
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
