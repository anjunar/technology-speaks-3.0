package app.pages.core

import app.domain.core.{Address, Data, Link, ManagedProperty, Schema, SchemaProperty, User, UserInfo, UserUpdated}
import app.domain.followers.{Group, RelationShip}
import app.services.ApplicationService
import app.support.Navigation.renderByRel
import app.support.{Api, Navigation}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.Link.link
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.ComboBox
import jfx.form.ComboBox.*
import jfx.form.Control.placeholder
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

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

class UserPage(val payload: Data[User]) extends PageComposite("User", pageResizable = false) {

  println(s"UserPage constructor - payload: $payload")
  println (s"UserPage constructor - payload.schema: ${payload.schema.entries}")
  println (s"UserPage constructor - payload.data: ${payload.data}")

  val model: User = payload.data

  override def pageWidth: Int = 1240
  override def pageHeight: Int = 760

  private given ExecutionContext = ExecutionContext.global

  private val infoDisabled = Property(model.info.get == null)
  private val addressDisabled = Property(model.address.get == null)
  private val visibilityCatalogProperty = Property(UserPage.VisibilityCatalog.empty)

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page"
    loadVisibilityCatalog()

    withDslContext {
      val service = inject[ApplicationService]

      hbox {
        classes = "user-page-shell"

        form(model) {
          classes = "user-page-form"

          onSubmit = { (event: Form[User]) =>

            model.update().onComplete {
              case Success(saved) =>
                service.messageBus.publish(new UserUpdated(saved))
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

                renderByRel("update", model.links) { () =>
                  div {
                    classes = "user-page-profile-card"

                    span {
                      classes = "user-page-section-eyebrow"
                      text = "Account"
                    }

                    link("/security/account") {
                      classes = "user-page-manage-link"
                      text = "Passwort und Zugang verwalten"
                    }
                  }
                }

                UserFollowAction.action(model)
              }

              vbox {
                classes = "user-page-details"

                div {
                  classes = "user-page-section"

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
                      button(infoToggleIcon(infoDisabled.get)) {
                        buttonType = "button"
                        classes = Seq("material-icons", "user-page-toggle")

                        val btn = summon[Button]
                        addDisposable(infoDisabled.observe(disabled => {
                          btn.textContent = infoToggleIcon(disabled)
                        }))

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

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Vorname") {
                          input("firstName") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-info-first-name", nestedProperty("info", "firstName"), visibilityCatalogProperty)
                    }

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Nachname") {
                          input("lastName") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-info-last-name", nestedProperty("info", "lastName"), visibilityCatalogProperty)
                    }

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Geburtsdatum") {
                          input("birthDate") {
                            classes = "user-page-input"
                            inputType = "date"
                          }
                        }
                      }

                      UserPage.accessControl("user-info-birth-date", nestedProperty("info", "birthDate"), visibilityCatalogProperty)
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
                      button(addressToggleIcon(addressDisabled.get)) {
                        buttonType = "button"
                        classes = Seq("material-icons", "user-page-toggle")

                        val btn = summon[Button]
                        addDisposable(addressDisabled.observe(disabled => {
                          btn.textContent = addressToggleIcon(disabled)
                        }))

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

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Strasse") {
                          input("street") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-address-street", nestedProperty("address", "street"), visibilityCatalogProperty)
                    }

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Hausnummer") {
                          input("number") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-address-number", nestedProperty("address", "number"), visibilityCatalogProperty)
                    }

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Postleitzahl") {
                          input("zipCode") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-address-zip-code", nestedProperty("address", "zipCode"), visibilityCatalogProperty)
                    }

                    hbox {
                      classes = "user-page-field-row"

                      div {
                        classes = "user-page-field-input"

                        inputContainer("Land") {
                          input("country") {
                            classes = "user-page-input"
                          }
                        }
                      }

                      UserPage.accessControl("user-address-country", nestedProperty("address", "country"), visibilityCatalogProperty)
                    }

                    editable = !addressDisabled.get
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


              // Add Delete User Button
              renderByRel("delete", model.links) { () =>
                val deleteLink = model.links.find(_.rel == "delete").orNull

                if (deleteLink != null) {
                  button("Benutzer löschen") {
                    buttonType = "button"
                    classes = Seq("user-page-delete-btn", "destructive-action")

                    onClick { _ =>
                      Api.request("/service" + deleteLink.url).delete(model).unit.onComplete {
                        case Success(_) =>
                          Viewport.notify("Benutzer erfolgreich gelöscht.", Viewport.NotificationKind.Success)
                        // Example: Navigation.goto("/users")

                        case Failure(e: ErrorResponseException) =>
                          Viewport.notify(s"Fehler beim Löschen des Benutzers: ${e.errors.mkString(", ")}", Viewport.NotificationKind.Error)

                        case Failure(error) =>
                          Api.logFailure("Delete user", error)
                          Viewport.notify("Fehler beim Löschen des Benutzers.", Viewport.NotificationKind.Error)
                      }
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

  private def nestedProperty(parentName: String, propertyName: String): SchemaProperty | Null =
    Option(payload.schema)
      .flatMap(schema => Option(schema.findProperty(parentName)))
      .flatMap(parent => Option(parent.schema))
      .flatMap(schema => Option(schema.findProperty(propertyName)))
      .orNull

  private def infoToggleIcon(disabled: Boolean): String =
    if (disabled) "person_add" else "person_remove"

  private def addressToggleIcon(disabled: Boolean): String =
    if (disabled) "add_location_alt" else "location_off"

  private def loadVisibilityCatalog(): Unit = {
    UserPage.loadVisibilityCatalog().onComplete {
      case Success(value) =>
        visibilityCatalogProperty.set(value)
      case Failure(error) =>
        Api.logFailure("User visibility catalog", error)
        Viewport.notify("Sichtbarkeitsoptionen konnten nicht geladen werden.", Viewport.NotificationKind.Error)
    }
  }
}

object UserPage {
  def userPage(payload: Data[User], init: UserPage ?=> Unit = {})(using Scope): UserPage =
    CompositeSupport.buildPage(new UserPage(payload))(init)

  private def accessControl(
    fieldKey: String,
    property: SchemaProperty | Null,
    visibilityCatalogProperty: Property[VisibilityCatalog]
  )(using Scope): ManagedPropertyAccessControl =
    CompositeSupport.buildComposite(new ManagedPropertyAccessControl(fieldKey, property, visibilityCatalogProperty))

  private def loadVisibilityCatalog()(using ExecutionContext): scala.concurrent.Future[VisibilityCatalog] = {
    RelationShip.list(0, 200).flatMap { relationShipTable =>
      Group.list(0, 200).map { groupTable =>
        buildVisibilityCatalog(relationShipTable.rows.iterator.map(_.data).toVector, groupTable.rows.iterator.map(_.data).toVector)
      }
    }
  }

  private def buildVisibilityCatalog(
    relationShips: Vector[RelationShip],
    groups: Vector[Group]
  ): VisibilityCatalog = {
    val followersById =
      relationShips
        .iterator
        .flatMap(relationShip => Option(relationShip.follower.get))
        .flatMap(user => userId(user).map(_ -> user))
        .toMap

    val directTargets =
      followersById.valuesIterator
        .toVector
        .sortBy(userDisplayName)
        .map(user =>
          VisibilityTarget(
            key = s"user:${user.id.get}",
            label = userDisplayName(user),
            kind = "user",
            users = Vector(user),
            user = user
          )
        )

    val groupMembersById =
      relationShips.foldLeft(Map.empty[UUID, Vector[User]]) { (acc, relationShip) =>
        val follower = Option(relationShip.follower.get)
        relationShip.groups.iterator.foldLeft(acc) { (inner, group) =>
          val updated =
            for {
              groupId <- Option(group.id.get)
              user <- follower
            } yield inner.updated(groupId, inner.getOrElse(groupId, Vector.empty) :+ user)

          updated.getOrElse(inner)
        }
      }

    val distinctGroups =
      (groups ++ relationShips.flatMap(_.groups.iterator))
        .flatMap(group => Option(group.id.get).map(_ -> group))
        .groupBy(_._1)
        .values
        .flatMap(_.lastOption.map(_._2))
        .toVector
        .sortBy(group => Option(group.name.get).map(_.trim.toLowerCase).getOrElse(""))

    val groupTargets =
      distinctGroups.map { group =>
        val members = Option(group.id.get).flatMap(groupMembersById.get).getOrElse(Vector.empty).distinctBy(userKey)

        VisibilityTarget(
          key = s"group:${group.id.get}",
          label = Option(group.name.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Namen)"),
          kind = "group",
          users = members,
          group = group
        )
      }

    VisibilityCatalog(
      allTarget = VisibilityTarget("all", "Alle", "all", Vector.empty),
      groupTargets = groupTargets,
      userTargets = directTargets
    )
  }

  private def userDisplayName(user: User): String =
    Option(user.nickName.get).filter(_.trim.nonEmpty).getOrElse("Benutzer")

  private def userId(user: User): Option[UUID] =
    Option(user.id.get)

  private def userKey(user: User): String =
    userId(user).map(_.toString).getOrElse(userDisplayName(user))

  final case class VisibilityTarget(
    key: String,
    label: String,
    kind: String,
    users: Vector[User],
    user: User | Null = null,
    group: Group | Null = null
  )

  final case class VisibilityCatalog(
    allTarget: VisibilityTarget,
    groupTargets: Vector[VisibilityTarget],
    userTargets: Vector[VisibilityTarget]
  ) {
    val targets: Vector[VisibilityTarget] =
      Vector(allTarget) ++ groupTargets ++ userTargets

    def selectedTargets(managedProperty: ManagedProperty): Vector[VisibilityTarget] = {
      val selectedGroupIds =
        managedProperty.groups.iterator
          .flatMap(group => Option(group.id.get))
          .toSet

      val selectedUserIds =
        managedProperty.users.iterator
          .flatMap(userId)
          .toSet

      val selectedGroups =
        groupTargets.filter(target =>
          Option(target.group).flatMap(group => Option(group.id.get)).exists(selectedGroupIds.contains)
        )

      val directUsers =
        userTargets.filter(target =>
          Option(target.user).flatMap(userId).exists(selectedUserIds.contains)
        )

      val all =
        if (managedProperty.visibleForAll.get) Vector(allTarget)
        else Vector.empty

      all ++ selectedGroups ++ directUsers
    }

    def resolveDirectUsers(selected: Seq[VisibilityTarget]): Vector[User] =
      selected
        .iterator
        .flatMap(target => Option(target.user))
        .foldLeft(Vector.empty[User]) { (acc, user) =>
          if (acc.exists(existing => userId(existing) == userId(user))) acc
          else acc :+ user
        }

    def resolveGroups(selected: Seq[VisibilityTarget]): Vector[Group] =
      selected
        .iterator
        .flatMap(target => Option(target.group))
        .foldLeft(Vector.empty[Group]) { (acc, group) =>
          if (acc.exists(existing => Option(existing.id.get) == Option(group.id.get))) acc
          else acc :+ group
        }
  }

  object VisibilityCatalog {
    val empty: VisibilityCatalog = VisibilityCatalog(
      allTarget = VisibilityTarget("all", "Alle", "all", Vector.empty),
      groupTargets = Vector.empty,
      userTargets = Vector.empty
    )
  }
}

private final class ManagedPropertyAccessControl(
  fieldKey: String,
  property: SchemaProperty | Null,
  visibilityCatalogProperty: Property[UserPage.VisibilityCatalog]
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val availableTargets = ListProperty[UserPage.VisibilityTarget]()
  private val managedPropertyProperty = Property[ManagedProperty | Null](null)
  private val busyProperty = Property(false)

  private var selectorRef: ComboBox[UserPage.VisibilityTarget] | Null = null
  private var syncingSelection = false

  private val propertyLink =
    Option(property)
      .toSeq
      .flatMap(_.links.iterator)
      .find(_.rel == "property")
      .orNull

  override protected def compose(using DslContext): Unit = {
    classProperty += "user-page-access-control"

    if (propertyLink == null) {
      element.style.display = "none"
      return
    }

    addDisposable(
      visibilityCatalogProperty.observe { catalog =>
        availableTargets.setAll(catalog.targets)
        Option(managedPropertyProperty.get).foreach(syncSelection(_, catalog))
      }
    )

    withDslContext {
      div {
        classes = "user-page-access-shell"

        div {
          classes = "user-page-access-label"
          text = "Sichtbar fuer"
        }

        selectorRef = comboBox[UserPage.VisibilityTarget](fieldKey, standalone = true) {
          ComboBox.items = availableTargets
          placeholder = "Sichtbarkeit"
          identityBy = { (target: UserPage.VisibilityTarget) => target.key }
          multipleSelection = true
          rowHeightPx = 40.0
          dropdownHeightPx = 220.0

          valueRenderer = {
            div {
              classes = "user-page-access-value"
              text = selectedValueText(comboSelectedItems[UserPage.VisibilityTarget].iterator.toVector)
            }
          }

          itemRenderer = {
            val option = comboItem[UserPage.VisibilityTarget]
            val selected = comboItemSelected

            hbox {
              classes = "user-page-access-option"

              div {
                classes = Seq(
                  "material-icons",
                  "user-page-access-option-icon",
                  s"is-${option.kind}",
                  if (selected) "is-selected" else "is-unselected"
                )
                text =
                  if (selected) "check_circle"
                  else option.kind match {
                    case "group" => "groups"
                    case "all"   => "public"
                    case _       => "person"
                  }
              }

              div {
                classes = "user-page-access-option-text"
                text = option.label
              }
            }
          }

          dropdownFooterRenderer = {
            link("/followers/groups") {
              classes = Seq("jfx-combo-box__footer-link", "user-page-manage-groups-link")
              text = "Gruppen verwalten"
              summon[jfx.control.Link].element.setAttribute("data-jfx-combo-box-action", "true")
            }
          }
        }
      }
    }

    addDisposable(
      selectorRef.nn.valueProperty.observeWithoutInitial { values =>
        if (!syncingSelection) {
          persistSelection(values.iterator.toVector)
        }
      }
    )

    addDisposable(
      busyProperty.observe { busy =>
        val elementRef = selectorRef.nn.element
        elementRef.style.pointerEvents = if (busy) "none" else "auto"
        elementRef.style.opacity = if (busy) "0.7" else "1"
      }
    )

    loadManagedProperty()
  }

  private def loadManagedProperty(): Unit = {
    busyProperty.set(true)

    Api.link(propertyLink).invoke.read[ManagedProperty].onComplete {
      case Success(value) =>
        managedPropertyProperty.set(value)
        syncSelection(value, visibilityCatalogProperty.get)
        busyProperty.set(false)

      case Failure(error) =>
        Api.logFailure(s"Managed property $fieldKey", error)
        element.style.display = "none"
        busyProperty.set(false)
    }
  }

  private def persistSelection(selectedTargets: Vector[UserPage.VisibilityTarget]): Unit = {
    val managedProperty = managedPropertyProperty.get
    if (managedProperty == null || busyProperty.get) {
      Option(managedProperty).foreach(syncSelection(_, visibilityCatalogProperty.get))
      return
    }

    val previousVisibleForAll = managedProperty.visibleForAll.get
    val previousUsers = managedProperty.users.iterator.toVector
    val previousGroups = managedProperty.groups.iterator.toVector
    val visibleForAll = selectedTargets.exists(_.kind == "all")
    val resolvedUsers = visibilityCatalogProperty.get.resolveDirectUsers(selectedTargets)
    val resolvedGroups = visibilityCatalogProperty.get.resolveGroups(selectedTargets)

    managedProperty.visibleForAll.set(visibleForAll)
    managedProperty.users.setAll(resolvedUsers)
    managedProperty.groups.setAll(resolvedGroups)

    busyProperty.set(true)

    managedProperty.updateFromLink().onComplete {
      case Success(updated) =>
        managedPropertyProperty.set(updated)
        syncSelection(updated, visibilityCatalogProperty.get)
        busyProperty.set(false)

      case Failure(error) =>
        managedProperty.visibleForAll.set(previousVisibleForAll)
        managedProperty.users.setAll(previousUsers)
        managedProperty.groups.setAll(previousGroups)
        syncSelection(managedProperty, visibilityCatalogProperty.get)
        Api.logFailure(s"Managed property update $fieldKey", error)
        Viewport.notify("Sichtbarkeit konnte nicht aktualisiert werden.", Viewport.NotificationKind.Error)
        busyProperty.set(false)
    }
  }

  private def syncSelection(managedProperty: ManagedProperty, catalog: UserPage.VisibilityCatalog): Unit = {
    syncingSelection = true
    try selectorRef.nn.valueProperty.setAll(catalog.selectedTargets(managedProperty))
    finally syncingSelection = false
  }

  private def selectedValueText(selected: Vector[UserPage.VisibilityTarget]): String = {
    if (selected.isEmpty) {
      "Nur du"
    } else {
      selected.map(_.label).mkString(", ")
    }
  }
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
