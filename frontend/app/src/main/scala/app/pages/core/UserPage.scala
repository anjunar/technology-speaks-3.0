package app.pages.core

import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Address, Data, User, UserInfo}
import app.domain.followers.{Group, GroupAssignmentRequest}
import app.support.Navigation.renderByRel
import app.support.{Api, AppJson, Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, buttonType_=, onClick}
import jfx.control.Image.image
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
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
import jfx.statement.DynamicOutlet.dynamicOutlet

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success}

class UserPage(val model: User) extends PageComposite("User", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val availableGroupsProperty: RemoteListProperty[Group, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Group], Group](pageSize = 60) { (index, limit) =>
      Group.list(index, limit)
    }(_.data)
  private val assignedGroupsProperty: ListProperty[Group] = ListProperty()

  private val infoDisabled = Property(model.info.get == null)
  private val addressDisabled = Property(model.address.get == null)

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page"

    withDslContext {

      hbox {

        form(model) {

          onSubmit = { (event : Form[User]) =>

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

            hbox {
              imageCropper("image") {

                style {
                  width = "512px"
                  height = "512px"
                }

                aspectRatio = 1.0
                outputType = "image/jpeg"
                outputQuality = 0.92
                outputMaxWidth = 512
                outputMaxHeight = 512

              }

              div {

                style {
                  width = "300px"
                }

                inputContainer("Nickname") {
                  input("nickName")
                }

                hbox {

                  style {
                    alignItems = "flex-start"
                  }

                  subForm[UserInfo]("info") {

                    style {
                      flex = "1"
                    }

                    factory = () => new UserInfo()

                    addDisposable(infoDisabled.observeWithoutInitial(disabled => {
                      editable = ! disabled
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
                      classes = "material-icons"

                      onClick { _ =>
                        infoDisabled.set(! infoDisabled.get)
                      }
                    }
                  }


                }

                hbox {
                  subForm[Address]("address") {

                    style {
                      flex = "1"
                    }

                    factory = () => new Address()

                    addDisposable(addressDisabled.observeWithoutInitial(disabled => {
                      editable = ! disabled

                      if (disabled) {
                        SubForm.clearForm()
                      } else {
                        SubForm.newInstance()
                      }
                    }))

                    inputContainer("Straße") {
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
                      classes = "material-icons"

                      onClick { _ =>
                        addressDisabled.set(!addressDisabled.get)
                      }
                    }
                  }

                }

              }

            }

            hbox {

              style {
                justifyContent = "flex-end"
                columnGap = "10px"
              }

              renderByRel("update", model.links) { () =>
                button("Speichern")
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