package app.pages.core

import app.domain.core.{Address, Link, User, UserInfo}
import app.support.Navigation.renderByRel
import app.support.Api
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property
import jfx.dsl.*
import jfx.form.Editable.{editable, editable_=}
import jfx.form.{ErrorResponseException, Form, SubForm}
import jfx.form.Form.{form, onSubmit}
import jfx.form.ImageCropper.{aspectRatio, imageCropper, outputMaxHeight, outputMaxWidth, outputQuality, outputType}
import jfx.form.Input.{input, inputType}
import jfx.form.InputContainer.inputContainer
import jfx.form.SubForm.{factory, subForm}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserPage(val model: User) extends PageComposite("User", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val infoDisabled = Property(model.info.get == null)
  private val addressDisabled = Property(model.address.get == null)

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page"

    withDslContext {
      hbox {
        classes = "user-page-shell"

        form(model) {

          onSubmit = { (event: Form[User]) =>

            model.update().onComplete {
              case Success(_) =>
                Viewport.notify("Benutzer gespeichert!", Viewport.NotificationKind.Success)

              case Failure(e: ErrorResponseException) =>
                Viewport.notify("Fehler im Benutzer", Viewport.NotificationKind.Error)
                event.setErrorResponses(e.errors)

              case _ => ()
            }
          }

          style {
            width = "100%"
          }

          vbox {
            classes = "user-page-content"

            hbox {
              classes = "user-page-header"

              imageCropper("image") {
                classes = "user-page-avatar"

                style {
                  width = "420px"
                  height = "420px"
                  maxWidth = "100%"
                }

                aspectRatio = 1.0
                outputType = "image/jpeg"
                outputQuality = 0.92
                outputMaxWidth = 512
                outputMaxHeight = 512
              }

              div {
                classes = "user-page-sidebar"

                UserFollowAction.action(model)

                inputContainer("Nickname") {
                  input("nickName")
                }

                hbox {
                  classes = "user-page-section-row"

                  style {
                    alignItems = "flex-start"
                  }

                  subForm[UserInfo]("info") {

                    style {
                      flex = "1"
                    }

                    factory = () => new UserInfo()

                    addDisposable(infoDisabled.observeWithoutInitial(disabled => {
                      editable = !disabled
                      if (disabled) {
                        SubForm.clearForm()
                      } else {
                        SubForm.newInstance()
                      }
                    }))

                    inputContainer("Vorname") {
                      input("firstName")
                    }

                    inputContainer("Nachname") {
                      input("lastName")
                    }

                    inputContainer("Geburtsdatum") {
                      input("birthDate") {
                        inputType = "date"
                      }
                    }

                    editable = !infoDisabled.get
                  }

                  renderByRel("update", model.links) { () =>
                    button("close") {
                      buttonType = "button"
                      classes = Seq("material-icons", "user-page-toggle")

                      onClick { _ =>
                        infoDisabled.set(!infoDisabled.get)
                      }
                    }
                  }
                }

                hbox {
                  classes = "user-page-section-row"

                  subForm[Address]("address") {

                    style {
                      flex = "1"
                    }

                    factory = () => new Address()

                    addDisposable(addressDisabled.observeWithoutInitial(disabled => {
                      editable = !disabled

                      if (disabled) {
                        SubForm.clearForm()
                      } else {
                        SubForm.newInstance()
                      }
                    }))

                    inputContainer("Strasse") {
                      input("street")
                    }

                    inputContainer("Hausnummer") {
                      input("number")
                    }

                    inputContainer("Postleitzahl") {
                      input("zipCode")
                    }

                    inputContainer("Land") {
                      input("country")
                    }

                    editable = !addressDisabled.get
                  }

                  renderByRel("update", model.links) { () =>
                    button("close") {
                      buttonType = "button"
                      classes = Seq("material-icons", "user-page-toggle")

                      onClick { _ =>
                        addressDisabled.set(!addressDisabled.get)
                      }
                    }
                  }
                }
              }
            }

            hbox {
              classes = "user-form-actions"

              style {
                justifyContent = "flex-end"
                columnGap = "10px"
              }

              renderByRel("update", model.links) { () =>
                button("Speichern") {
                  classes = "user-page-save-btn"
                }
              }
            }
          }

          editable = model.links.exists(_.rel == "update")
        }
      }
    }
  }
}

object UserPage {
  def userPage(model: User, init: UserPage ?=> Unit = {})(using Scope): UserPage =
    CompositeSupport.buildPage(new UserPage(model))(init)
}

private final class UserFollowAction(model: User) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val actionLinkProperty: Property[Link | Null] = Property(model.followActionLink.orNull)
  private val busyProperty: Property[Boolean] = Property(false)

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-follow-slot"

    addDisposable(
      model.links.observe { _ =>
        actionLinkProperty.set(model.followActionLink.orNull)
      }
    )

    withDslContext {
      observeRender(actionLinkProperty) { link =>
        if (link != null) {
          val isUnfollow = link.rel == "unfollow"
          val buttonLabel = if (isUnfollow) "nicht Folgen" else "Folgen"
          val statusText =
            if (isUnfollow) "Du folgst diesem Nutzer bereits."
            else "Folge diesem Nutzer direkt aus dem Profil."

          hbox {
            classes = "user-follow-card"

            div {
              classes = "user-follow-copy"

              div {
                classes = "user-follow-label"
                text = "Netzwerk"
              }

              div {
                classes = "user-follow-text"
                text = statusText
              }
            }

            val actionButton = button(buttonLabel) {
              buttonType = "button"
              classes = Seq("user-follow-btn", if (isUnfollow) "is-unfollow" else "is-follow")

              onClick { _ =>
                if (!busyProperty.get) {
                  val successMessage =
                    if (isUnfollow) "Nutzer entfolgt."
                    else "Nutzer gefolgt."

                  busyProperty.set(true)

                  model.invokeFollowAction().onComplete {
                    case Success(_) =>
                      Viewport.notify(successMessage, Viewport.NotificationKind.Success)
                      busyProperty.set(false)

                    case Failure(error) =>
                      Api.logFailure("Follow action", error)
                      Viewport.notify("Follow-Aktion fehlgeschlagen.", Viewport.NotificationKind.Error)
                      busyProperty.set(false)
                  }
                }
              }
            }

            actionButton.element.disabled = busyProperty.get
            addDisposable(
              busyProperty.observe { busy =>
                actionButton.element.disabled = busy
              }
            )
          }
        }
      }
    }
  }
}

private object UserFollowAction {
  def action(model: User)(using Scope): UserFollowAction =
    CompositeSupport.buildComposite(new UserFollowAction(model))
}
