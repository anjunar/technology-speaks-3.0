package app.pages.security

import app.domain.core.Data
import app.domain.security.{Account, CreatePassword, PasswordChange}
import app.services.ApplicationService
import app.support.{Api, Navigation, JsonResponse}
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

class AccountPage(val payload: Data[Account]) extends PageComposite("Account", pageResizable = false) {

  override def pageWidth: Int = 880
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val model: Account = payload.data
  private val passwordChangeForm = new PasswordChange()
  private val createPasswordForm = new CreatePassword()

  override protected def compose(using DslContext): Unit = {
    classProperty += "account-page"

    withDslContext {
      val service = inject[ApplicationService]
      val changePasswordLink = Navigation.linkByRel("changePassword", model.links)
      val createPasswordLink = Navigation.linkByRel("createPassword", model.links)
      val canChangePassword = changePasswordLink.isDefined
      val canCreatePassword = createPasswordLink.isDefined

      def navigateBack(): Unit = {
        val target =
          Option(service.app.get.user.id.get)
            .map(id => s"/core/users/user/$id")
            .getOrElse("/")

        Navigation.navigate(target, replace = true)
      }

      def contentLayout(using DslContext): Unit = {
        vbox {
          classes = "security-page__layout"

          vbox {
            classes = "security-page__hero"

            div {
              classes = "security-page__hero-copy"

              span {
                classes = "security-page__eyebrow"
                text = "Account"
              }

              heading(2) {
                classes = "security-page__title"
                text = "Zugang und Passwort"
              }

              span {
                classes = "security-page__subtitle"
                text =
                  if (canChangePassword) "Hier aenderst du dein Passwort getrennt von Profil, Sichtbarkeit und anderen Profildaten."
                  else if (canCreatePassword) "Dein Konto nutzt noch kein Passwort. Du kannst hier zusaetzlich einen Passwort-Zugang anlegen."
                  else "Fuer diesen Account ist aktuell keine Passwort-Aktion verfuegbar."
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
                  text =
                    if (canChangePassword) "Passwort aendern"
                    else if (canCreatePassword) "Passwort setzen"
                    else "Account"
                }
              }

              span {
                classes = "security-page__panel-copy"
                text =
                  if (canChangePassword) "Zur Sicherheit wird dein aktuelles Passwort geprueft, bevor das neue gespeichert wird."
                  else if (canCreatePassword) "Dein Account verwendet derzeit nur WebAuthn oder einen passwortlosen Zugang. Hier kannst du zusaetzlich ein Passwort hinterlegen."
                  else "Bitte pruefe die verfuegbaren Login-Methoden deines Accounts."
              }

              div {
                classes = "security-page__field-group"

                if (canChangePassword) {
                  inputContainer("Aktuelles Passwort") {
                    input("currentPassword") {
                      classes = "security-page__input"
                      inputType_=("password")
                    }
                  }
                }

                inputContainer("Neues Passwort") {
                  input(if (canChangePassword) "newPassword" else "newPassword") {
                    classes = "security-page__input"
                    inputType_=("password")
                  }
                }

                inputContainer("Neues Passwort wiederholen") {
                  input(if (canChangePassword) "confirmPassword" else "confirmPassword") {
                    classes = "security-page__input"
                    inputType_=("password")
                  }
                }
              }

              div {
                classes = "security-page__actions"

                button("Zurueck") {
                  buttonType_=("button")
                  classes = "security-page__button-secondary"
                  onClick(_ => navigateBack())
                }

                button("Passwort speichern") {
                  classes = "security-page__button-primary"
                }
              }
            }
          }
        }
      }

      if (canChangePassword) {
        form(passwordChangeForm) {
          classes = "security-page__form"

          onSubmit_= { (_: Form[PasswordChange]) =>
            val currentPassword = passwordChangeForm.currentPassword.get.trim
            val newPassword = passwordChangeForm.newPassword.get.trim
            val confirmPassword = passwordChangeForm.confirmPassword.get.trim

            if (currentPassword.isEmpty) {
              Viewport.notify("Bitte gib dein aktuelles Passwort ein.", Viewport.NotificationKind.Error)
            } else if (newPassword.isEmpty) {
              Viewport.notify("Bitte gib ein neues Passwort ein.", Viewport.NotificationKind.Error)
            } else if (newPassword != confirmPassword) {
              Viewport.notify("Die neuen Passwoerter stimmen nicht ueberein.", Viewport.NotificationKind.Error)
            } else {
              Api.invokeLink[JsonResponse](changePasswordLink.get, passwordChangeForm)
                .foreach { response =>
                  if (response != null && response.status == "success") {
                    Viewport.notify("Passwort aktualisiert.", Viewport.NotificationKind.Success)
                    passwordChangeForm.currentPassword.set("")
                    passwordChangeForm.newPassword.set("")
                    passwordChangeForm.confirmPassword.set("")
                  } else {
                    Viewport.notify(
                      Option(response)
                        .flatMap(value => Option(value.message))
                        .getOrElse("Passwort konnte nicht aktualisiert werden."),
                      Viewport.NotificationKind.Error
                    )
                  }
                }
            }
          }

          contentLayout
        }
      } else if (canCreatePassword) {
        form(createPasswordForm) {
          classes = "security-page__form"

          onSubmit_= { (_: Form[CreatePassword]) =>
            val newPassword = createPasswordForm.newPassword.get.trim
            val confirmPassword = createPasswordForm.confirmPassword.get.trim

            if (newPassword.isEmpty) {
              Viewport.notify("Bitte gib ein neues Passwort ein.", Viewport.NotificationKind.Error)
            } else if (newPassword != confirmPassword) {
              Viewport.notify("Die neuen Passwoerter stimmen nicht ueberein.", Viewport.NotificationKind.Error)
            } else {
              Api.invokeLink[JsonResponse](createPasswordLink.get, createPasswordForm)
                .foreach { response =>
                  if (response != null && response.status == "success") {
                    Viewport.notify("Passwort hinzugefuegt.", Viewport.NotificationKind.Success)
                    Navigation.navigate("/security/account", replace = true)
                  } else {
                    Viewport.notify(
                      Option(response)
                        .flatMap(value => Option(value.message))
                        .getOrElse("Passwort konnte nicht gesetzt werden."),
                      Viewport.NotificationKind.Error
                    )
                  }
                }
            }
          }

          contentLayout
        }
      } else {
        div {
          classes = "security-page__form"
          contentLayout
        }
      }
    }
  }
}

object AccountPage {
  def accountPage(payload: Data[Account], init: AccountPage ?=> Unit = {})(using Scope): AccountPage =
    CompositeSupport.buildPage(new AccountPage(payload))(init)
}
