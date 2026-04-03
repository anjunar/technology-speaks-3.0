package app.pages.security

import app.domain.documents.Document
import app.domain.security.WebAuthnLogin
import app.services.{ApplicationService, WebAuthnLoginClient}
import app.support.Navigation
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.form.{ErrorResponseException, Form}
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.{input, inputType_=}
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class WebAuthnLoginPage extends PageComposite("Login mit WebAuthn", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val loginForm = new WebAuthnLogin()

  override protected def compose(using DslContext): Unit = {
    classProperty += "webauthn-login-page"

    withDslContext {
      val service = inject[ApplicationService]

      form(loginForm) {
        classes = "security-page__form"

        onSubmit_= { (event : Form[WebAuthnLogin])  =>
          WebAuthnLoginClient
            .login(loginForm.email.get)
            .onComplete {
              case Success(saved) =>
                Viewport.notify("Login erfolgreich!", Viewport.NotificationKind.Success)
                close()
                service.invoke()
                val target = Navigation.queryParam("redirect").filter(_.trim.nonEmpty).getOrElse("/")
                Navigation.navigate(target, replace = true)

              case Failure(e: ErrorResponseException) =>
                Viewport.notify("Fehler beim Anmelden!", Viewport.NotificationKind.Error)
                event.setErrorResponses(e.errors)

              case Failure(e: Throwable) =>
                Viewport.notify(s"Fehler im Server ${e.getMessage}", Viewport.NotificationKind.Error)
            }
        }

        vbox {
          classes = "security-page__layout"

          vbox {
            classes = "security-page__hero"

            div {
              classes = "security-page__hero-copy"

              span {
                classes = "security-page__eyebrow"
                text = "Passkey"
              }

              heading(2) {
                classes = "security-page__title"
                text = "Anmeldung mit WebAuthn"
              }

              span {
                classes = "security-page__subtitle"
                text = "Nutze deinen sicheren lokalen Zugang statt eines Passworts."
              }
            }
          }

          hbox {
            classes = "security-page__content"

            div {
              classes = "security-page__media-shell"

              image {
                classes = "security-page__image"
                src = s"/app/security/login_webauthn_${if service.darkMode.get then "dark" else "light"}.png"
              }
            }

            vbox {
              classes = "security-page__panel"

              hbox {
                classes = "security-page__panel-header"

                heading(3) {
                  classes = "security-page__panel-title"
                  text = "WebAuthn Login"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text = "Gib deine Email ein und bestaetige die Anmeldung auf deinem Geraet."
              }

              div {
                classes = "security-page__field-group"

                inputContainer("Email") {
                  input("email") {
                    classes = "security-page__input"
                    inputType_=("email")
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

                button("Anmelden") {
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

object WebAuthnLoginPage {
  def webAuthnLoginPage(init: WebAuthnLoginPage ?=> Unit = {})(using Scope): WebAuthnLoginPage =
    CompositeSupport.buildPage(new WebAuthnLoginPage)(init)
}
