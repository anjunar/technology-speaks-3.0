package app.pages.documents

import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.documents.{Document, Issue, IssueCreated, IssueUpdated}
import app.services.ApplicationService
import app.support.{Navigation, RemotePageQuery, RemoteTableList, TimeAgo}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.control.TableColumn.{cellFactory_=, cellValueFactory_=, column, prefWidth_=}
import jfx.control.TableView.{fixedCellSize_=, items_=, showHeader_=, tableView}
import jfx.control.{TableCell, TableView}
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.input
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import jfx.virtual.virtualList
import org.scalajs.dom.Node

import scala.concurrent.{ExecutionContext, Future}

class DocumentPage extends PageComposite("Dokument") {

  private given ExecutionContext = ExecutionContext.global
  private val documentsPageSize = 50
  private val issuesPageSize = 50

  private val currentDocumentProperty: Property[Document] = Property(new Document())
  private val documentsProperty: RemoteListProperty[Data[Document], RemotePageQuery] =
    RemoteTableList.create[Data[Document]](pageSize = documentsPageSize) { (index, limit) =>
      Document.list(index, limit)
    }
  private val issuesProperty: RemoteListProperty[Issue, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Issue], Issue](pageSize = issuesPageSize) { (index, limit) =>
      val document = currentDocumentProperty.get
      if (Option(document.id.get).exists(_.trim.nonEmpty)) {
        Issue.list(index, limit, document)
      } else {
        Future.successful(new Table[Data[Issue]]())
      }
    }(_.data)
  private val editorPanelProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)

  def model(root: Data[Document]): Unit =
    currentDocumentProperty.set(root.data)

  override protected def compose(using DslContext): Unit = {
    classProperty += "document-page"

    addDisposable(
      currentDocumentProperty.observe { document =>
        editorPanelProperty.set(DocumentEditorPanel.panel(document, handleDocumentSaved))
        reloadIssues()
      }
    )
    addDisposable(
      ApplicationService.messageBus.subscribe {
        case _: IssueCreated =>
          reloadIssues()
        case _: IssueUpdated =>
          reloadIssues()
        case _ =>
          ()
      }
    )

    withDslContext {
      hbox {
        style {
          setProperty("height", "100%")
          setProperty("width", "100%")
          columnGap = "12px"
          overflow = "hidden"
        }

        DocumentListPanel.panel(documentsProperty, currentDocumentProperty, createNewDocument)

        div {
          style {
            flex = "1"
            minWidth = "0px"
            setProperty("height", "100%")
          }

          dynamicOutlet(editorPanelProperty)
        }

        IssuesPanel.panel(currentDocumentProperty, issuesProperty)
      }
    }
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
    if (Option(currentDocumentProperty.get.id.get).exists(_.trim.nonEmpty)) {
      RemoteTableList.reloadFirstPage(issuesProperty, pageSize = issuesPageSize)
    } else {
      issuesProperty.clear()
    }
}

object DocumentPage {
  def documentPage(init: DocumentPage ?=> Unit = {}): DocumentPage =
    CompositeSupport.buildPage(new DocumentPage)(init)
}

private final class DocumentListPanel(
  documents: ListProperty[Data[Document]],
  currentDocument: Property[Document],
  createNewDocument: () => Unit
) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "doc-panel"

    withDslContext {
      vbox {
        style {
          width = "420px"
          rowGap = "12px"
          height = "100%"
        }

        hbox {
          classes = "doc-panel-header"
          span {
            classes = "doc-panel-title"
            text = "Dokumente"
          }
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          val table = tableView[Data[Document]] {
            items_=(documents)
            fixedCellSize_=(64.0)
            showHeader_=(false)

            column[Data[Document], String]("Dokumente") {
              val current = summon[jfx.control.TableColumn[Data[Document], String]]
              current.setPrefWidth(400.0)
              current.setCellValueFactory(features => features.value.data.title)
              current.setCellFactory(_ => new DocumentSummaryCell())
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
              if (index >= 0) table.getSelectionModel.select(index)
              else table.getSelectionModel.clearSelection()
            }
          )
        }

        button("") {
          buttonType_=("button")
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
            text = "Neues Dokument"
          }

          onClick(_ => createNewDocument())
        }
      }
    }
  }
}

private object DocumentListPanel {
  def panel(
    documents: ListProperty[Data[Document]],
    currentDocument: Property[Document],
    createNewDocument: () => Unit
  ): DocumentListPanel =
    CompositeSupport.buildComposite(new DocumentListPanel(documents, currentDocument, createNewDocument))
}

private final class DocumentSummaryCell extends TableCell[Data[Document], String] {

  private val wrapper = newElement("div")
  private val icon = newElement("span")
  private val textColumn = newElement("div")
  private val title = newElement("div")
  private val subtitle = newElement("div")

  wrapper.style.display = "flex"
  wrapper.style.setProperty("align-items", "center")
  wrapper.style.columnGap = "10px"
  wrapper.style.width = "100%"

  icon.className = "material-icons"
  icon.textContent = "description"
  icon.style.fontSize = "18px"
  icon.style.opacity = "0.75"

  textColumn.style.overflow = "hidden"
  textColumn.style.minWidth = "0"
  textColumn.style.display = "flex"
  textColumn.style.setProperty("flex-direction", "column")

  title.style.fontWeight = "600"
  title.style.overflow = "hidden"
  title.style.textOverflow = "ellipsis"
  title.style.whiteSpace = "nowrap"

  subtitle.style.fontSize = "12px"
  subtitle.style.opacity = "0.75"

  textColumn.appendChild(title)
  textColumn.appendChild(subtitle)
  wrapper.appendChild(icon)
  wrapper.appendChild(textColumn)
  element.appendChild(wrapper)

  override protected def updateItem(item: String | Null, empty: Boolean): Unit = {
    val rowValue = Option(getTableRow).flatMap(row => Option(row.getItem))
    val isEmptyCell = empty || rowValue.isEmpty

    if (isEmptyCell) {
      element.classList.add("jfx-table-cell-empty")
      title.textContent = ""
      subtitle.textContent = ""
      wrapper.style.display = "none"
    } else {
      val document = rowValue.get.data
      element.classList.remove("jfx-table-cell-empty")
      title.textContent = Option(document.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)")
      subtitle.textContent = TimeAgo.format(document.created.get)
      wrapper.style.display = "flex"
    }
  }
}

private final class DocumentEditorPanel(
  document: Document,
  onSaved: Data[Document] => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "doc-panel"

    withDslContext {
      form(document) {
        onSubmit_= { _ =>
          val request =
            if (Option(document.id.get).exists(_.trim.nonEmpty)) document.update()
            else document.save()

          request.foreach(onSaved)
        }

        style {
          display = "flex"
          flexDirection = "column"
          height = "100%"
        }

        hbox {
          classes = "doc-titlebar"
          style {
            alignItems = "center"
            columnGap = "10px"
          }

          val titleInput = input("title") {
            style {
              flex = "1"
              minWidth = "0px"
            }
          }

          titleInput.placeholder = "Titel"
          addDisposable(document.editable.observe(editable => titleInput.element.disabled = !editable))

          val editButton = button("edit") {
            buttonType_=("button")
            classes = Seq("material-icons", "doc-icon-btn")
            onClick { _ =>
              document.editable.set(!document.editable.get)
            }
          }

          editButton.element.style.display =
            if (Option(document.id.get).exists(_.trim.nonEmpty)) "inline-flex" else "none"
        }

        val editorField = editor("editor") {
          classes = "doc-editor"
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

        addDisposable(Property.subscribeBidirectional(document.editable, editorField.editableProperty))
      }
    }
  }
}

private object DocumentEditorPanel {
  def panel(document: Document, onSaved: Data[Document] => Unit): DocumentEditorPanel =
    CompositeSupport.buildComposite(new DocumentEditorPanel(document, onSaved))
}

private final class IssuesPanel(
  currentDocument: Property[Document],
  issues: ListProperty[Issue]
) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "doc-panel"

    withDslContext {
      vbox {
        style {
          width = "420px"
          rowGap = "12px"
          height = "100%"
        }

        hbox {
          classes = "doc-panel-header"
          span {
            classes = "doc-panel-title"
            text = "Aufgaben"
          }
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(issues, estimateHeightPx = 220, overscanPx = 240, prefetchItems = 40) { (issue, _) =>
            if (issue == null) {
              val card = loadingCard {}
              card.minHeight("160px")
              card
            } else {
              IssueListItem.item(currentDocument, issue)
            }
          }
        }

        button("") {
          buttonType_=("button")
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
            text = "Neue Aufgabe"
          }

          onClick { _ =>
            val document = currentDocument.get
            if (Option(document.id.get).exists(_.trim.nonEmpty)) {
              Navigation.navigate(s"/document/documents/document/${document.id.get}/issues/issue")
            }
          }
        }
      }
    }
  }
}

private object IssuesPanel {
  def panel(currentDocument: Property[Document], issues: ListProperty[Issue]): IssuesPanel =
    CompositeSupport.buildComposite(new IssuesPanel(currentDocument, issues))
}

private final class IssueListItem(
  currentDocument: Property[Document],
  issue: Issue
) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      var editorFieldRef: jfx.form.Editor | Null = null

      vbox {
        style {
          rowGap = "10px"
          cursor = "pointer"
        }

        val header = componentHeader {}
        header.model(issue)

        div {
          style {
            fontWeight = "600"
          }
          text = Option(issue.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)")
        }

        editorFieldRef = editor("editor") {
          basePlugin {}
          headingPlugin {}
          listPlugin {}
          linkPlugin {}
          imagePlugin {}
        }

        editorFieldRef.nn.editableProperty.set(false)
      }

      addDisposable(Property.subscribeBidirectional(issue.editor, editorFieldRef.nn.valueProperty))
      element.onclick = _ => {
        val documentId = currentDocument.get.id.get
        if (Option(documentId).exists(_.trim.nonEmpty) && Option(issue.id.get).exists(_.trim.nonEmpty)) {
          Navigation.navigate(s"/document/documents/document/$documentId/issues/issue/${issue.id.get}")
        }
      }
    }
  }
}

private object IssueListItem {
  def item(currentDocument: Property[Document], issue: Issue): IssueListItem =
    CompositeSupport.buildComposite(new IssueListItem(currentDocument, issue))
}
