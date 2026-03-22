package app.pages.documents

import app.components.shared.componentHeader
import app.domain.core.Data
import app.domain.core.Table
import app.domain.documents.Document
import app.domain.documents.Issue
import app.domain.documents.IssueCreated
import app.domain.documents.IssueUpdated
import app.services.ApplicationService
import jFx2.encodeURIComponent
import jFx2.controls.button
import jFx2.controls.heading
import jFx2.controls.span
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.rendering.observeRender
import jFx2.core.template
import jFx2.forms.editor
import jFx2.forms.editor.plugins.basePlugin
import jFx2.forms.editor.plugins.headingPlugin
import jFx2.forms.editor.plugins.imagePlugin
import jFx2.forms.editor.plugins.listPlugin
import jFx2.forms.editor.plugins.linkPlugin
import jFx2.forms.form
import jFx2.forms.input
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.layout.vbox
import jFx2.router.PageInfo
import jFx2.router.navigateByRel
import jFx2.router.renderByRel
import jFx2.state.Property
import jFx2.table.ComponentCell
import jFx2.table.DataProvider
import jFx2.table.LazyTableModel
import jFx2.table.SelectionMode
import jFx2.table.SortState
import jFx2.table.tableView
import jFx2.virtual.RangeDataProvider
import jFx2.virtual.virtualList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.time.Clock

@JfxComponentBuilder(classes = ["document-page"])
class DocumentPage(override var node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Document"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    private val model = Property(Document())

    private val searchQuery = Property("")
    private val provider = DocumentsProvider(searchQuery)
    private val job = SupervisorJob()
    private val cs = CoroutineScope(job)
    private val tableModel = LazyTableModel(cs, provider, pageSize = 200, prefetchPages = 2)

    fun model(value : Document, documents : Table<Data<Document>>) {
        model.set(value)
        tableModel.setAll(documents.rows)
        tableModel.totalCount.set(documents.size)
    }

    fun timeAgo(dateTime: LocalDateTime, clock: Clock = Clock.System): String {
        val now = clock.now()
        val zone = TimeZone.currentSystemDefault()

        val createdInstant = dateTime.toInstant(zone)
        val duration = now - createdInstant

        val hours = duration.inWholeHours
        val days = duration.inWholeDays

        return when {
            days > 0 -> "vor $days Tagen"
            hours > 0 -> "vor $hours Stunden"
            else -> "vor ${duration.inWholeMinutes} Minuten"
        }
    }

    context(scope: NodeScope)
    fun afterBuild() {

        val issuesProvider = IssuesRangeProvider(document = model)
        onDispose { job.cancel() }

        scope.dispose.register(
            ApplicationService.messageBus.subscribe { message ->
                when (message) {
                    is IssueCreated -> issuesProvider.upsert(message.post)
                    is IssueUpdated -> issuesProvider.upsert(message.post)
                }
            })

        template {
            style {
                height = "100%"
                width = "100%"
                setProperty("overflow", "hidden")
            }

            hbox {
                node.classList.add("documents-layout")
                style {
                    height = "100%"
                    width = "100%"
                }

                vbox {
                    node.classList.add("doc-panel")

                    style {
                        width = "420px"
                    }

                    hbox {
                        node.classList.add("doc-panel-header")

                        span {
                            node.classList.add("doc-panel-title")
                            text("Dokumente")
                        }
                    }

                    div {
                        node.classList.add("doc-search")

                        span {
                            node.classList.add("material-icons")
                            text("search")
                        }

                        input("search") {
                            type("search")
                            placeholder = "Suche..."
                            subscribeBidirectional(searchQuery, valueProperty)
                        }
                    }

                    val docsTable = tableView(tableModel, rowHeightPx = 64, headerVisible = false) {

                        columnProperty("title", "Titel", 400, valueProperty = { it.data.title }) {
                            ComponentCell(
                                outerScope = scope,
                                node = scope.create("div"),
                                render = { row, idx, v ->
                                    template {
                                        hbox {

                                            style {
                                                alignItems = "center"
                                                columnGap = "10px"
                                                width = "100%"
                                            }

                                            span {
                                                node.classList.add("material-icons")
                                                style {
                                                    fontSize = "18px"
                                                    opacity = "0.75"
                                                }
                                                text("description")
                                            }

                                            vbox {
                                                style {
                                                    setProperty("overflow", "hidden")
                                                }
                                                div {
                                                    style {
                                                        fontWeight = "600"
                                                        setProperty("overflow", "hidden")
                                                        textOverflow = "ellipsis"
                                                    }

                                                    val title = v?.ifBlank { "(Ohne Titel)" } ?: "(Ohne Titel)"
                                                    text(title)
                                                }
                                                div {

                                                    style {
                                                        fontSize = "12px"
                                                        opacity = "0.75"
                                                    }

                                                    text(timeAgo(row.data.created.get()))
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        onSelectionChanged { selected ->
                            selected.firstOrNull()?.let { model.set(it.data) }
                            issuesProvider.reload()
                        }
                    }

                    docsTable.node.classList.add("doc-table")
                    docsTable.selectionModel.mode.set(SelectionMode.SINGLE)

                    var searchJob: Job? = null
                    onDispose(searchQuery.observeWithoutInitial { _ ->
                        searchJob?.cancel()
                        searchJob = cs.launch {
                            delay(250)
                            (docsTable.node.querySelector(".jfx-table-viewport") as? HTMLElement)?.scrollTop = 0.0
                            docsTable.selectionModel.clearSelection()
                            docsTable.focusModel.focus(null)
                            tableModel.clearCache()
                            provider.totalCount.set(100_000)
                        }
                    })
                    onDispose { searchJob?.cancel() }

                    navigateByRel("create-document", model.get().links) {
                        button("") {
                            node.classList.add("doc-new-btn")
                            style {
                                marginBottom = "12px"
                                display = "flex"
                                alignItems = "center"
                                justifyContent = "center"
                                columnGap = "10px"
                            }
                            type("button")

                            span {
                                node.classList.add("material-icons")
                                style {
                                    fontSize = "20px"
                                    opacity = "0.85"
                                }
                                text("add")
                            }
                            span {
                                text("Neues Dokument")
                            }

                            onClick {
                                docsTable.selectionModel.clearSelection()
                                val document = Document()
                                document.editable.set(true)
                                model.set(document)
                            }
                        }
                    }

                }

                observeRender(model) { observedModel ->
                    form(model = observedModel, clazz = Document::class) {
                        node.classList.add("doc-panel")

                        subscribeBidirectional(model.editable, editable)

                        onSubmit {
                            if (model.id == null) {
                                model.save()
                            } else {
                                model.update()
                            }
                        }

                        style {
                            flex = "1"
                            minWidth = "0"
                            height = "100%"
                            display = "flex"
                            flexDirection = "column"
                        }

                        hbox {
                            node.classList.add("doc-titlebar")
                            input("title") {

                                style {
                                    flex = "1"
                                    minWidth = "0"
                                }

                                placeholder = "Titel"
                                subscribeBidirectional(model.title, valueProperty)
                                onDispose(model.editable.observe { editable -> node.disabled = !editable })
                            }

                            renderByRel("update", model.links) {
                                button("edit") {
                                    type("button")

                                    onClick { model.editable.set(!model.editable.get()) }

                                    node.classList.add("material-icons")
                                    node.classList.add("doc-icon-btn")
                                    onDispose(
                                        model.editable.observe { editable ->
                                            text(if (editable) "done" else "edit")
                                            if (editable) node.classList.add("active") else node.classList.remove("active")
                                        }
                                    )
                                }
                            }
                        }

                        editor("editor") {
                            node.classList.add("doc-editor")

                            basePlugin { }
                            headingPlugin { }
                            listPlugin { }
                            linkPlugin { }
                            imagePlugin { }

                            button("save") {
                                node.classList.add("material-icons")
                                node.classList.add("doc-icon-btn")
                            }

                            subscribeBidirectional(model.editor, valueProperty)
                            subscribeBidirectional(model.editable, editable)

                        }
                    }
                }


                vbox {
                    node.classList.add("doc-panel")

                    style {
                        width = "420px"
                    }

                    hbox {
                        node.classList.add("doc-panel-header")

                        span {
                            node.classList.add("doc-panel-title")
                            text("Aufgaben")
                        }
                    }

                    div {

                        style {
                            flex = "1"
                        }

                        virtualList(
                            dataProvider = issuesProvider,
                            estimateHeightPx = 240,
                            overscanPx = 240,
                            prefetchItems = 80,
                            renderer = { item, index ->

                                template {

                                    if (item == null) {
                                        div {
                                            className { "glass-border" }
                                            style {
                                                height = "200px"
                                            }

                                            componentHeader {}
                                        }

                                    } else {
                                        form(model = item.data, clazz = Issue::class) {

                                            disabled = true

                                            className { "glass-border" }

                                            componentHeader {
                                                model(model)
                                            }

                                            heading(3) {
                                                text(model.title.get())
                                            }

                                            editor("editor") {
                                                basePlugin { }
                                                headingPlugin { }
                                                listPlugin { }
                                                linkPlugin { }
                                                imagePlugin { }

                                                subscribeBidirectional(this@form.model.editor, valueProperty)
                                            }

                                        }
                                    }


                                }
                            })
                    }

                    navigateByRel("create-issue", model.get().links) { navigate ->
                        button("") {
                            node.classList.add("doc-new-btn")
                            style {
                                marginBottom = "12px"
                                display = "flex"
                                alignItems = "center"
                                justifyContent = "center"
                                columnGap = "10px"
                            }
                            type("button")

                            span {
                                node.classList.add("material-icons")
                                style {
                                    fontSize = "20px"
                                    opacity = "0.85"
                                }
                                text("add")
                            }
                            span {
                                text("Neue Aufgabe")
                            }

                            onClick {
                                navigate()
                            }
                        }
                    }

                }

            }
        }


    }

    companion object {
        class DocumentsProvider(private val query: Property<String>) : DataProvider<Data<Document>> {
            override val totalCount = Property<Int?>(100_000)
            override val sortState: Property<SortState?> = Property(null)

            override suspend fun loadRange(offset: Int, limit: Int): List<Data<Document>> {
                val queryValue = query.get().trim()
                val queryParameter = if (queryValue.isBlank()) "" else "&name=${encodeURIComponent(queryValue)}"
                val sortParameter = if (queryValue.isBlank()) "&sort=created:desc" else ""
                val table = Document.list(offset, limit)

                totalCount.set(table.size)

                return table.rows
            }
        }

        class IssuesRangeProvider(override val maxItems: Int = 5000, override val pageSize: Int = 50,val document: Property<Document>) : RangeDataProvider<Data<Issue>>() {

            override suspend fun fetch(index: Int, limit: Int): Table<Data<Issue>> {
                val documentId = document.get().id?.get()
                if (documentId.isNullOrBlank()) return Table<Data<Issue>>(size = 0)

                return Issue.list(index, limit, document.get())

            }
        }
    }

}