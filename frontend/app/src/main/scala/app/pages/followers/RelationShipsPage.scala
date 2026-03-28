package app.pages.followers

import app.domain.core.{Data, MediaHelper, User}
import app.domain.followers.{Group, RelationShip}
import app.support.{Api, Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.control.Image.{image, srcProperty}
import jfx.control.Link.link
import jfx.control.TableColumn.column
import jfx.control.TableView.{fixedCellSize, items, rowFactory, tableView}
import jfx.control.{TableCell, TableColumn, TableRow, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.domain.Media
import jfx.dsl.*
import jfx.form.ComboBox
import jfx.form.ComboBox.*
import jfx.form.Control.placeholder
import jfx.form.Input.{input, inputType, stringValueProperty}
import jfx.layout.Div.div
import jfx.layout.HBox
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.statement.Conditional.{conditional, elseDo, thenDo}

import scala.concurrent.ExecutionContext
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.{Failure, Success}

class RelationShipsPage(
  val relationShipsProperty: RemoteListProperty[Data[RelationShip], RemotePageQuery],
  searchQuery: Property[String],
  selectedGroups: ListProperty[Group]
) extends PageComposite("Following") {

  override def pageWidth: Int = 1040
  override def pageHeight: Int = 860

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 200
  private var pendingReload: SetTimeoutHandle | Null = null
  private val availableGroupsProperty: RemoteListProperty[Group, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Group], Group](pageSize = pageSize) { (index, limit) =>
      Group.list(index, limit)
    }(_.data)

  addDisposable(
    searchQuery.observeWithoutInitial { _ =>
      scheduleReload()
    }
  )
  addDisposable(
    selectedGroups.observeWithoutInitial { _ =>
      scheduleReload()
    }
  )
  addDisposable(() => cancelScheduledReload())

  override protected def compose(using DslContext): Unit = {
    classProperty += "relation-ships-page"
    RemoteTableList.reloadFirstPage(relationShipsProperty, pageSize = pageSize)
    RemoteTableList.reloadFirstPage(availableGroupsProperty, pageSize = pageSize)

    withDslContext {
      vbox {
        classes = "relation-ships-page__layout"

        vbox {
          classes = "relation-ships-page__hero"

          div {
            classes = "relation-ships-page__hero-copy"

            span {
              classes = "relation-ships-page__eyebrow"
              text = "Beziehungen"
            }

            span {
              classes = "relation-ships-page__title"
              text = "Folgen und Gruppen"
            }

            span {
              classes = "relation-ships-page__subtitle"
              text = "Ordne die Profile, denen du folgst, in Gruppen ein und navigiere direkt weiter."
            }
          }
        }

        div {
          classes = "relation-ships-page__panel"
          style {
            flex = "1"
            minHeight = "0px"
          }

          vbox {
            classes = "relation-ships-page__panel-copy"

            span {
              classes = "relation-ships-page__panel-eyebrow"
              text = "Netzwerk"
            }

            span {
              classes = "relation-ships-page__panel-title"
              text = "Deine Verbindungen"
            }
          }

          hbox {
            classes = "relation-ships-page__filters"

            div {
              classes = "relation-ships-page__search"

              span {
                classes = "material-icons"
                text = "search"
              }

              input("search") {
                placeholder = "Nach Nickname, Vorname oder Nachname suchen..."
                inputType = "search"
                subscribeBidirectional(searchQuery, stringValueProperty)
              }
            }

            div {
              classes = "relation-ships-page__group-filter"

              val groupFilterRef = comboBox[Group]("relationshipGroupFilter", standalone = true) {
                ComboBox.items = availableGroupsProperty
                placeholder = "Alle Gruppen"
                identityBy = { (group: Group) => Option(group.id.get).getOrElse(group) }
                multipleSelection = true
                rowHeightPx = 40.0
                dropdownHeightPx = 220.0

                valueRenderer = {
                  div {
                    classes = "relation-ships-page__group-filter-value"
                    text = RelationShipsPage.groupFilterValue(comboSelectedItems[Group].iterator)
                  }
                }

                itemRenderer = {
                  val group = comboItem[Group]
                  val selected = comboItemSelected

                  hbox {
                    classes = "relation-ships-page__group-filter-option"

                    div {
                      classes = Seq(
                        "material-icons",
                        "relation-ships-page__group-filter-option-icon",
                        if (selected) "is-selected" else "is-unselected"
                      )
                      text = if (selected) "check_circle" else "radio_button_unchecked"
                    }

                    div {
                      classes = "relation-ships-page__group-filter-option-text"
                      text = Option(group.name.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Namen)")
                    }
                  }
                }
              }

              addDisposable(ListProperty.subscribeBidirectional(groupFilterRef.valueProperty, selectedGroups))
            }
          }

          div {
            classes = "relation-ships-page__table-shell"
            style {
              flex = "1"
              minHeight = "0px"
            }

            val table = tableView[Data[RelationShip]] {
              items = relationShipsProperty
              fixedCellSize = 76.0
              rowFactory = (_: TableView[Data[RelationShip]]) => new RelationShipNavigationRow()

              column[Data[RelationShip], Media | Null]("Bild") {
                val current = summon[TableColumn[Data[RelationShip], Media | Null]]
                current.setPrefWidth(96.0)
                current.setCellValueFactory(features => RelationShipsPage.followerImage(features.value))
                current.setCellFactory(_ => new RelationShipImageCell())
              }

              column[Data[RelationShip], String]("Nickname") {
                val current = summon[TableColumn[Data[RelationShip], String]]
                current.setPrefWidth(200.0)
                current.setCellValueFactory(features => Property(RelationShipsPage.nickName(features.value)))
              }

              column[Data[RelationShip], String]("Vorname") {
                val current = summon[TableColumn[Data[RelationShip], String]]
                current.setPrefWidth(150.0)
                current.setCellValueFactory(features => Property(RelationShipsPage.firstName(features.value)))
              }

              column[Data[RelationShip], String]("Nachname") {
                val current = summon[TableColumn[Data[RelationShip], String]]
                current.setPrefWidth(150.0)
                current.setCellValueFactory(features => Property(RelationShipsPage.lastName(features.value)))
              }

              column[Data[RelationShip], Data[RelationShip]]("Gruppen") {
                val current = summon[TableColumn[Data[RelationShip], Data[RelationShip]]]
                current.setPrefWidth(260.0)
                current.setCellValueFactory(features => Property(features.value))
                current.setCellFactory(_ => new RelationShipGroupsCell(availableGroupsProperty, reloadRelationShips))
              }
            }

            table.classProperty += "relation-ship-page-table"
          }
        }
      }
    }
  }

  private def scheduleReload(): Unit = {
    cancelScheduledReload()
    pendingReload = setTimeout(250) {
      reloadRelationShips()
    }
  }

  private def reloadRelationShips(): Unit =
    RemoteTableList.reloadFirstPage(relationShipsProperty, pageSize = pageSize)

  private def cancelScheduledReload(): Unit =
    if (pendingReload != null) {
      clearTimeout(pendingReload.nn)
      pendingReload = null
    }
}

object RelationShipsPage {
  def relationShipsPage(
    relationShipsProperty: RemoteListProperty[Data[RelationShip], RemotePageQuery],
    searchQuery: Property[String],
    selectedGroups: ListProperty[Group]
  )(init: RelationShipsPage ?=> Unit = {})(using Scope): RelationShipsPage =
    CompositeSupport.buildPage(new RelationShipsPage(relationShipsProperty, searchQuery, selectedGroups))(init)

  private def follower(data: Data[RelationShip]): User | Null =
    data.data.follower.get

  private def followerImage(data: Data[RelationShip]): Property[Media | Null] = {
    val currentFollower = follower(data)
    if (currentFollower == null) Property(null)
    else currentFollower.image
  }

  private def nickName(data: Data[RelationShip]): String = {
    val currentFollower = follower(data)
    if (currentFollower == null) "User"
    else Option(currentFollower.nickName.get).filter(_.trim.nonEmpty).getOrElse("User")
  }

  private def firstName(data: Data[RelationShip]): String =
    Option(follower(data))
      .flatMap(user => Option(user.info.get))
      .flatMap(info => Option(info.firstName.get))
      .getOrElse("")

  private def lastName(data: Data[RelationShip]): String =
    Option(follower(data))
      .flatMap(user => Option(user.info.get))
      .flatMap(info => Option(info.lastName.get))
      .getOrElse("")

  private def groupNames(data: Data[RelationShip]): String =
    groupNames(data.data.groups.iterator)

  def groupNames(groups: IterableOnce[Group]): String = {
    val values =
      groups.iterator
        .flatMap(group => Option(group.name.get).map(_.trim).filter(_.nonEmpty))
        .toVector

    if (values.isEmpty) "Keine Gruppe"
    else values.mkString(", ")
  }

  def groupFilterValue(groups: IterableOnce[Group]): String = {
    val values = groups.iterator.toVector
    if (values.isEmpty) "Alle Gruppen"
    else groupNames(values)
  }
}

private final class RelationShipNavigationRow extends TableRow[Data[RelationShip]] {

  element.ondblclick = _ => {
    val item = getItem
    val follower = if (item == null) null else item.data.follower.get
    if (follower != null && follower.id.get != null) {
      Navigation.navigate(s"/core/users/user/${follower.id.get}")
    }
  }
}

private final class RelationShipImageCell extends TableCell[Data[RelationShip], Media | Null] {

  val imageSource = Property[String](null)
  val imageVisible = Property[Boolean](false)

  val wrapper: HBox = hbox {
    classes = "relation-ships-page__avatar-cell"

    style {
      alignItems = "center"
      justifyContent = "center"
    }

    conditional(imageVisible) {

      thenDo {
        image {

          Property.subscribeBidirectional(imageSource, srcProperty)

          style {
            width = "40px"
            height = "40px"
            borderRadius = "50%"
          }
        }
      }

      elseDo {
        div {
          classes = Seq("material-icons", "relation-ships-page__avatar-fallback")
          text = "account_circle"
        }
      }

    }

  }

  wrapper.onMount()
  element.appendChild(wrapper.element)
  addDisposable(() => wrapper.dispose())

  override protected def updateItem(item: Media | Null, empty: Boolean): Unit = {
    val isEmptyCell = empty || item == null
    if (isEmptyCell) {
      element.classList.add("jfx-table-cell-empty")
      imageVisible.set(false)
      imageSource.set(null)
    } else {
      element.classList.remove("jfx-table-cell-empty")
      imageVisible.set(true)
      imageSource.set(MediaHelper.thumbnailLink(item))
    }
  }
}

private final class RelationShipGroupsCell(
  availableGroups: RemoteListProperty[Group, RemotePageQuery],
  onGroupsUpdated: () => Unit
)
    extends TableCell[Data[RelationShip], Data[RelationShip]] {

  private given ExecutionContext = ExecutionContext.global

  private var currentRelationShip: Data[RelationShip] | Null = null
  private var selectorRef: ComboBox[Group] | Null = null
  private var syncingSelection = false
  private var activeRequestRowId: String | Null = null

  private val wrapper = div {
    classes = "relation-ship-groups-editor"

    selectorRef = comboBox[Group]("relationGroups", standalone = true) {
      placeholder = "Keine Gruppe"

      ComboBox.items = availableGroups
      identityBy = { (group: Group) => Option(group.id.get).getOrElse(group) }
      multipleSelection = true
      rowHeightPx = 40.0
      dropdownHeightPx = 124.0
      valueRenderer = {
        div {
          classes = "relation-ship-groups-value"
          text = RelationShipsPage.groupNames(comboSelectedItems[Group].iterator)
        }
      }

      itemRenderer = {
        val group = comboItem[Group]
        val selected = comboItemSelected

        hbox {
          classes = "relation-ship-group-option"

          div {
            classes = Seq(
              "material-icons",
              "relation-ship-group-option-icon",
              if (selected) "is-selected" else "is-unselected"
            )
            text = if (selected) "check_circle" else "radio_button_unchecked"
          }

          div {
            classes = "relation-ship-group-option-text"
            text = Option(group.name.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Namen)")
          }
        }
      }

      dropdownFooterRenderer = {
        link("/followers/groups") {
          classes = Seq("jfx-combo-box__footer-link", "relation-ship-manage-groups-link")
          text = "Gruppen verwalten"
          summon[jfx.control.Link].element.setAttribute("data-jfx-combo-box-action", "true")
        }
      }
    }
  }

  wrapper.onMount()
  element.appendChild(wrapper.element)
  addDisposable(() => wrapper.dispose())

  addDisposable(
    selectorRef.nn.valueProperty.observeWithoutInitial { values =>
      if (!syncingSelection) {
        val relationShip = currentRelationShip
        if (relationShip != null) {
          persistSelection(relationShip, values.iterator.toVector)
        }
      }
    }
  )

  override protected def updateItem(item: Data[RelationShip] | Null, empty: Boolean): Unit = {
    val isEmptyCell = empty || item == null
    if (isEmptyCell) {
      currentRelationShip = null
      element.classList.add("jfx-table-cell-empty")
      wrapper.element.style.display = "none"
      syncSelection(Vector.empty)
    } else {
      currentRelationShip = item
      element.classList.remove("jfx-table-cell-empty")
      wrapper.element.style.display = "flex"
      syncSelection(item.data.groups.iterator.toVector)
    }

    applyBusyState()
  }

  private def syncSelection(groups: Seq[Group]): Unit = {
    syncingSelection = true
    try selectorRef.nn.valueProperty.setAll(groups)
    finally syncingSelection = false
  }

  private def persistSelection(relationShip: Data[RelationShip], groups: Vector[Group]): Unit = {
    val relationShipId = rowId(relationShip)
    if (relationShipId == null || activeRequestRowId != null) {
      syncSelection(relationShip.data.groups.iterator.toVector)
      return
    }

    activeRequestRowId = relationShipId
    applyBusyState()

    relationShip.data.updateGroups(groups).onComplete {
      case Success(_) =>
        if (activeRequestRowId == relationShipId) {
          activeRequestRowId = null
        }
        if (currentRelationShip eq relationShip) {
          syncSelection(relationShip.data.groups.iterator.toVector)
        }
        onGroupsUpdated()
        Option(getTableView).foreach(_.refresh())
        applyBusyState()

      case Failure(error) =>
        if (activeRequestRowId == relationShipId) {
          activeRequestRowId = null
        }
        Api.logFailure("RelationShip groups", error)
        Viewport.notify("Gruppen konnten nicht aktualisiert werden.", Viewport.NotificationKind.Error)
        if (currentRelationShip eq relationShip) {
          syncSelection(relationShip.data.groups.iterator.toVector)
        }
        applyBusyState()
    }
  }

  private def applyBusyState(): Unit = {
    val isBusyForCurrentRow =
      activeRequestRowId != null &&
        currentRelationShip != null &&
        activeRequestRowId == rowId(currentRelationShip.nn)

    val selector = selectorRef.nn.element
    selector.style.pointerEvents = if (isBusyForCurrentRow) "none" else "auto"
    selector.style.opacity = if (isBusyForCurrentRow) "0.7" else "1"
  }

  private def rowId(relationShip: Data[RelationShip]): String | Null =
    Option(relationShip.data.id.get).map(_.toString).orNull
}
