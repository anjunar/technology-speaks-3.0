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
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.{input, inputType_=}
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

import scala.concurrent.ExecutionContext

class WebAuthnLoginPage extends PageComposite("Login mit WebAuthn", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val loginForm = new WebAuthnLogin()

  override protected def compose(using DslContext): Unit = {
    classProperty += "webauthn-login-page"

    withDslContext {
      val service = injectFromDsl[ApplicationService]

      form(loginForm) {
        onSubmit_= { (event : Form[WebAuthnLogin])  =>
          WebAuthnLoginClient
            .login(loginForm.email.get)
            .flatMap(_ => {
              service.invoke()
            })
            .foreach { _ =>
              close()
              Navigation.queryParam("redirect").foreach(path => Navigation.navigate(path, replace = true))
            }
        }

        image {
          style {
            width = "500px"
          }
          src = "/app/security/login_webauthn.png"
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Moechtest du dich mit WebAuthn anmelden?"
          }
        }

        div {
          style {
            padding = "20px"
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

          button("Anmelden") {
            classes = "btn-danger"
          }
        }
      }
    }
  }
}

object WebAuthnLoginPage {
  def webAuthnLoginPage(init: WebAuthnLoginPage ?=> Unit = {}): WebAuthnLoginPage =
    CompositeSupport.buildPage(new WebAuthnLoginPage)(init)
}
