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
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.ExecutionContext

class PasswordRegisterPage extends PageComposite("Register", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val registerForm = new PasswordRegister()

  override protected def compose(using DslContext): Unit = {
    classProperty += "password-register-page"

    withDslContext {
      form(registerForm) {
        classes = "security-page__form"

        onSubmit_= { (event : Form[PasswordRegister])  =>
          registerForm
            .save()
            .foreach(response =>
              if (response != null && response.status == "success") {
                Viewport.notify("Registrierung abgeschlossen.", Viewport.NotificationKind.Success)
                close()
              } else {
                Viewport.notify(
                  Option(response)
                    .flatMap(value => Option(value.message))
                    .getOrElse("Registrierung fehlgeschlagen."),
                  Viewport.NotificationKind.Error
                )
              }
            )
        }

        vbox {
          classes = "security-page__layout"

          vbox {
            classes = "security-page__hero"

            div {
              classes = "security-page__hero-copy"

              span {
                classes = "security-page__eyebrow"
                text = "Eintritt"
              }

              heading(2) {
                classes = "security-page__title"
                text = "Einen neuen Zugang anlegen"
              }

              span {
                classes = "security-page__subtitle"
                text = "Erstelle dein Profil und oeffne dir den Zugang zu Dokumenten, Dialogen und Gruppen."
              }
            }
          }

          hbox {
            classes = "security-page__content"

            div {
              classes = "security-page__media-shell"

              image {
                classes = "security-page__image"
                src_=("/app/security/register_password_dark.png")
              }
            }

            vbox {
              classes = "security-page__panel"

              hbox {
                classes = "security-page__panel-header"

                heading(3) {
                  classes = "security-page__panel-title"
                  text = "Registrierung"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text = "Lege Nickname, Email und Passwort fuer dein Konto fest."
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

                inputContainer("Password") {
                  input("password") {
                    classes = "security-page__input"
                    inputType_=("password")
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

object PasswordRegisterPage {
  def passwordRegisterPage(init: PasswordRegisterPage ?=> Unit = {})(using Scope): PasswordRegisterPage =
    CompositeSupport.buildPage(new PasswordRegisterPage)(init)
}
