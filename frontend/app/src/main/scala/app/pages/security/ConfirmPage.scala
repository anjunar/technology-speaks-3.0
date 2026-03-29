package app.pages.security

import app.domain.documents.Document
import app.domain.security.ConfirmCode
import app.services.ApplicationService
import app.support.Api
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import org.scalajs.dom
import org.scalajs.dom.{RequestInit, fetch}

import scala.concurrent.ExecutionContext
import scala.scalajs.js

class ConfirmPage extends PageComposite("Bestaetigen", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 720

  private given ExecutionContext = ExecutionContext.global

  private val confirmForm = new ConfirmCode()

  override protected def compose(using DslContext): Unit = {
    classProperty += "confirm-page"

    withDslContext {
      val service = inject[ApplicationService]

      form(confirmForm) {
        classes = "security-page__form"

        onSubmit_= { (event : Form[ConfirmCode])  =>

          val init = js.Dynamic.literal(
            method = "POST"
          )


          fetch(s"/service/security/confirm?code=${confirmForm.confirm.get}", init.asInstanceOf[RequestInit])
            .`then`(response => {
              service.invoke()
              Viewport.notify("Bestaetigung erfolgreich.", Viewport.NotificationKind.Success)
              close()
            })
        }

        vbox {
          classes = "security-page__layout"

          vbox {
            classes = "security-page__hero"

            div {
              classes = "security-page__hero-copy"

              span {
                classes = "security-page__eyebrow"
                text = "Pruefung"
              }

              heading(2) {
                classes = "security-page__title"
                text = "Email bestaetigen"
              }

              span {
                classes = "security-page__subtitle"
                text = "Gib den Bestaetigungscode aus deiner Email ein, um den Zugang freizuschalten."
              }
            }
          }

          hbox {
            classes = "security-page__content"

            div {
              classes = "security-page__media-shell"

              image {
                classes = "security-page__image"
                src_=(s"/app/security/confirm_${if service.darkMode.get then "dark" else "light"}.png")
              }
            }

            vbox {
              classes = "security-page__panel"

              hbox {
                classes = "security-page__panel-header"

                heading(3) {
                  classes = "security-page__panel-title"
                  text = "Bestaetigung"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text = "Der Code verbindet diese Sitzung mit deinem bestaetigten Konto."
              }

              div {
                classes = "security-page__field-group"

                inputContainer("Bestaetigen") {
                  input("confirm") {
                    classes = "security-page__input"
                  }
                }
              }

              div {
                classes = "security-page__actions"

                button("Abbrechen") {
                  buttonType_=("button")
                  classes = "security-page__button-secondary"
                  onClick(_ => close())
                }

                button("Bestaetigen") {
                  classes = "security-page__button-primary"
                }
              }
            }
          }
        }
      }
    }
  }
}

object ConfirmPage {
  def confirmPage(init: ConfirmPage ?=> Unit = {})(using Scope): ConfirmPage =
    CompositeSupport.buildPage(new ConfirmPage)(init)
}
