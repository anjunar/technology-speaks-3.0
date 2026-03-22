package app.pages.core

import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Address, Data, User, UserInfo}
import app.domain.followers.{Group, GroupAssignmentRequest}
import app.support.{Api, AppJson, Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.control.Image.image
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.SubForm.subForm
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import jfx.virtual.virtualList

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class UserPage extends PageComposite("User", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val modelProperty: Property[Data[User]] = Property(new Data[User](new User()))
  private val groupsPageSize = 60
  private val availableGroupsProperty: RemoteListProperty[Group, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Group], Group](pageSize = groupsPageSize) { (index, limit) =>
      Group.list(index, limit)
    }(_.data)
  private val assignedGroupsProperty: ListProperty[Group] = ListProperty()
  private val contentProperty: Property[NodeComponent[? <: org.scalajs.dom.Node] | Null] = Property(null)

  def model(data: Data[User]): Unit = {
    modelProperty.set(data)
    loadGroups(data.data)
  }

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page"

    addDisposable(modelProperty.observe(data => contentProperty.set(UserPageContent.content(data.data, availableGroupsProperty, assignedGroupsProperty, saveGroups, refreshUser))))

    withDslContext {
      dynamicOutlet(contentProperty)
    }
  }

  private def loadGroups(user: User): Unit = {
    assignedGroupsProperty.clear()

    Navigation.linkByRel("groups", user.links).foreach { link =>
      Api.requestJson(link.method, Navigation.prefixedServiceUrl(link.url)).foreach { raw =>
        assignedGroupsProperty.setAll(deserializeGroups(raw))
      }
    }
  }

  private def saveGroups(user: User): Unit =
    Navigation.linkByRel("updateGroups", user.links).foreach { link =>
      val request = new GroupAssignmentRequest(assignedGroupsProperty.flatMap(group => Option(group.id.get).filter(_.trim.nonEmpty)).toJSArray)
      Api.requestJson(link.method, Navigation.prefixedServiceUrl(link.url), request)
    }

  private def refreshUser(saved: Data[User]): Unit =
    modelProperty.set(saved)

  private def deserializeGroups(raw: js.Any): Seq[Group] =
    if (raw == null || js.isUndefined(raw)) {
      Seq.empty
    } else if (js.Array.isArray(raw.asInstanceOf[js.Any])) {
      raw
        .asInstanceOf[js.Array[js.Any]]
        .iterator
        .collect {
          case value if value != null && !js.isUndefined(value) =>
            AppJson.mapper.deserialize(value.asInstanceOf[js.Dynamic]).asInstanceOf[Data[Group]].data
        }
        .toSeq
    } else {
      Seq.empty
    }
}

object UserPage {
  def userPage(init: UserPage ?=> Unit = {}): UserPage =
    CompositeSupport.buildPage(new UserPage)(init)
}

private final class UserPageContent(
  user: User,
  availableGroups: ListProperty[Group],
  assignedGroups: ListProperty[Group],
  saveGroups: User => Unit,
  refreshUser: Data[User] => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val infoEnabled: Property[Boolean] = Property(user.info.get != null)
  private val addressEnabled: Property[Boolean] = Property(user.address.get != null)
  private val infoModel: UserInfo = Option(user.info.get).getOrElse(new UserInfo())
  private val addressModel: Address = Option(user.address.get).getOrElse(new Address())

  user.info.set(infoModel)
  user.address.set(addressModel)

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      hbox {
        style {
          columnGap = "12px"
          height = "100%"
        }

        form(user) {
          onSubmit_= { _ =>
            user.info.set(if (infoEnabled.get) infoModel else null)
            user.address.set(if (addressEnabled.get) addressModel else null)

            val request =
              if (Option(user.id.get).exists(_.trim.nonEmpty)) user.update()
              else user.save()

            request.foreach(refreshUser)
          }

          style {
            flex = "1"
            minWidth = "0px"
          }

          vbox {
            style {
              rowGap = "12px"
            }

            hbox {
              style {
                columnGap = "12px"
                alignItems = "flex-start"
              }

              div {
                style {
                  width = "128px"
                  height = "128px"
                  display = "flex"
                  alignItems = "center"
                  justifyContent = "center"
                  backgroundColor = "var(--color-background-secondary)"
                  borderRadius = "12px"
                }

                Option(user.image.get) match {
                  case Some(media) =>
                    val imageView = image {
                      style {
                        width = "128px"
                        height = "128px"
                        borderRadius = "12px"
                      }
                    }
                    imageView.src = media.thumbnailLink()
                  case None =>
                    div {
                      classes = Seq("material-icons")
                      style {
                        fontSize = "96px"
                      }
                      text = "account_circle"
                    }
                }
              }

              vbox {
                style {
                  rowGap = "10px"
                  flex = "1"
                }

                inputContainer("Nick Name") {
                  input("nickName") {}
                }

                val infoSection = subForm[UserInfo]("info") {
                  inputContainer("Vorname") {
                    input("firstName") {}
                  }
                  inputContainer("Nachname") {
                    input("lastName") {}
                  }
                  inputContainer("Geburtsdatum") {
                    val birthDate = input("birthDate") {}
                    birthDate.element.`type` = "date"
                  }
                }

                val addressSection = subForm[Address]("address") {
                  inputContainer("Strasse") {
                    input("street") {}
                  }
                  inputContainer("Hausnummer") {
                    input("number") {}
                  }
                  inputContainer("Postleitzahl") {
                    input("zipCode") {}
                  }
                  inputContainer("Land") {
                    input("country") {}
                  }
                }

                addDisposable(infoEnabled.observe(enabled => infoSection.element.style.display = if (enabled) "block" else "none"))
                addDisposable(addressEnabled.observe(enabled => addressSection.element.style.display = if (enabled) "block" else "none"))
              }
            }

            hbox {
              style {
                justifyContent = "flex-end"
                columnGap = "10px"
              }

              button(if (infoEnabled.get) "Info entfernen" else "Info hinzufuegen") {
                buttonType_=("button")
                onClick { _ =>
                  infoEnabled.set(!infoEnabled.get)
                }
              }

              button(if (addressEnabled.get) "Adresse entfernen" else "Adresse hinzufuegen") {
                buttonType_=("button")
                onClick { _ =>
                  addressEnabled.set(!addressEnabled.get)
                }
              }

              button("Speichern") {
                classes = Seq("btn-secondary")
              }
            }
          }
        }

        UserActionsPanel.panel(user, availableGroups, assignedGroups, saveGroups)
      }
    }
  }
}

private object UserPageContent {
  def content(
    user: User,
    availableGroups: ListProperty[Group],
    assignedGroups: ListProperty[Group],
    saveGroups: User => Unit,
    refreshUser: Data[User] => Unit
  ): UserPageContent =
    CompositeSupport.buildComposite(new UserPageContent(user, availableGroups, assignedGroups, saveGroups, refreshUser))
}

private final class UserActionsPanel(
  user: User,
  availableGroups: ListProperty[Group],
  assignedGroups: ListProperty[Group],
  saveGroups: User => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      vbox {
        style {
          width = "320px"
          rowGap = "12px"
          height = "100%"
        }

        Navigation.linkByRel("follow", user.links).foreach { link =>
          button("Folgen") {
            buttonType_=("button")
            onClick(_ => Api.requestJson(link.method, Navigation.prefixedServiceUrl(link.url)))
          }
        }

        Navigation.linkByRel("unfollow", user.links).foreach { link =>
          button("Nicht mehr folgen") {
            buttonType_=("button")
            onClick(_ => Api.requestJson(link.method, Navigation.prefixedServiceUrl(link.url)))
          }
        }

        vbox {
          style {
            rowGap = "8px"
            flex = "1"
            minHeight = "0px"
          }

          div {
            style {
              fontWeight = "600"
            }
            text = "Gruppen"
          }

          div {
            style {
              flex = "1"
              minHeight = "0px"
            }

            virtualList(availableGroups, estimateHeightPx = 52, overscanPx = 120, prefetchItems = 40) { (group, _) =>
              if (group == null) {
                val card = loadingCard {}
                card.minHeight("44px")
                card
              } else {
                GroupToggle.item(group, assignedGroups)
              }
            }
          }

          button("Gruppen speichern") {
            buttonType_=("button")
            onClick(_ => saveGroups(user))
          }
        }
      }
    }
  }
}

private object UserActionsPanel {
  def panel(
    user: User,
    availableGroups: ListProperty[Group],
    assignedGroups: ListProperty[Group],
    saveGroups: User => Unit
  ): UserActionsPanel =
    CompositeSupport.buildComposite(new UserActionsPanel(user, availableGroups, assignedGroups, saveGroups))
}

private final class GroupToggle(group: Group, assignedGroups: ListProperty[Group]) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      val toggle = button(group.name.get) {
        buttonType_=("button")
        onClick { _ =>
          val index = assignedGroups.indexWhere(_.id.get == group.id.get)
          if (index >= 0) assignedGroups.remove(index)
          else assignedGroups += group
        }
      }

      addDisposable(
        assignedGroups.observe { _ =>
          val active = assignedGroups.exists(_.id.get == group.id.get)
          toggle.classProperty.setAll(if (active) Seq("btn-secondary") else Seq.empty)
        }
      )
    }
  }
}

private object GroupToggle {
  def item(group: Group, assignedGroups: ListProperty[Group]): GroupToggle =
    CompositeSupport.buildComposite(new GroupToggle(group, assignedGroups))
}
