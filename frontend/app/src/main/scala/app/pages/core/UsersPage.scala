package app.pages.core

import app.domain.core.{Data, MediaHelper, User}
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.control.TableColumn.{cellFactory, cellValueFactory, column, prefWidth}
import jfx.control.TableView.{fixedCellSize, items, rowFactory, tableView}
import jfx.control.Image.{image, srcProperty}
import jfx.layout.HBox.hbox
import jfx.control.{TableCell, TableRow, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.domain.Media
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox
import jfx.layout.VBox.vbox
import jfx.statement.Conditional.{conditional, elseDo, thenDo}
import org.scalajs.dom.HTMLImageElement

import scala.concurrent.ExecutionContext

class UsersPage(usersProperty: ListProperty[Data[User]]) extends PageComposite("Users") {

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
  def usersPage(list : ListProperty[Data[User]])(using Scope): UsersPage =
    CompositeSupport.buildPage(new UsersPage(list))({})
}

private final class UserNavigationRow extends TableRow[Data[User]] {

  element.ondblclick = _ => {
    val item = getItem
    if (item != null && item.data.id.get != null) {
      Navigation.navigate(s"/core/users/user/${item.data.id.get}")
    }
  }
}

private final class UserImageCell extends TableCell[Data[User], Media | Null] {

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
