package app.pages.followers

import app.domain.core.Data
import app.domain.core.Table
import app.domain.followers.RelationShip
import jFx2.controls.image
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.style
import jFx2.core.template
import jFx2.router.PageInfo
import jFx2.router.navigateByRel
import jFx2.layout.div
import jFx2.state.Property
import jFx2.table.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["relation-ships-page"])
class RelationShipsPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Following"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    private val provider = RelationShipsProvider()
    private val job = SupervisorJob()
    private val cs = CoroutineScope(job)
    private val model = LazyTableModel(cs, provider, pageSize = 200, prefetchPages = 2)

    fun model(table: Table<Data<RelationShip>>) {
        model.setAll(table.rows)
        model.totalCount.set(table.size)
    }

    context(scope: NodeScope)
    fun afterBuild() {

        onDispose { job.cancel() }

        template {
            div {
                className { "relation-ship-page-table" }

                tableView(model, rowHeightPx = 64) {

                    columnProperty("image", "Bild", 160, valueProperty = { it.data.follower.get().image }) {
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
                        valueProperty = { it.data.follower.get().nickName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    columnProperty(
                        id = "firstName",
                        header = "Vorname",
                        prefWidthPx = 200,
                        valueProperty = { it.data.follower.get().info.get()?.firstName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    columnProperty(
                        id = "lastName",
                        header = "Nachname",
                        prefWidthPx = 200,
                        valueProperty = { it.data.follower.get().info.get()?.lastName },
                        cellFactory = {
                            val host = scope.create<HTMLDivElement>("div")
                            TextCell(host)
                        }
                    )

                    columnProperty("group", "Gruppe", 160, valueProperty = { it.data.groups }) {
                        ComponentCell(
                            outerScope = scope,
                            node = scope.create("div"),
                            render = { row, idx, v ->
                                template {
                                    div {
                                        text(v?.map { it.name }?.joinToString(", ") ?: "Keine Gruppe")
                                    }
                                }
                            }
                        )
                    }

                    onRowDoubleClick { user, _ ->
                        navigateByRel("read", user.data.links) { navigate -> navigate() }
                    }
                }
            }
        }

    }

    companion object {
        class RelationShipsProvider : DataProvider<Data<RelationShip>> {
            override val totalCount = Property<Int?>(100_000)
            override val sortState: Property<SortState?> = Property(null)

            override suspend fun loadRange(offset: Int, limit: Int): List<Data<RelationShip>> {
                val table = RelationShip.list(offset, limit)

                totalCount.set(table.size)

                return table.rows
            }
        }
    }


}
