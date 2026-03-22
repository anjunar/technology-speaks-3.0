package app.pages.core

import app.domain.core.Data
import app.domain.core.Table
import app.domain.core.User
import jFx2.client.JsonClient
import jFx2.controls.image
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.style
import jFx2.core.template
import jFx2.layout.div
import jFx2.router.PageInfo
import jFx2.router.navigate
import jFx2.router.navigateByRel
import jFx2.state.Property
import jFx2.table.ComponentCell
import jFx2.table.DataProvider
import jFx2.table.LazyTableModel
import jFx2.table.SortState
import jFx2.table.TextCell
import jFx2.table.tableView
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.w3c.dom.CustomEvent
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["users-page"])
class UsersPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Users"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    private val provider = UsersProvider()
    private val job = SupervisorJob()
    private val cs = CoroutineScope(job)
    private val model = LazyTableModel(cs, provider, pageSize = 200, prefetchPages = 2)

    fun model(table : Table<Data<User>>) {
        model.setAll(table.rows)
        model.totalCount.set(table.size)
    }

    context(scope: NodeScope)
    fun afterBuild() {

        onDispose { job.cancel() }

        template {
            div {
                className { "users-page-table" }

                tableView(model, rowHeightPx = 64) {

                    columnProperty("image", "Bild", 160, valueProperty = { it.data.image }) {
                        ComponentCell(
                            outerScope = scope,
                            node = scope.create("div"),
                            render = { row, idx, v ->
                                template {
                                    if (v == null) {
                                        div {
                                            className { "material-icons" }
                                            style {
                                                fontSize = "64px"
                                            }
                                            text("account_circle")
                                        }
                                    } else {
                                        image {
                                            style {
                                                height = "64px"
                                                width = "64px"
                                            }
                                            src = v.thumbnailLink()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    columnProperty(
                        id = "nickName",
                        header = "Nick Name",
                        prefWidthPx = 200,
                        valueProperty = { it.data.nickName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    columnProperty(
                        id = "firstName",
                        header = "Vorname",
                        prefWidthPx = 200,
                        valueProperty = { it.data.info.get()?.firstName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    columnProperty(
                        id = "lastName",
                        header = "Nachname",
                        prefWidthPx = 200,
                        valueProperty = { it.data.info.get()?.lastName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    onRowDoubleClick { user, _ ->
                        navigateByRel("read", user.data.links) { navigate -> navigate() }
                    }
                }
            }
        }



    }

    companion object {
        class UsersProvider : DataProvider<Data<User>> {
            override val totalCount = Property<Int?>(100_000)
            override val sortState: Property<SortState?> = Property(null)

            override suspend fun loadRange(offset: Int, limit: Int): List<Data<User>> {
                val table = User.list(offset, limit)

                totalCount.set(table.size)

                return table.rows
            }
        }
    }

}