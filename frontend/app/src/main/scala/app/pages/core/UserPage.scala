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
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UserPage(val model: User) extends PageComposite("User", pageResizable = false) {

  override def pageWidth: Int = 1040
  override def pageHeight: Int = 860

  private given ExecutionContext = ExecutionContext.global

  private val infoDisabled = Property(model.info.get == null)
  private val addressDisabled = Property(model.address.get == null)

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page"

    withDslContext {
      hbox {
        classes = "user-page-shell"

        form(model) {
          classes = "user-page-form"

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

          vbox {
            classes = "user-page-content"

            vbox {
              classes = "user-page-hero"

              div {
                classes = "user-page-hero-copy"

                span {
                  classes = "user-page-eyebrow"
                  text = "Profil"
                }

                span {
                  classes = "user-page-title"
                  text = "Praesenz und Beziehung"
                }

                span {
                  classes = "user-page-subtitle"
                  text = "Profil, Zugehoerigkeit und grundlegende Angaben in einer ruhigen Ansicht."
                }
              }
            }

            hbox {
              classes = "user-page-main"

              vbox {
                classes = "user-page-profile"

                imageCropper("image") {
                  classes = "user-page-avatar"

                  aspectRatio = 1.0
                  outputType = "image/jpeg"
                  outputQuality = 0.92
                  outputMaxWidth = 512
                  outputMaxHeight = 512
                }

                div {
                  classes = "user-page-profile-card"

                  span {
                    classes = "user-page-section-eyebrow"
                    text = "Profilname"
                  }

                  inputContainer("Nickname") {
                    input("nickName") {
                      classes = "user-page-input"
                    }
                  }
                }

                UserFollowAction.action(model)
              }

              vbox {
                classes = "user-page-details"

                div {
                  classes = "user-page-section"

                  hbox {
                    classes = "user-page-section-header"

                    div {
                      classes = "user-page-section-copy"

                      span {
                        classes = "user-page-section-eyebrow"
                        text = "Identitaet"
                      }

                      span {
                        classes = "user-page-section-title"
                        text = "Persoenliche Angaben"
                      }
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

                  subForm[UserInfo]("info") {

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
                      input("firstName") {
                        classes = "user-page-input"
                      }
                    }

                    inputContainer("Nachname") {
                      input("lastName") {
                        classes = "user-page-input"
                      }
                    }

                    inputContainer("Geburtsdatum") {
                      input("birthDate") {
                        classes = "user-page-input"
                        inputType = "date"
                      }
                    }

                    editable = !infoDisabled.get
                  }
                }

                div {
                  classes = "user-page-section"

                  hbox {
                    classes = "user-page-section-header"

                    div {
                      classes = "user-page-section-copy"

                      span {
                        classes = "user-page-section-eyebrow"
                        text = "Ort"
                      }

                      span {
                        classes = "user-page-section-title"
                        text = "Adresse und Herkunft"
                      }
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

                  subForm[Address]("address") {

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
                      input("street") {
                        classes = "user-page-input"
                      }
                    }

                    inputContainer("Hausnummer") {
                      input("number") {
                        classes = "user-page-input"
                      }
                    }

                    inputContainer("Postleitzahl") {
                      input("zipCode") {
                        classes = "user-page-input"
                      }
                    }

                    inputContainer("Land") {
                      input("country") {
                        classes = "user-page-input"
                      }
                    }

                    editable = !addressDisabled.get
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
