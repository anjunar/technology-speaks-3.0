package app.pages.security

import app.domain.security.PasswordLogin
import app.services.ApplicationService
import app.support.{Api, Navigation}
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

class PasswordLoginPage extends PageComposite("Login", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val loginForm = new PasswordLogin()

  override protected def compose(using DslContext): Unit = {
    classProperty += "password-login-page"

    withDslContext {
      form(loginForm) {
        onSubmit_= { _ =>
          loginForm
            .save()
            .flatMap(_ => ApplicationService.invoke())
            .foreach { _ =>
              close()
              Navigation.queryParam("redirect").foreach(path => Navigation.navigate(path, replace = true))
            }
        }

        image {
          style {
            jfx.dsl.width_=("500px")
          }
          src_=("/app/security/login.png")
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Moechtest du dich anmelden?"
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

          inputContainer("Password") {
            input("password") {
              inputType_=("password")
            }
          }
        }

        div {
          classes = Seq("button-container")

          button("Abbrechen") {
            buttonType_=("button")
            classes = Seq("btn-secondary")
            onClick(_ => close())
          }

          button("Anmelden") {
            classes = Seq("btn-danger")
          }
        }
      }
    }
  }
}

object PasswordLoginPage {
  def passwordLoginPage(init: PasswordLoginPage ?=> Unit = {}): PasswordLoginPage =
    CompositeSupport.buildPage(new PasswordLoginPage)(init)
}
