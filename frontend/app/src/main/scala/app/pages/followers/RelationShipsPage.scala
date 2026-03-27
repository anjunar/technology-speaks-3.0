package app.pages.followers

import app.domain.core.{Data, MediaHelper, User}
import app.domain.followers.RelationShip
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.control.Image.{image, srcProperty}
import jfx.control.TableColumn.column
import jfx.control.TableView.{fixedCellSize, items, rowFactory, tableView}
import jfx.control.{TableCell, TableRow, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{Property, RemoteListProperty}
import jfx.domain.Media
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.HBox
import jfx.layout.VBox.vbox
import jfx.statement.Conditional.{conditional, elseDo, thenDo}

import scala.concurrent.ExecutionContext

class RelationShipsPage extends PageComposite("Following") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 200
  private val relationShipsProperty: RemoteListProperty[Data[RelationShip], RemotePageQuery] =
    RemoteTableList.create[Data[RelationShip]](pageSize = pageSize) { (index, limit) =>
      RelationShip.list(index, limit)
    }

  override protected def compose(using DslContext): Unit = {
    classProperty += "relation-ships-page"
    RemoteTableList.reloadFirstPage(relationShipsProperty, pageSize = pageSize)

    withDslContext {
      vbox {
        style {
          setProperty("height", "100%")
          padding = "12px"
          boxSizing = "border-box"
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          val table = tableView[Data[RelationShip]] {
            items = relationShipsProperty
            fixedCellSize = 64.0
            rowFactory = (_: TableView[Data[RelationShip]]) => new RelationShipNavigationRow()

            column[Data[RelationShip], Media | Null]("Bild") {
              val current = summon[jfx.control.TableColumn[Data[RelationShip], Media | Null]]
              current.setPrefWidth(96.0)
              current.setCellValueFactory(features => RelationShipsPage.followerImage(features.value))
              current.setCellFactory(_ => new RelationShipImageCell())
            }

            column[Data[RelationShip], String]("Nickname") {
              val current = summon[jfx.control.TableColumn[Data[RelationShip], String]]
              current.setPrefWidth(220.0)
              current.setCellValueFactory(features => Property(RelationShipsPage.nickName(features.value)))
            }

            column[Data[RelationShip], String]("Vorname") {
              val current = summon[jfx.control.TableColumn[Data[RelationShip], String]]
              current.setPrefWidth(180.0)
              current.setCellValueFactory(features => Property(RelationShipsPage.firstName(features.value)))
            }

            column[Data[RelationShip], String]("Nachname") {
              val current = summon[jfx.control.TableColumn[Data[RelationShip], String]]
              current.setPrefWidth(180.0)
              current.setCellValueFactory(features => Property(RelationShipsPage.lastName(features.value)))
            }

            column[Data[RelationShip], String]("Gruppe") {
              val current = summon[jfx.control.TableColumn[Data[RelationShip], String]]
              current.setPrefWidth(240.0)
              current.setCellValueFactory(features => Property(RelationShipsPage.groupNames(features.value)))
            }
          }

          table.classProperty += "relation-ship-page-table"
        }
      }
    }
  }
}

object RelationShipsPage {
  def relationShipsPage(init: RelationShipsPage ?=> Unit = {})(using Scope): RelationShipsPage =
    CompositeSupport.buildPage(new RelationShipsPage)(init)

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

  private def groupNames(data: Data[RelationShip]): String = {
    val groups = data.data.groups.map(_.name.get).filter(name => name != null && name.trim.nonEmpty)
    if (groups.isEmpty) "Keine Gruppe" else groups.mkString(", ")
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
          classes = "material-icons"
          text = "account_circle"
          style {
            fontSize = "40px"
          }
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
