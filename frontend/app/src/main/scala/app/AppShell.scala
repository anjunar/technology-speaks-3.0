package app

import app.components.security.LoggedInUser.loggedInUser
import app.services.ApplicationService
import app.support.{LayoutMode, LayoutResolver}
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.Link.{link, onClick as onLinkClick}
import jfx.core.component.CompositeComponent.DslContext
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property
import jfx.dsl.*
import jfx.layout.Drawer
import jfx.layout.Drawer.{drawer, drawerContent, drawerNavigation}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.layout.Viewport.viewport
import jfx.router.Router.router
import jfx.router.WindowRouter.{windowRouter, windowRouterLoading}
import jfx.statement.ForEach.forEach
import jfx.statement.ObserveRender.observeRender

object AppShellNavigation {

  def render(service: ApplicationService, variant: String, closeNavigation: () => Unit = () => {})(using DslContext): Unit = {
    link("/") {
      classes = Seq("app-shell-nav-link", s"app-shell-nav-link--$variant")

      onLinkClick { _ =>
        closeNavigation()
      }

      vbox {
        classes = Seq("app-shell-nav-item", s"app-shell-nav-item--$variant")

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
          classes = Seq("app-shell-nav-link", s"app-shell-nav-link--$variant")

          onLinkClick { _ =>
            closeNavigation()
          }

          vbox {
            classes = Seq("app-shell-nav-item", s"app-shell-nav-item--$variant")

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
}

class DesktopAppShell extends DivComposite {

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

            AppShellNavigation.render(service, "desktop")
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

class MobileAppShell extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = "app-shell"

    withDslContext {
      val service = inject[ApplicationService]
      val mobileNavigationOpen = Property(false)

      vbox {
        classes = "app-shell-mobile"

        style {
          display = "flex"
          flexDirection = "column"
          minHeight = "100vh"
        }

        drawer {
          classes = "app-shell-mobile-drawer"

          val mobileDrawer = summon[Drawer]
          mobileDrawer.width = "296px"
          addDisposable(Property.subscribeBidirectional(mobileNavigationOpen, mobileDrawer.openProperty))

          drawerNavigation {
            div {
              classes = Seq("app-shell-nav", "app-shell-nav-mobile")

              AppShellNavigation.render(service, "mobile", () => mobileNavigationOpen.set(false))
            }
          }

          drawerContent {
            div {
              classes = "app-shell-router-host"
              style {
                flex = "1"
                minHeight = "0px"
                overflow = "auto"
              }

              val mobileRouter = router(Routes.routes) {}
              given jfx.router.Router = mobileRouter

              observeRender(mobileRouter.loadingProperty) { isLoading =>
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
        }

        hbox {
          classes = "app-footer-bar glass"
          style {
            justifyContent = "space-between"
          }

          hbox {
            classes = "app-footer-actions"
            style {
              gap = "10px"
            }

            observeRender(mobileNavigationOpen) { isOpen =>
              button(if (isOpen) "close" else "menu") {
                classes = Seq("material-icons", "app-footer-control", "app-footer-control-nav")
                onClick { _ =>
                  mobileNavigationOpen.set(!mobileNavigationOpen.get)
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
}

object AppShell {
  def appShell()(using Scope): DivComposite =
    LayoutResolver.queryOverrideFromNavigation.getOrElse(LayoutResolver.autoDetect()) match {
      case LayoutMode.Desktop =>
        CompositeSupport.buildComposite(new DesktopAppShell())
      case LayoutMode.Mobile =>
        CompositeSupport.buildComposite(new MobileAppShell())
    }
}
