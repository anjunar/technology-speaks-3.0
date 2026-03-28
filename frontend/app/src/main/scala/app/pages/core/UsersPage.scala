package app.pages.core

import app.domain.core.{Data, MediaHelper, User}
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.control.TableColumn.{cellFactory, cellValueFactory, column, prefWidth}
import jfx.control.TableView.{fixedCellSize, items, rowFactory, tableView}
import jfx.control.Image.{image, srcProperty}
import jfx.form.Input.{input, inputType, placeholder, stringValueProperty}
import jfx.layout.HBox.hbox
import jfx.control.{TableCell, TableRow, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.domain.Media
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.statement.Conditional.{conditional, elseDo, thenDo}
import org.scalajs.dom.HTMLImageElement

import scala.concurrent.ExecutionContext
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}

class UsersPage(usersProperty: RemoteListProperty[Data[User], RemotePageQuery], searchQuery: Property[String]) extends PageComposite("Users") {

  private val pageSize = 50
  private var pendingReload: SetTimeoutHandle | Null = null

  override def pageWidth: Int = 1040
  override def pageHeight: Int = 860

  addDisposable(
    searchQuery.observeWithoutInitial { _ =>
      scheduleReload()
    }
  )
  addDisposable(() => cancelScheduledReload())

  override protected def compose(using DslContext): Unit = {
    classProperty += "users-page"

    withDslContext {
      vbox {
        classes = "users-page__layout"

        vbox {
          classes = "users-page__hero"

          div {
            classes = "users-page__hero-copy"

            span {
              classes = "users-page__eyebrow"
              text = "Netzwerk"
            }

            span {
              classes = "users-page__title"
              text = "Menschen im Wissensraum"
            }

            span {
              classes = "users-page__subtitle"
              text = "Eine ruhige Uebersicht ueber alle Profile, erreichbar direkt aus der Liste."
            }
          }
        }

        div {
          classes = "users-page__panel"
          style {
            flex = "1"
            minHeight = "0px"
          }

          vbox {
            classes = "users-page__panel-copy"

            span {
              classes = "users-page__panel-eyebrow"
              text = "Profile"
            }

            span {
              classes = "users-page__panel-title"
              text = "Alle Nutzer"
            }
          }

          div {
            classes = "users-page__search"

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
            classes = "users-page__table-shell"
            style {
              flex = "1"
              minHeight = "0px"
            }

            val table = tableView[Data[User]] {
              items = usersProperty
              fixedCellSize = 72.0
              rowFactory = (_: TableView[Data[User]]) => new UserNavigationRow()

              column[Data[User], Media | Null]("Bild") {
                val current = summon[jfx.control.TableColumn[Data[User], Media | Null]]
                current.setPrefWidth(104.0)
                current.setCellValueFactory(features => features.value.data.image)
                current.setCellFactory(_ => new UserImageCell())
              }

              column[Data[User], String]("Nickname") {
                val current = summon[jfx.control.TableColumn[Data[User], String]]
                current.setPrefWidth(260.0)
                current.setSortable(true)
                current.setSortKey("nickName")
                current.setCellValueFactory(features => features.value.data.nickName)
              }

              column[Data[User], String]("Score") {
                val current = summon[jfx.control.TableColumn[Data[User], String]]
                current.setPrefWidth(120.0)
                current.setCellValueFactory(features => Property(f"${features.value.score}%.3f"))
              }

              column[Data[User], String]("Vorname") {
                val current = summon[jfx.control.TableColumn[Data[User], String]]
                current.setPrefWidth(220.0)
                current.setSortable(true)
                current.setSortKey("info.firstName")
                current.setCellValueFactory(features => {
                  val info = features.value.data.info.get
                  Property(if (info == null) "" else info.firstName.get)
                })
              }

              column[Data[User], String]("Nachname") {
                val current = summon[jfx.control.TableColumn[Data[User], String]]
                current.setPrefWidth(220.0)
                current.setSortable(true)
                current.setSortKey("info.lastName")
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

  private def scheduleReload(): Unit = {
    cancelScheduledReload()
    pendingReload = setTimeout(250) {
      RemoteTableList.reloadFirstPage(usersProperty, pageSize = pageSize)
    }
  }

  private def cancelScheduledReload(): Unit =
    if (pendingReload != null) {
      clearTimeout(pendingReload.nn)
      pendingReload = null
    }
}

object UsersPage {
  def usersPage(list : RemoteListProperty[Data[User], RemotePageQuery], searchQuery: Property[String])(using Scope): UsersPage =
    CompositeSupport.buildPage(new UsersPage(list, searchQuery))({})
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
    classes = "users-page__avatar-cell"

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
          classes = Seq("material-icons", "users-page__avatar-fallback")
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
