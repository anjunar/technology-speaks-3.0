package app.pages.documents

import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Link, Table}
import app.domain.curation.CurationCandidate
import app.domain.documents.{Document, Issue, IssueCreated, IssueUpdated}
import app.editor.plugins.{DocumentLinkPlugin, DocumentLinkSuggestion}
import app.editor.plugins.DocumentLinkPlugin.documentLinkPlugin
import app.services.ApplicationService
import app.support.{Api, LayoutMode, LayoutResolver, Navigation, RemotePageQuery, RemoteTableList, TimeAgo}
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
import jfx.form.Input.{booleanValueProperty, input, inputType, placeholder, standaloneInput, stringValueProperty}
import jfx.form.editor.plugins.*
import jfx.layout.Drawer
import jfx.layout.Drawer.{drawer, drawerContent, drawerNavigation}
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport
import jfx.statement.ObserveRender.observeRender

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.{Failure, Success}

class DocumentPage(val model: Document) extends PageComposite("Dokument") {

  private given ExecutionContext = ExecutionContext.global
  private val mobileLayout = LayoutResolver.queryOverrideFromNavigation.getOrElse(LayoutResolver.autoDetect()) == LayoutMode.Mobile

  private val documentsPageSize = 10
  private val issuesPageSize = 10

  private val currentDocumentProperty: Property[Document] = Property(if (model != null) model else new Document())
  private val searchQueryProperty: Property[String] = Property("")
  private val leftSidebarExpandedProperty: Property[Boolean] = Property(!mobileLayout)
  private val rightSidebarExpandedProperty: Property[Boolean] = Property(!mobileLayout)
  private val documentsProperty: RemoteListProperty[Data[Document], RemotePageQuery] =
    RemoteTableList.create[Data[Document]](pageSize = documentsPageSize) { query =>
      Document.list(query.index, query.limit, searchQueryProperty.get, sorting = query.effectiveSortSpecs(Seq("bookname:asc", "title:asc")))
    }
  private val issuesProperty: RemoteListProperty[Issue, RemotePageQuery] =
    RemoteTableList.createMapped[Data[Issue], Issue](pageSize = issuesPageSize) { query =>
      val document = currentDocumentProperty.get
      if (document.id.get != null) {
        Issue.list(query.index, query.limit, document)
      } else {
        Future.successful(new Table[Data[Issue]]())
      }
    }(_.data)
  private val provenancePageSize = 10
  private val curationCandidatesProperty: RemoteListProperty[Data[CurationCandidate], RemotePageQuery] =
    RemoteTableList.create[Data[CurationCandidate]](pageSize = provenancePageSize) { query =>
      val document = currentDocumentProperty.get
      if (document.id.get != null) {
        CurationCandidate.listForDocument(document, query.index, query.limit)
      } else {
        Future.successful(new Table[Data[CurationCandidate]]())
      }
    }

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

      if (mobileLayout) {
        renderMobileLayout()
      } else {
        renderDesktopLayout()
      }
    }
  }

  private def renderDesktopLayout()(using DslContext): Unit = {
    drawer {
      classes = "documents-layout documents-layout--desktop documents-drawer documents-drawer-left"
      summon[Drawer].width = "420px"
      summon[Drawer].isOpen = leftSidebarExpandedProperty.get

      addDisposable(leftSidebarExpandedProperty.observe(isOpen => summon[Drawer].isOpen = isOpen))
      addDisposable(summon[Drawer].openProperty.observe(isOpen => leftSidebarExpandedProperty.set(isOpen)))

      drawerNavigation {
        DocumentListPanel.panel(
          documentsProperty,
          currentDocumentProperty,
          searchQueryProperty,
          leftSidebarExpandedProperty,
          createNewDocument,
          openDocument,
          handleDocumentsImported
        )
      }

      drawerContent {
        drawer {
          classes = "documents-drawer documents-drawer-right"
          summon[Drawer].width = "304px"
          summon[Drawer].side = Drawer.Side.End
          summon[Drawer].isOpen = rightSidebarExpandedProperty.get

          addDisposable(rightSidebarExpandedProperty.observe(isOpen => summon[Drawer].isOpen = isOpen))
          addDisposable(summon[Drawer].openProperty.observe(isOpen => rightSidebarExpandedProperty.set(isOpen)))

          drawerNavigation {
            IssuesPanel.panel(currentDocumentProperty, issuesProperty, curationCandidatesProperty, rightSidebarExpandedProperty)
          }

          drawerContent {
            renderEditorStage()
          }
        }
      }
    }
  }

  private def renderMobileLayout()(using DslContext): Unit = {
    drawer {
      classes = "documents-layout documents-layout--mobile documents-drawer documents-drawer-left"
      summon[Drawer].width = "320px"

      addDisposable(leftSidebarExpandedProperty.observe(isOpen => summon[Drawer].isOpen = isOpen))
      addDisposable(summon[Drawer].openProperty.observe(isOpen => leftSidebarExpandedProperty.set(isOpen)))

      drawerNavigation {
        DocumentListPanel.panel(
          documentsProperty,
          currentDocumentProperty,
          searchQueryProperty,
          leftSidebarExpandedProperty,
          createNewDocument,
          openDocument,
          handleDocumentsImported
        )
      }

      drawerContent {
        drawer {
          classes = "documents-drawer documents-drawer-right"
          summon[Drawer].width = "304px"
          summon[Drawer].side = Drawer.Side.End

          addDisposable(rightSidebarExpandedProperty.observe(isOpen => summon[Drawer].isOpen = isOpen))
          addDisposable(summon[Drawer].openProperty.observe(isOpen => rightSidebarExpandedProperty.set(isOpen)))

          drawerNavigation {
            IssuesPanel.panel(currentDocumentProperty, issuesProperty, curationCandidatesProperty, rightSidebarExpandedProperty)
          }

          drawerContent {
            div {
              classes = Seq("documents-mobile-main", "doc-editor-stage")
              style {
                minWidth = "0px"
                position = "relative"
                height = "100%"
              }

              renderDockToggles()

              observeRender(currentDocumentProperty) { document =>
                DocumentEditorPanel.panel(document, curationCandidatesProperty, handleDocumentSaved, openDocumentByHref)
              }
            }
          }
        }
      }
    }
  }

  private def renderEditorStage()(using DslContext): Unit = {
    div {
      classes = "doc-editor-stage"
      style {
        flex = "1"
        minWidth = "0px"
        position = "relative"
        height = "100%"
      }

      renderDockToggles()

      observeRender(currentDocumentProperty) { document =>
        DocumentEditorPanel.panel(document, curationCandidatesProperty, handleDocumentSaved, openDocumentByHref)
      }
    }
  }

  private def renderDockToggles()(using DslContext): Unit = {
    button("reopen-left-sidebar") {
      buttonType = "button"
      classes = Seq("material-icons", "doc-icon-btn", "doc-dock-toggle", "doc-dock-toggle-left")
      text = "left_panel_open"
      style {
        display <-- leftSidebarExpandedProperty.map(isExpanded => if (isExpanded) "none" else "inline-flex")
      }
      onClick { _ =>
        if (mobileLayout) {
          rightSidebarExpandedProperty.set(false)
        }
        leftSidebarExpandedProperty.set(true)
      }
    }

    button("reopen-right-sidebar") {
      buttonType = "button"
      classes = Seq("material-icons", "doc-icon-btn", "doc-dock-toggle", "doc-dock-toggle-right")
      text = "right_panel_open"
      style {
        display <-- rightSidebarExpandedProperty.map(isExpanded => if (isExpanded) "none" else "inline-flex")
      }
      onClick { _ =>
        if (mobileLayout) {
          leftSidebarExpandedProperty.set(false)
        }
        rightSidebarExpandedProperty.set(true)
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
    if (mobileLayout) {
      leftSidebarExpandedProperty.set(false)
    }
  }

  private def openDocument(document: Document): Unit = {
    if (document == null) {
      return
    }
    val selectedId = document.id.get
    val currentId = currentDocumentProperty.get.id.get

    if (selectedId == null) {
      currentDocumentProperty.set(document)
      if (mobileLayout) {
        leftSidebarExpandedProperty.set(false)
      }
    } else if (selectedId == currentId) {
      if (currentDocumentProperty.get.links.isEmpty && document.links.nonEmpty) {
        currentDocumentProperty.set(document)
      }
    } else {
      Document
        .read(selectedId.toString)
        .map(_.data)
        .foreach { loaded =>
          currentDocumentProperty.set(loaded)
          if (mobileLayout) {
            leftSidebarExpandedProperty.set(false)
          }
        }
    }
  }

  private def openDocumentByHref(href: String): Unit = {
    val prefix = "/document/documents/document/"
    if (href != null && href.startsWith(prefix)) {
      val documentId = href.drop(prefix.length).takeWhile(_ != '/')
      if (documentId.nonEmpty) {
        Document
          .read(documentId)
          .map(_.data)
          .foreach { loaded =>
            currentDocumentProperty.set(loaded)
            if (mobileLayout) {
              leftSidebarExpandedProperty.set(false)
            }
          }
      }
    }
  }

  private def handleDocumentSaved(saved: Data[Document]): Unit = {
    RemoteTableList.reloadFirstPage(documentsProperty, pageSize = documentsPageSize)
    if (saved != null && saved.data != null) {
      saved.data.editable.set(false)
      currentDocumentProperty.set(saved.data)
      if (mobileLayout) {
        leftSidebarExpandedProperty.set(false)
      }
    }
  }

  private def handleDocumentsImported(): Unit =
    RemoteTableList.reloadFirstPage(documentsProperty, pageSize = documentsPageSize)

  private def reloadIssues(): Unit =
    if (currentDocumentProperty.get != null && currentDocumentProperty.get.id != null && currentDocumentProperty.get.id.get != null) {
      RemoteTableList.reloadFirstPage(issuesProperty, pageSize = issuesPageSize)
      RemoteTableList.reloadFirstPage(curationCandidatesProperty, pageSize = provenancePageSize)
    } else {
      issuesProperty.clear()
      curationCandidatesProperty.clear()
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
                                       expanded: Property[Boolean],
                                       createNewDocument: () => Unit,
                                       openDocument: Document => Unit,
                                       onDocumentsImported: () => Unit
                                     ) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("doc-panel", "glass", "doc-sidebar", "doc-sidebar-left")
    style {
      height = "100%"
      display <-- expanded.map(isExpanded => if (isExpanded) "block" else "none")
    }

    withDslContext {
      vbox {
        classes = "doc-panel-shell"
        style {
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

          button("toggle-left-sidebar") {
            buttonType = "button"
            classes = Seq("material-icons", "doc-icon-btn", "doc-sidebar-toggle")
            text = "left_panel_close"
            onClick(_ => expanded.set(!expanded.get))
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

        observeRender(currentDocument) { document =>
          Navigation.renderByRel("import-documents", document.links) { () =>
            DocumentImportPanel.panel(document, onDocumentsImported)
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
            showHeader = true

            column[Data[Document], String]("Titel") {
              val current = summon[TableColumn[Data[Document], String]]
              prefWidth = 400.0
              current.setSortable(true)
              current.setSortKey("title")
              cellValueFactory = (features: TableColumn.CellDataFeatures[Data[Document], String]) => features.value.data.title
              cellFactory = (column: TableColumn[Data[Document], String]) => new DocumentSummaryCell()
            }
          }

          table.classProperty += "doc-table"

          addDisposable(
            table.getSelectionModel.selectedItemProperty.observe { selected =>
              if (selected != null) {
                openDocument(selected.data)
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
            expanded: Property[Boolean],
            createNewDocument: () => Unit,
            openDocument: Document => Unit,
            onDocumentsImported: () => Unit)(using Scope): DocumentListPanel =
    CompositeSupport.buildComposite(new DocumentListPanel(documents, currentDocument, searchQuery, expanded, createNewDocument, openDocument, onDocumentsImported))
}

private final class DocumentImportPanel(document: Document, onImported: () => Unit) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val expandedProperty = Property(false)
  private val importPathProperty = Property("")
  private val overwriteExistingProperty = Property(false)
  private val importingProperty = Property(false)

  override protected def compose(using DslContext): Unit = {
    classes = "doc-import"

    withDslContext {
      vbox {
        classes = "doc-import__shell"

        hbox {
          classes = "doc-import__header"

          vbox {
            classes = "doc-import__copy"

            span {
              classes = "doc-import__eyebrow"
              text = "Admin"
            }

            span {
              classes = "doc-import__title"
              text = "Import"
            }
          }

          button("toggle-import-panel") {
            buttonType = "button"
            classes = Seq("material-icons", "doc-icon-btn")
            text = "upload_file"
            onClick(_ => expandedProperty.set(!expandedProperty.get))
          }
        }

        div {
          classes = "doc-import__panel"
          style {
            display <-- expandedProperty.map(expanded => if (expanded) "block" else "none")
          }

          vbox {
            classes = "doc-import__fields"

            span {
              classes = "doc-import__intro"
              text = "Rekursiv nach Text.md suchen und Markdown als Dokumentinhalt importieren."
            }

            div {
              classes = "doc-import__path"

              standaloneInput("import-path") {
                placeholder = "C:\\docs\\wissen"
                subscribeBidirectional(importPathProperty, stringValueProperty)
              }
            }

            hbox {
              classes = "doc-import__options"

              input("overwrite-existing") {
                inputType = "checkbox"
                subscribeBidirectional(overwriteExistingProperty, booleanValueProperty)
              }

              span {
                classes = "doc-import__option-label"
                text = "Vorhandene Titel ueberschreiben"
              }
            }

            button("Import starten") {
              buttonType = "button"
              classes = "doc-new-btn"
              onClick(_ => triggerImport())
            }
          }
        }
      }
    }
  }

  private def triggerImport(): Unit = {
    val importLink = Navigation.linkByRel("import-documents", document.links).orNull
    val normalizedPath = importPathProperty.get.trim

    if (normalizedPath.isEmpty) {
      Viewport.notify("Bitte ein Importverzeichnis angeben.", Viewport.NotificationKind.Error)
      return
    }
    if (importLink == null) {
      Viewport.notify("Import-Link ist nicht verfuegbar.", Viewport.NotificationKind.Error)
      return
    }
    if (importingProperty.get) {
      return
    }

    importingProperty.set(true)

    Api.request(Navigation.prefixedServiceUrl(importLink.url))
      .post(
        js.Dynamic.literal(
          path = normalizedPath,
          overwriteExisting = overwriteExistingProperty.get
        )
      )
      .raw[js.Dynamic]
      .onComplete {
      case Success(raw) =>
        importingProperty.set(false)
        val result = raw.asInstanceOf[js.Dynamic]
        val files = readInt(result, "files")
        val created = readInt(result, "created")
        val updated = readInt(result, "updated")
        val skipped = readInt(result, "skipped")

        Viewport.notify(
          s"Import abgeschlossen: $files Dateien, $created neu, $updated aktualisiert, $skipped uebersprungen.",
          Viewport.NotificationKind.Success
        )
        expandedProperty.set(false)
        onImported()

      case Failure(error) =>
        importingProperty.set(false)
        Api.logFailure("document import", error)
        Viewport.notify(s"Import fehlgeschlagen: ${error.getMessage}", Viewport.NotificationKind.Error)
    }
  }

  private def readInt(dynamic: js.Dynamic, key: String): Int = {
    val value = dynamic.selectDynamic(key)
    if (js.isUndefined(value) || value == null) 0
    else value.asInstanceOf[Int]
  }
}

private object DocumentImportPanel {
  def panel(document: Document, onImported: () => Unit)(using Scope): DocumentImportPanel =
    CompositeSupport.buildComposite(new DocumentImportPanel(document, onImported))
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
      subtitleProperty.set(s"${document.bookname.get} - ${TimeAgo.format(document.created.get)}")
      visibleProperty.set(true)
    }
  }
}

private final class DocumentEditorPanel(
                                         document: Document,
                                         provenance: ListProperty[Data[CurationCandidate]],
                                         onSaved: Data[Document] => Unit,
                                         onOpenDocumentLink: String => Unit
                                       ) extends DivComposite {

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

//        SectionProvenancePanel.panel(document, provenance)

        val editorField = editor("editor") {
          classes = Seq("doc-editor", "glass")
          style {
            flex = "1"
            minHeight = "0px"
          }

          basePlugin {}
          headingPlugin {}
          listPlugin {}
          documentLinkPlugin {
            val current = summon[DocumentLinkPlugin]
            current.searchDocuments = (query, limit) =>
              Document
                .list(0, limit, query)
                .map(_.rows.iterator.map { data =>
                  val document = data.data
                  DocumentLinkSuggestion(
                    href = s"/document/documents/document/${document.id.get}",
                    title = Option(document.title.get).filter(_.trim.nonEmpty).getOrElse("(Ohne Titel)"),
                    subtitle = document.created.get
                  )
                }.toSeq)
          }
          imagePlugin {}

          button("save") {
            classes = Seq("material-icons", "doc-icon-btn")
          }
        }

        editorField.onInternalDocumentLinkNavigate = href => onOpenDocumentLink(href)

        subscribeBidirectional(document.editable, editorField.editableProperty)
      }
    }
  }
}

private object DocumentEditorPanel {
  def panel(document: Document, provenance: ListProperty[Data[CurationCandidate]], onSaved: Data[Document] => Unit, onOpenDocumentLink: String => Unit)(using Scope): DocumentEditorPanel =
    CompositeSupport.buildComposite(new DocumentEditorPanel(document, provenance, onSaved, onOpenDocumentLink))
}

private final class IssuesPanel(
                                 currentDocument: Property[Document],
                                 issues: ListProperty[Issue],
                                 provenance: ListProperty[Data[CurationCandidate]],
                                 expanded: Property[Boolean]
                               ) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("doc-panel", "glass", "doc-sidebar", "doc-sidebar-right")
    style {
      height = "100%"
      display <-- expanded.map(isExpanded => if (isExpanded) "block" else "none")
    }

    withDslContext {
      vbox {
        classes = "doc-panel-shell"
        style {
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

          button("toggle-right-sidebar") {
            buttonType = "button"
            classes = Seq("material-icons", "doc-icon-btn", "doc-sidebar-toggle")
            text = "right_panel_close"
            onClick(_ => expanded.set(!expanded.get))
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
  def panel(currentDocument: Property[Document], issues: ListProperty[Issue], provenance: ListProperty[Data[CurationCandidate]], expanded: Property[Boolean])(using Scope): IssuesPanel =
    CompositeSupport.buildComposite(new IssuesPanel(currentDocument, issues, provenance, expanded))
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

private final class ProvenanceListItem(entry: Data[CurationCandidate]) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("glass-border", "issue-card")
    style {
      padding = "12px"
      borderRadius = "14px"
    }

    withDslContext {
      vbox {
        style {
          rowGap = "8px"
        }

        hbox {
          style {
            columnGap = "8px"
            alignItems = "center"
          }

          span {
            style {
              fontWeight = "600"
            }
            text = entry.data.resonanceType.get
          }

          span {
            style {
              opacity = "0.66"
            }
            text = entry.data.status.get
          }
        }

        span {
          text = Option(entry.data.title.get).filter(_.trim.nonEmpty).getOrElse("Resonanz")
        }

        span {
          text = entry.data.excerpt.get
        }

        entry.data.decisions.iterator.toSeq.headOption.foreach { decision =>
          vbox {
            style {
              rowGap = "4px"
            }

            span {
              style {
                fontSize = "12px"
                opacity = "0.66"
              }
              text = s"${decision.decisionType.get} - ${decision.decidedBy.get}"
            }

            if (decision.note.get != null && decision.note.get.nn.trim.nonEmpty) {
              span {
                text = decision.note.get.nn
              }
            }
          }
        }
      }
    }
  }
}

private object ProvenanceListItem {
  def item(entry: Data[CurationCandidate])(using Scope): ProvenanceListItem =
    CompositeSupport.buildComposite(new ProvenanceListItem(entry))
}

private final class SectionProvenancePanel(document: Document, provenance: ListProperty[Data[CurationCandidate]]) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classes = Seq("glass-border", "issue-card")
    style {
      padding = "14px"
      borderRadius = "16px"
      marginBottom = "14px"
    }

    observeRender(document.editor) { _ =>
      val grouped = groupBySection(document, provenance.iterator.toSeq)

      if (grouped.nonEmpty) {
        vbox {
          style {
            rowGap = "12px"
          }

          span {
            style {
              fontWeight = "600"
            }
            text = "Entstanden aus Resonanzen"
          }

          grouped.foreach { group =>
            vbox {
              style {
                rowGap = "8px"
              }

              span {
                style {
                  fontSize = "13px"
                  opacity = "0.72"
                }
                text = group.label
              }

              group.entries.foreach { entry =>
                ProvenanceListItem.item(entry)
              }
            }
          }
        }
      } else {
        div {
          style {
            display = "none"
          }
        }
      }
    }
  }

  private def groupBySection(document: Document, entries: Seq[Data[CurationCandidate]]): Seq[SectionGroup] = {
    val sections = extractSections(document.editor.get)
    val groupedEntries = entries.groupBy { entry =>
      Option(entry.data.target.get)
        .flatMap(target => Option(target.sectionId))
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse("")
    }

    val sectionGroups = sections.flatMap { section =>
      groupedEntries.get(section.id).filter(_.nonEmpty).map(entries => SectionGroup(section.label, entries))
    }

    val unmatched = groupedEntries.iterator
      .filter { case (key, value) => key.nonEmpty && value.nonEmpty && !sections.exists(_.id == key) }
      .map { case (key, value) => SectionGroup(s"Abschnitt: $key", value) }
      .toSeq

    val root = groupedEntries.get("").filter(_.nonEmpty).map(entries => SectionGroup("Dokument", entries)).toSeq

    sectionGroups ++ unmatched ++ root
  }

  private def extractSections(editorValue: Any): Seq[SectionRef] = {
    val root = editorValue.asInstanceOf[js.Dynamic]
    if (root == null || js.isUndefined(root)) {
      Seq.empty
    } else {
      readChildren(root).flatMap(readSection)
    }
  }

  private def readSection(node: js.Dynamic): Option[SectionRef] = {
    val nodeType = readString(node, "type")
    if (nodeType != "heading") {
      None
    } else {
      val title = readText(node).trim
      if (title.isEmpty) None
      else Some(SectionRef(slug(title), title))
    }
  }

  private def readChildren(node: js.Dynamic): Seq[js.Dynamic] = {
    val content = node.selectDynamic("content")
    if (js.isUndefined(content) || content == null) Seq.empty
    else content.asInstanceOf[js.Array[js.Dynamic]].toSeq
  }

  private def readText(node: js.Dynamic): String = {
    val text = readString(node, "text")
    val nested = readChildren(node).map(readText).mkString(" ")
    s"$text $nested".trim.replaceAll("\\s+", " ")
  }

  private def readString(node: js.Dynamic, field: String): String = {
    val value = node.selectDynamic(field)
    if (js.isUndefined(value) || value == null) ""
    else value.toString
  }

  private def slug(value: String): String =
    value.toLowerCase
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-+|-+$)", "")
}

private object SectionProvenancePanel {
  def panel(document: Document, provenance: ListProperty[Data[CurationCandidate]])(using Scope): SectionProvenancePanel =
    CompositeSupport.buildComposite(new SectionProvenancePanel(document, provenance))
}

private final case class SectionRef(id: String, label: String)

private final case class SectionGroup(label: String, entries: Seq[Data[CurationCandidate]])
