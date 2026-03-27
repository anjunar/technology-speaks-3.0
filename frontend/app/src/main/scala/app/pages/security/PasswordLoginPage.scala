package app.pages.security

import app.domain.documents.Document
import app.domain.security.PasswordLogin
import app.services.ApplicationService
import app.support.{Api, Navigation}
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

class PasswordLoginPage extends PageComposite("Login", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val loginForm = new PasswordLogin()

  override protected def compose(using DslContext): Unit = {
    classProperty += "password-login-page"

    withDslContext {
      val service = inject[ApplicationService]

      form(loginForm) {
        classes = "security-page__form"

        onSubmit_= { (event : Form[PasswordLogin])  =>
          loginForm
            .save()
            .flatMap(response => {
              if (response == null || response.status != "success") {
                Viewport.notify(
                  Option(response)
                    .flatMap(value => Option(value.message))
                    .getOrElse("Login fehlgeschlagen."),
                  Viewport.NotificationKind.Error
                )
                scala.concurrent.Future.failed(RuntimeException("Password login failed"))
              } else {
                service.invoke()
              }
            })
            .foreach { _ =>
              close()
              val target = Navigation.queryParam("redirect").filter(_.trim.nonEmpty).getOrElse("/")
              Navigation.navigate(target, replace = true)
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
                text = "Zugang"
              }

              heading(2) {
                classes = "security-page__title"
                text = "Willkommen im Wissensraum"
              }

              span {
                classes = "security-page__subtitle"
                text = "Melde dich an, um Dokumente, Beziehungen und Resonanzen weiterzufuehren."
              }
            }
          }

          hbox {
            classes = "security-page__content"

            div {
              classes = "security-page__media-shell"

              image {
                classes = "security-page__image"
                src_=(s"/app/security/login_password_${if service.darkMode.get then "dark" else "light"}.png")
              }
            }

            vbox {
              classes = "security-page__panel"

              hbox {
                classes = "security-page__panel-header"

                heading(3) {
                  classes = "security-page__panel-title"
                  text = "Anmeldung"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text = "Gib deine Email und dein Passwort ein."
              }

              div {
                classes = "security-page__field-group"

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

object PasswordLoginPage {
  def passwordLoginPage(init: PasswordLoginPage ?=> Unit = {})(using Scope): PasswordLoginPage =
    CompositeSupport.buildPage(new PasswordLoginPage)(init)
}
