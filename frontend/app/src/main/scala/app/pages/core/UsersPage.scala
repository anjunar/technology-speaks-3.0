package app.pages.core

import app.domain.core.{Data, Media, User}
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.control.TableColumn.{cellFactory_=, cellValueFactory_=, column, prefWidth_=}
import jfx.control.TableView.{fixedCellSize, fixedCellSize_=, items, items_=, rowFactory, rowFactory_=, tableView}
import jfx.control.{TableCell, TableRow, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{Property, RemoteListProperty}
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import org.scalajs.dom.HTMLImageElement

import scala.concurrent.ExecutionContext

class UsersPage extends PageComposite("Users") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 50
  private val usersProperty: RemoteListProperty[Data[User], RemotePageQuery] =
    RemoteTableList.create[Data[User]](pageSize = pageSize) { (index, limit) =>
      User.list(index, limit)
    }

  override protected def compose(using DslContext): Unit = {
    classProperty += "users-page"

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

          val table = tableView[Data[User]] {
            items = usersProperty
            fixedCellSize = 64.0
            rowFactory = (_: TableView[Data[User]]) => new UserNavigationRow()

            column[Data[User], Media | Null]("Bild") {
              val current = summon[jfx.control.TableColumn[Data[User], Media | Null]]
              current.setPrefWidth(96.0)
              current.setCellValueFactory(features => features.value.data.image)
              current.setCellFactory(_ => new UserImageCell())
            }

            column[Data[User], String]("Nickname") {
              val current = summon[jfx.control.TableColumn[Data[User], String]]
              current.setPrefWidth(220.0)
              current.setCellValueFactory(features => features.value.data.nickName)
            }

            column[Data[User], String]("Vorname") {
              val current = summon[jfx.control.TableColumn[Data[User], String]]
              current.setPrefWidth(180.0)
              current.setCellValueFactory(features => {
                val info = features.value.data.info.get
                Property(if (info == null) "" else info.firstName.get)
              })
            }

            column[Data[User], String]("Nachname") {
              val current = summon[jfx.control.TableColumn[Data[User], String]]
              current.setPrefWidth(180.0)
              current.setCellValueFactory(features => {
                val info = features.value.data.info.get
                Property(if (info == null) "" else info.lastName.get)
              })
            }
          }

          table.classProperty += "users-page-table"
        }
      }
    }
  }
}

object UsersPage {
  def usersPage(init: UsersPage ?=> Unit = {}): UsersPage =
    CompositeSupport.buildPage(new UsersPage)(init)
}

private final class UserNavigationRow extends TableRow[Data[User]] {

  element.ondblclick = _ => {
    val item = getItem
    if (item != null && Option(item.data.id.get).exists(_.trim.nonEmpty)) {
      Navigation.navigate(s"/core/users/user/${item.data.id.get}")
    }
  }
}

private final class UserImageCell extends TableCell[Data[User], Media | Null] {

  private val icon = newElement("div")
  private val image = newElement("img").asInstanceOf[HTMLImageElement]

  icon.className = "material-icons"
  icon.textContent = "account_circle"
  icon.style.fontSize = "40px"

  image.style.width = "40px"
  image.style.height = "40px"
  image.style.borderRadius = "50%"
  image.style.setProperty("object-fit", "cover")
  image.style.display = "none"

  element.style.display = "flex"
  element.style.setProperty("align-items", "center")
  element.style.setProperty("justify-content", "center")
  element.appendChild(icon)
  element.appendChild(image)

  override protected def updateItem(item: Media | Null, empty: Boolean): Unit = {
    val isEmptyCell = empty || item == null
    if (isEmptyCell) {
      element.classList.add("jfx-table-cell-empty")
      image.src = ""
      image.style.display = "none"
      icon.style.display = "flex"
    } else {
      element.classList.remove("jfx-table-cell-empty")
      image.src = item.thumbnailLink()
      image.style.display = "block"
      icon.style.display = "none"
    }
  }
}
