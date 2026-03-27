package app.pages.followers

import app.domain.core.Data
import app.domain.followers.Group
import app.support.{Api, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.TableColumn.column
import jfx.control.TableView.{fixedCellSize, items, showHeader, tableView}
import jfx.control.TableColumn
import jfx.core.component.ElementComponent.*
import jfx.core.state.{Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Form
import jfx.form.Form.{form, onSubmit}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class GroupsPage extends PageComposite("Groups") {

  private given ExecutionContext = ExecutionContext.global

  private val pageSize = 100
  private val groupsProperty: RemoteListProperty[Data[Group], RemotePageQuery] =
    RemoteTableList.create[Data[Group]](pageSize = pageSize) { (index, limit) =>
      Group.list(index, limit)
    }
  private val currentGroupProperty: Property[Group] = Property(new Group())

  override protected def compose(using DslContext): Unit = {
    classProperty += "groups-page"
    RemoteTableList.reloadFirstPage(groupsProperty, pageSize = pageSize)

    withDslContext {
      hbox {
        classes = "groups-layout"

        div {
          classes = "groups-panel"

          vbox {
            style {
              height = "100%"
            }

            hbox {
              classes = "groups-panel-header"

              span {
                classes = "groups-panel-title"
                text = "Gruppen"
              }
            }

            div {
              style {
                flex = "1"
                minHeight = "0px"
              }

              val table = tableView[Data[Group]] {
                items = groupsProperty
                fixedCellSize = 56.0
                showHeader = false

                column[Data[Group], String]("Name") {
                  val current = summon[TableColumn[Data[Group], String]]
                  current.setPrefWidth(300.0)
                  current.setCellValueFactory(features => features.value.data.name)
                }
              }

              table.classProperty += "groups-table"

              addDisposable(
                table.getSelectionModel.selectedItemProperty.observe { selected =>
                  if (selected != null) {
                    currentGroupProperty.set(selected.data)
                  }
                }
              )

              addDisposable(
                currentGroupProperty.observe { active =>
                  val activeId = Option(active).flatMap(group => Option(group.id.get))
                  val index =
                    if (activeId.isEmpty) -1
                    else groupsProperty.indexWhere(row => Option(row.data.id.get).contains(activeId.get))

                  if (index >= 0) {
                    table.getSelectionModel.select(index)
                  } else {
                    table.getSelectionModel.clearSelection()
                  }
                }
              )
            }

            button("Neue Gruppe") {
              buttonType = "button"
              classes = "groups-new-btn"

              onClick { _ =>
                currentGroupProperty.set(new Group())
              }
            }
          }
        }

        div {
          classes = "groups-panel"
          style {
            flex = "1"
            minWidth = "0px"
          }

          observeRender(currentGroupProperty) { group =>
            GroupEditorPanel.panel(
              group = group,
              onSaved = saved => {
                RemoteTableList.reloadFirstPage(groupsProperty, pageSize = pageSize)
                currentGroupProperty.set(saved.data)
              },
              onDeleted = () => {
                RemoteTableList.reloadFirstPage(groupsProperty, pageSize = pageSize)
                currentGroupProperty.set(new Group())
              }
            )
          }
        }
      }
    }
  }
}

object GroupsPage {
  def groupsPage(init: GroupsPage ?=> Unit = {})(using Scope): GroupsPage =
    CompositeSupport.buildPage(new GroupsPage)(init)
}

private final class GroupEditorPanel(
  group: Group,
  onSaved: Data[Group] => Unit,
  onDeleted: () => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "groups-editor"

    withDslContext {
      form(group) {
        onSubmit = (_: Form[Group]) => {
          val request =
            if (group.id.get == null) group.save()
            else group.update()

          request.onComplete {
            case Success(saved) =>
              Viewport.notify("Gruppe gespeichert.", Viewport.NotificationKind.Success)
              onSaved(saved)

            case Failure(error) =>
              Api.logFailure("Group save", error)
              Viewport.notify("Gruppe konnte nicht gespeichert werden.", Viewport.NotificationKind.Error)
          }
        }

        vbox {
          classes = "groups-editor-content"

          hbox {
            classes = "groups-panel-header"

            span {
              classes = "groups-panel-title"
              text = if (group.id.get == null) "Neue Gruppe" else "Gruppe bearbeiten"
            }
          }

          div {
            classes = "groups-editor-body"

            inputContainer("Name") {
              input("name")
            }
          }

          hbox {
            classes = "groups-editor-actions"

            if (group.id.get != null) {
              button("Loeschen") {
                buttonType = "button"
                classes = "groups-delete-btn"

                onClick { _ =>
                  group.delete().onComplete {
                    case Success(_) =>
                      Viewport.notify("Gruppe geloescht.", Viewport.NotificationKind.Success)
                      onDeleted()

                    case Failure(error) =>
                      Api.logFailure("Group delete", error)
                      Viewport.notify("Gruppe konnte nicht geloescht werden.", Viewport.NotificationKind.Error)
                  }
                }
              }
            }

            button("Speichern") {
              classes = "groups-save-btn"
            }
          }
        }
      }
    }
  }
}

private object GroupEditorPanel {
  def panel(
    group: Group,
    onSaved: Data[Group] => Unit,
    onDeleted: () => Unit
  )(using Scope): GroupEditorPanel =
    CompositeSupport.buildComposite(new GroupEditorPanel(group, onSaved, onDeleted))
}
