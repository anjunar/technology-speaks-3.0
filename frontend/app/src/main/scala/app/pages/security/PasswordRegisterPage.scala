package app.pages.security

import app.domain.documents.Document
import app.domain.security.PasswordRegister
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

import scala.concurrent.ExecutionContext

class PasswordRegisterPage extends PageComposite("Register", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val registerForm = new PasswordRegister()

  override protected def compose(using DslContext): Unit = {
    classProperty += "password-register-page"

    withDslContext {
      form(registerForm) {
        onSubmit_= { (event : Form[PasswordRegister])  =>
          registerForm
            .save()
            .foreach(_ => close())
        }

        image {
          style {
            jfx.dsl.width_=("500px")
          }
          src_=("/app/security/register.png")
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Moechtest du dich registrieren?"
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

          inputContainer("Password") {
            input("password") {
              inputType_=("password")
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

object PasswordRegisterPage {
  def passwordRegisterPage(init: PasswordRegisterPage ?=> Unit = {})(using Scope): PasswordRegisterPage =
    CompositeSupport.buildPage(new PasswordRegisterPage)(init)
}
