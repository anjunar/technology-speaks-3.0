package app.pages.security

import app.domain.documents.Document
import app.domain.security.WebAuthnRegister
import app.services.{ApplicationService, WebAuthnRegistrationClient}
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.{input, inputType_=}
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class WebAuthnRegisterPage extends PageComposite("Register mit WebAuthn", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val registerForm = new WebAuthnRegister()

  override protected def compose(using DslContext): Unit = {
    classProperty += "webauthn-register-page"

    withDslContext {
      val service = inject[ApplicationService]
      
      form(registerForm) {
        classes = "security-page__form"

        onSubmit_= { (event : Form[WebAuthnRegister])  =>
          WebAuthnRegistrationClient
            .register(registerForm.email.get, registerForm.nickName.get)
            .foreach(_ => close())
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
                text = "Registrierung mit WebAuthn"
              }

              span {
                classes = "security-page__subtitle"
                text = "Lege einen passwortlosen Zugang an und verknuepfe ihn mit deinem Profil."
              }
            }
          }

          hbox {
            classes = "security-page__content"

            div {
              classes = "security-page__media-shell"

              image {
                classes = "security-page__image"
                src_=(s"/app/security/register_webauthn_${if service.darkMode.get then "dark" else "light"}.png")
              }
            }

            vbox {
              classes = "security-page__panel"

              hbox {
                classes = "security-page__panel-header"

                heading(3) {
                  classes = "security-page__panel-title"
                  text = "WebAuthn Registrierung"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text = "Trage Nickname und Email ein, danach erfolgt die Passkey-Bestaetigung."
              }

              div {
                classes = "security-page__field-group"

                inputContainer("Nick name") {
                  input("nickName") {
                    classes = "security-page__input"
                  }
                }

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

                button("Registrieren") {
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

object WebAuthnRegisterPage {
  def webAuthnRegisterPage(init: WebAuthnRegisterPage ?=> Unit = {})(using Scope): WebAuthnRegisterPage =
    CompositeSupport.buildPage(new WebAuthnRegisterPage)(init)
}
