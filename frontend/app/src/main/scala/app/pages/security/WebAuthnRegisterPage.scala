package app.pages.security

import app.domain.security.WebAuthnRegister
import app.services.WebAuthnRegistrationClient
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.{input, inputType_=}
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

import scala.concurrent.ExecutionContext

class WebAuthnRegisterPage extends PageComposite("Register mit WebAuthn", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val registerForm = new WebAuthnRegister()

  override protected def compose(using DslContext): Unit = {
    classProperty += "webauthn-register-page"

    withDslContext {
      form(registerForm) {
        onSubmit_= { _ =>
          WebAuthnRegistrationClient
            .register(registerForm.email.get, registerForm.nickName.get)
            .foreach(_ => close())
        }

        image {
          style {
            jfx.dsl.width_=("500px")
          }
          src_=("/app/security/register_webauthn.png")
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Moechtest du dich mit WebAuthn registrieren?"
          }
        }

        div {
          style {
            padding = "20px"
          }

          inputContainer("Nick name") {
            input("nickName") {}
          }

          inputContainer("Email") {
            input("email") {
              inputType_=("email")
            }
          }
        }

        div {
          classes = "button-container"

          button("Abbrechen") {
            buttonType_=("button")
            classes = "btn-secondary"
            onClick(_ => close())
          }

          button("Registrieren") {
            classes = "btn-danger"
          }
        }
      }
    }
  }
}

object WebAuthnRegisterPage {
  def webAuthnRegisterPage(init: WebAuthnRegisterPage ?=> Unit = {}): WebAuthnRegisterPage =
    CompositeSupport.buildPage(new WebAuthnRegisterPage)(init)
}
