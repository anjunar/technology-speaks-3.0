package app.pages.documents

import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.documents.{Document, Issue, IssueCreated, IssueUpdated}
import app.services.ApplicationService
import app.support.{Navigation, RemotePageQuery, RemoteTableList, TimeAgo}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.TableColumn.{cellFactory, cellValueFactory, column, prefWidth}
import jfx.control.TableView.{fixedCellSize, items, showHeader, tableView}
import jfx.control.{TableCell, TableColumn, virtualList}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Control.valueProperty
import jfx.form.Editable.editable
import jfx.form.Editor.editor
import jfx.form.Form
import jfx.form.Form.{form, onSubmit}
import jfx.form.Input.{input, inputType, placeholder, stringValueProperty}
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}

class DocumentPage(val model: Document) extends PageComposite("Dokument") {

  private given ExecutionContext = ExecutionContext.global

  private val documentsPageSize = 50
  private val issuesPageSize = 50

  private val currentDocumentProperty: Property[Document] = Property(model)
  private val searchQueryProperty: Property[String] = Property("")
  private val documentsProperty: RemoteListProperty[Data[Document], RemotePageQuery] =
    RemoteTableList.create[Data[Document]](pageSize = documentsPageSize) { (index, limit) =>
      Document.list(index, limit, searchQueryProperty.get)
    }
  private val issuesProperty: RemoteListProperty[Issue, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Issue], Issue](pageSize = issuesPageSize) { (index, limit) =>
      val document = currentDocumentProperty.get
      if (document.id.get != null) {
        Issue.list(index, limit, document)
      } else {
        Future.successful(new Table[Data[Issue]]())
      }
    }(_.data)

  override protected def compose(using DslContext): Unit = {
    classProperty += "document-page"

    addDisposable(
      currentDocumentProperty.observe { document =>
        reloadIssues()
      }
    )
    addDisposable(
      searchQueryProperty.observeWithoutInitial { _ =>
        scheduleDocumentReload()
      }
    )
    addDisposable(() => cancelScheduledReload())

    withDslContext {
      val service = inject[ApplicationService]

      addDisposable(
        service.messageBus.subscribe {
          case _: IssueCreated =>
            reloadIssues()
          case _: IssueUpdated =>
            reloadIssues()
          case _ =>
            ()
        }
      )

      hbox {
        classes = "documents-layout"
        style {
          height = "100%"
          width = "100%"
          overflow = "hidden"
        }

        DocumentListPanel.panel(documentsProperty, currentDocumentProperty, searchQueryProperty, createNewDocument)

        div {
          style {
            flex = "1"
            minWidth = "0px"
          }

          observeRender(currentDocumentProperty) { document =>
            DocumentEditorPanel.panel(document, handleDocumentSaved)
          }
        }

        IssuesPanel.panel(currentDocumentProperty, issuesProperty)
      }
    }
  }

  private var pendingDocumentReload: SetTimeoutHandle | Null = null

  private def scheduleDocumentReload(): Unit = {
    cancelScheduledReload()
    pendingDocumentReload = setTimeout(250) {
      RemoteTableList.reloadFirstPage(documentsProperty, pageSize = documentsPageSize)
    }
  }

  private def cancelScheduledReload(): Unit =
    if (pendingDocumentReload != null) {
      clearTimeout(pendingDocumentReload.nn)
      pendingDocumentReload = null
    }

  private def createNewDocument(): Unit = {
    val document = new Document()
    document.editable.set(true)
    currentDocumentProperty.set(document)
  }

  private def handleDocumentSaved(saved: Data[Document]): Unit = {
    RemoteTableList.reloadFirstPage(documentsProperty, pageSize = documentsPageSize)
    saved.data.editable.set(false)
    currentDocumentProperty.set(saved.data)
  }

  private def reloadIssues(): Unit =
    if (currentDocumentProperty.get.id.get != null) {
      RemoteTableList.reloadFirstPage(issuesProperty, pageSize = issuesPageSize)
    } else {
      issuesProperty.clear()
    }
}

object DocumentPage {
  def documentPage(model: Document, init: DocumentPage ?=> Unit = {})(using Scope): DocumentPage =
    CompositeSupport.buildPage(new DocumentPage(model))(init)
}

private final class DocumentListPanel(
                                       documents: ListProperty[Data[Document]],
                                       currentDocument: Property[Document],
                                       searchQuery: Property[String],
                                       createNewDocument: () => Unit
                                     ) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("doc-panel", "glass", "doc-sidebar", "doc-sidebar-left")

    withDslContext {
      vbox {
        classes = "doc-panel-shell"
        style {
          width = "420px"
          height = "100%"
        }

        hbox {
          classes = "doc-panel-header"

          vbox {
            classes = "doc-heading"

            span {
              classes = "doc-panel-title"
              text = "Wissensraum"
            }

            span {
              classes = "doc-panel-subtitle"
              text = "Diskurse"
            }
          }
        }

        div {
          classes = "doc-search"

          span {
            classes = "material-icons"
            text = "search"
          }

          input("search") {
            placeholder = "Suche..."
            inputType = "search"
            subscribeBidirectional(searchQuery, stringValueProperty)
          }
        }

        div {
          classes = "doc-table-shell"
          style {
            flex = "1"
            minHeight = "0px"
          }

          val table = tableView[Data[Document]] {
            items = documents
            fixedCellSize = 64.0
            showHeader = false

            column[Data[Document], String]("Titel") {
              prefWidth = 400.0
              cellValueFactory = (features: TableColumn.CellDataFeatures[Data[Document], String]) => features.value.data.title
              cellFactory = (column: TableColumn[Data[Document], String]) => new DocumentSummaryCell()
            }
          }

          table.classProperty += "doc-table"

          addDisposable(
            table.getSelectionModel.selectedItemProperty.observe { selected =>
              if (selected != null) {
                currentDocument.set(selected.data)
              }
            }
          )

          addDisposable(
            currentDocument.observe { active =>
              val index = documents.indexWhere(_.data.id.get == active.id.get)
              if (index >= 0) {
                table.getSelectionModel.select(index)
              } else {
                table.getSelectionModel.clearSelection()
              }
            }
          )
        }

        button("Neuer Text") {
          buttonType = "button"
          classes = "doc-new-btn"
          style {
            display = "flex"
            alignItems = "center"
            justifyContent = "center"
            columnGap = "10px"
          }

          span {
            classes = "material-icons"
            text = "add"
          }

          span {
            text = "Neuer Text"
          }

          onClick(_ => createNewDocument())
        }
      }
    }
  }
}

private object DocumentListPanel {
  def panel(documents: ListProperty[Data[Document]],
            currentDocument: Property[Document],
            searchQuery: Property[String],
            createNewDocument: () => Unit)(using Scope): DocumentListPanel =
    CompositeSupport.buildComposite(new DocumentListPanel(documents, currentDocument, searchQuery, createNewDocument))
}

private final class DocumentSummaryCell extends TableCell[Data[Document], String] {
  private val titleProperty = Property("")
  private val subtitleProperty = Property("")
  private val visibleProperty = Property(false)

  val wrapper = hbox {
    classes = "doc-summary"

    style {
      alignItems = "center"
      columnGap = "14px"
      width = "100%"
      display <-- visibleProperty.map(visible => if (visible) "flex" else "none")
    }

    span {
      classes = Seq("material-icons", "doc-summary-icon")
      text = "description"
    }

    vbox {
      classes = "doc-summary-copy"
      style {
        flex = "1"
        minWidth = "0px"
        overflow = "hidden"
      }

      div {
        classes = "doc-summary-title"
        subscribeBidirectional(titleProperty, textProperty)
      }

      div {
        classes = "doc-summary-subtitle"
        subscribeBidirectional(subtitleProperty, textProperty)
      }
    }
  }

  wrapper.onMount()
  element.appendChild(wrapper.element)
  addDisposable(() => wrapper.dispose())

  override protected def updateItem(item: String | Null, empty: Boolean): Unit = {
    val rowValue = Option(getTableRow).flatMap(row => Option(row.getItem))
    if (empty || rowValue.isEmpty) {
      element.classList.add("jfx-table-cell-empty")
      visibleProperty.set(false)
    } else {
      val document = rowValue.get.data
      element.classList.remove("jfx-table-cell-empty")
      titleProperty.set(Option(document.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)"))
      subtitleProperty.set(TimeAgo.format(document.created.get))
      visibleProperty.set(true)
    }
  }
}

private final class DocumentEditorPanel(document: Document, onSaved: Data[Document] => Unit) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classes = Seq("doc-panel", "glass", "doc-editor-panel")
    style {
      height = "100%"
    }

    withDslContext {
      form(document) {
        onSubmit = (_: Form[Document]) =>
          val request =
            if (document.id.get != null) document.update()
            else document.save()

          request.foreach(onSaved)


        style {
          display = "flex"
          flexDirection = "column"
          flex = "1"
          minWidth = "0px"
          height = "100%"
        }

        hbox {
          classes = "doc-titlebar"

          vbox {
            classes = "doc-title-copy"
            style {
              flex = "1"
              minWidth = "0px"
            }

            span {
              classes = "doc-panel-title"
              text = "Gedankenfeld"
            }

            val titleInput = input("title") {
              classes = "doc-title-input"
              style {
                flex = "1"
                minWidth = "0px"
              }
            }

            titleInput.placeholder = "Titel"
            addDisposable(document.editable.observe(editable => titleInput.element.disabled = !editable))
          }

          Navigation.renderByRel("update", document.links) { () =>
            val editButton = button("edit") {
              buttonType = "button"
              classes = Seq("material-icons", "doc-icon-btn")
              onClick { _ =>
                document.editable.set(!document.editable.get)
              }
            }

            addDisposable(
              document.editable.observe { editable =>
                editButton.element.textContent = if (editable) "done" else "edit"
                if (editable) editButton.element.classList.add("active")
                else editButton.element.classList.remove("active")
              }
            )
          }
        }

        val editorField = editor("editor") {
          classes = Seq("doc-editor", "glass")
          style {
            flex = "1"
            minHeight = "0px"
          }

          basePlugin {}
          headingPlugin {}
          listPlugin {}
          linkPlugin {}
          imagePlugin {}

          button("save") {
            classes = Seq("material-icons", "doc-icon-btn")
          }
        }

        subscribeBidirectional(document.editable, editorField.editableProperty)
      }
    }
  }
}

private object DocumentEditorPanel {
  def panel(document: Document, onSaved: Data[Document] => Unit)(using Scope): DocumentEditorPanel =
    CompositeSupport.buildComposite(new DocumentEditorPanel(document, onSaved))
}

private final class IssuesPanel(
                                 currentDocument: Property[Document],
                                 issues: ListProperty[Issue]
                               ) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("doc-panel", "glass", "doc-sidebar", "doc-sidebar-right")

    withDslContext {
      vbox {
        classes = "doc-panel-shell"
        style {
          width = "420px"
          height = "100%"
          rowGap = "12px"
        }

        hbox {
          classes = "doc-panel-header"

          vbox {
            classes = "doc-heading"

            span {
              classes = "doc-panel-title"
              text = "Resonanz"
            }

            span {
              classes = "doc-panel-subtitle"
              text = "Dialoge"
            }
          }
        }

        div {
          classes = "issues-list-shell"
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(issues, estimateHeightPx = 220, overscanPx = 240, prefetchItems = 40) { (issue, _) =>
            if (issue == null) {
              loadingCard {
                style {
                  minHeight = "160px"
                }
              }
            } else {
              IssueListItem.item(currentDocument, issue)
            }
          }
        }

        button("Neuer Impuls") {
          buttonType = "button"
          classes = "doc-new-btn"
          style {
            display = "flex"
            alignItems = "center"
            justifyContent = "center"
            columnGap = "10px"
          }

          span {
            classes = "material-icons"
            text = "add"
          }

          span {
            text = "Neuer Impuls"
          }

          onClick { _ =>
            val document = currentDocument.get
            if (document.id.get != null) {
              Navigation.navigate(s"/document/documents/document/${document.id.get}/issues/issue")
            }
          }
        }
      }
    }
  }
}

private object IssuesPanel {
  def panel(currentDocument: Property[Document], issues: ListProperty[Issue])(using Scope): IssuesPanel =
    CompositeSupport.buildComposite(new IssuesPanel(currentDocument, issues))
}

private final class IssueListItem(currentDocument: Property[Document], issue: Issue) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("glass-border", "issue-card")

    withDslContext {

      vbox {
        classes = "issue-card-shell"
        style {
          rowGap = "10px"
          cursor = "pointer"
        }

        componentHeader(issue) {}

        div {
          style {
            fontWeight = "600"
          }

          text = Option(issue.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)")
        }

        editor("editor", true) {
          basePlugin {}
          headingPlugin {}
          listPlugin {}
          linkPlugin {}
          imagePlugin {}

          editable = false
          subscribeBidirectional(issue.editor, valueProperty)
        }
      }


    }
  }
}

private object IssueListItem {
  def item(currentDocument: Property[Document], issue: Issue)(using Scope): IssueListItem =
    CompositeSupport.buildComposite(new IssueListItem(currentDocument, issue))
}
