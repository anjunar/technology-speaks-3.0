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

  override def pageWidth: Int = 1040
  override def pageHeight: Int = 860

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
      vbox {
        classes = "groups-page__layout"

        vbox {
          classes = "groups-page__hero"

          div {
            classes = "groups-page__hero-copy"

            span {
              classes = "groups-page__eyebrow"
              text = "Struktur"
            }

            span {
              classes = "groups-page__title"
              text = "Gruppen und Zugehoerigkeit"
            }

            span {
              classes = "groups-page__subtitle"
              text = "Verwalte die Sammlungen, in denen Beziehungen und Personen geordnet werden."
            }
          }
        }

        hbox {
          classes = "groups-layout"

          div {
            classes = Seq("groups-panel", "groups-panel--list")

            vbox {
              classes = "groups-page__panel-content"

              hbox {
                classes = "groups-panel-header"

                div {
                  classes = "groups-page__panel-copy"

                  span {
                    classes = "groups-page__panel-eyebrow"
                    text = "Sammlung"
                  }

                  span {
                    classes = "groups-page__panel-title"
                    text = "Gruppen"
                  }
                }
              }

              span {
                classes = "groups-page__panel-hint"
                text = "Waehle eine Gruppe aus oder lege eine neue an."
              }

              div {
                classes = "groups-page__table-shell"
                style {
                  flex = "1"
                  minHeight = "0px"
                }

                val table = tableView[Data[Group]] {
                  items = groupsProperty
                  fixedCellSize = 62.0
                  showHeader = false

                column[Data[Group], String]("Name") {
                  val current = summon[TableColumn[Data[Group], String]]
                  current.setPrefWidth(260.0)
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

              hbox {
                classes = "groups-page__list-actions"

                button("Neue Gruppe") {
                  buttonType = "button"
                  classes = "groups-new-btn"

                  onClick { _ =>
                    currentGroupProperty.set(new Group())
                  }
                }
              }
            }
          }

          div {
            classes = Seq("groups-panel", "groups-panel--editor")
            style {
              flex = "1"
              minWidth = "0px"
              minHeight = "0px"
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
        classes = "groups-editor-form"

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

            div {
              classes = "groups-page__panel-copy"

              span {
                classes = "groups-page__panel-eyebrow"
                text = if (group.id.get == null) "Neu" else "Bearbeiten"
              }

              span {
                classes = "groups-page__panel-title"
                text = if (group.id.get == null) "Neue Gruppe" else "Gruppe bearbeiten"
              }
            }
          }

          span {
            classes = "groups-page__panel-hint"
            text = "Gib der Gruppe einen klaren Namen und speichere die Aenderung."
          }

          div {
            classes = "groups-editor-body"

            inputContainer("Name") {
              input("name") {
                classes = "groups-editor-input"
              }
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
