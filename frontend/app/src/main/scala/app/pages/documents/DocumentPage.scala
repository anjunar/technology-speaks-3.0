package app.pages.documents

import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.documents.{Document, Issue, IssueCreated, IssueUpdated}
import app.services.ApplicationService
import app.support.{Navigation, RemotePageQuery, RemoteTableList, TimeAgo}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, buttonType_=, onClick}
import jfx.control.TableColumn.{cellFactory_=, cellValueFactory_=, column, prefWidth_=}
import jfx.control.TableView.{fixedCellSize_=, items_=, showHeader_=, tableView}
import jfx.control.{TableCell, virtualList}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.{input, inputType_=}
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
            height = "100%"
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
    classProperty += "doc-panel"

    withDslContext {
      vbox {
        style {
          width = "420px"
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
          classes = "doc-search"

          span {
            classes = "material-icons"
            text = "search"
          }

          val searchInput = input("search") {}
          searchInput.placeholder = "Suche..."
          inputType_=("search")(using searchInput)
          subscribeBidirectional(searchQuery, searchInput.valueProperty.asInstanceOf[Property[String]])
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

            column[Data[Document], String]("Titel") {
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
              if (index >= 0) {
                table.getSelectionModel.select(index)
              } else {
                table.getSelectionModel.clearSelection()
              }
            }
          )
        }

        button("Neues Dokument") {
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
    searchQuery: Property[String],
    createNewDocument: () => Unit
  )(using Scope): DocumentListPanel =
    CompositeSupport.buildComposite(new DocumentListPanel(documents, currentDocument, searchQuery, createNewDocument))
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

  textColumn.style.display = "flex"
  textColumn.style.setProperty("flex-direction", "column")
  textColumn.style.overflow = "hidden"
  textColumn.style.minWidth = "0"

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
    if (empty || rowValue.isEmpty) {
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
        onSubmit_= { (_ : Form[Document]) =>
          val request =
            if (document.id.get != null) document.update()
            else document.save()

          request.foreach(onSaved)
        }

        style {
          display = "flex"
          flexDirection = "column"
          flex = "1"
          minWidth = "0px"
          height = "100%"
        }

        hbox {
          classes = "doc-titlebar"

          val titleInput = input("title") {
            style {
              flex = "1"
              minWidth = "0px"
            }
          }

          titleInput.placeholder = "Titel"
          addDisposable(document.editable.observe(editable => titleInput.element.disabled = !editable))

          Navigation.renderByRel("update", document.links) { () =>
            val editButton = button("edit") {
              buttonType_=("button")
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
    classProperty += "doc-panel"

    withDslContext {
      vbox {
        style {
          width = "420px"
          height = "100%"
          rowGap = "12px"
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

        button("Neue Aufgabe") {
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
            text = "Neue Aufgabe"
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

        componentHeader(issue) {}

        div {
          style {
            fontWeight = "600"
          }

          text = Option(issue.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)")
        }

        editorFieldRef = editor("editor", true) {
          basePlugin {}
          headingPlugin {}
          listPlugin {}
          linkPlugin {}
          imagePlugin {}
        }

        editorFieldRef.nn.editableProperty.set(false)
      }

      subscribeBidirectional(issue.editor, editorFieldRef.nn.valueProperty)
      element.onclick = _ => {
        val documentId = currentDocument.get.id.get
        if (documentId != null && issue.id.get != null) {
          Navigation.navigate(s"/document/documents/document/$documentId/issues/issue/${issue.id.get}")
        }
      }
    }
  }
}

private object IssueListItem {
  def item(currentDocument: Property[Document], issue: Issue)(using Scope): IssueListItem =
    CompositeSupport.buildComposite(new IssueListItem(currentDocument, issue))
}
