package app.pages.security

import app.services.ApplicationService
import app.support.Api
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class LogoutPage extends PageComposite("Abmelden", pageResizable = false) {

  override def pageWidth: Int = 840
  override def pageHeight: Int = 660

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "logout-page"

    withDslContext {

      val service = inject[ApplicationService]

      vbox {
        classes = "security-page__layout security-page__layout--compact"

        vbox {
          classes = "security-page__hero"

          div {
            classes = "security-page__hero-copy"

            span {
              classes = "security-page__eyebrow"
              text = "Ausgang"
            }

            heading(2) {
              classes = "security-page__title"
              text = "Sitzung beenden"
            }

            span {
              classes = "security-page__subtitle"
              text = "Melde dich ab, wenn du den Wissensraum in dieser Sitzung verlassen moechtest."
            }
          }
        }

        hbox {
          classes = "security-page__content"

          div {
            classes = "security-page__media-shell"

            image {
              classes = "security-page__image"
              src_=("/app/security/logout_dark.png")
            }
          }

          vbox {
            classes = "security-page__panel"

            hbox {
              classes = "security-page__panel-header"

              heading(3) {
                classes = "security-page__panel-title"
                text = "Abmelden"
              }
            }

            span {
              classes = "security-page__panel-copy"
              text = "Offene Ansichten bleiben lokal sichtbar, bis sie geschlossen werden."
            }

            div {
              classes = "security-page__actions"

              button("Abbrechen") {
                buttonType_=("button")
                classes = "security-page__button-secondary"
                onClick(_ => close())
              }

              button("Abmelden") {
                classes = "security-page__button-primary"
                onClick { _ =>
                  Api
                    .post[app.support.JsonResponse]("/service/security/logout")
                    .flatMap(_ => {
                      service.invoke()
                    })
                    .foreach(_ => close())
                }
              }
            }
          }
        }
      }
    }
  }
}

object LogoutPage {
  def logoutPage(init: LogoutPage ?=> Unit = {})(using Scope): LogoutPage =
    CompositeSupport.buildPage(new LogoutPage)(init)
}
