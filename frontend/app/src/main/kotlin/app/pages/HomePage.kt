package app.pages

import app.domain.core.Data
import app.domain.core.User
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.template
import jFx2.forms.comboBox
import jFx2.layout.div
import jFx2.router.PageInfo
import jFx2.state.Property
import jFx2.table.DataProvider
import jFx2.table.SortState
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["home-page"])
class HomePage(override var node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Home"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    context(scope: NodeScope)
    fun afterBuild() {

        template {

            comboBox(
                name = "placeholder",
                provider = object : DataProvider<Data<User>> {
                    override val totalCount: Property<Int?> = Property(100)
                    override val sortState: Property<SortState?> = Property(null)
                    override suspend fun loadRange(offset: Int, limit: Int): List<Data<User>> {
                        val table = User.list(offset, limit)
                        totalCount.set(table.size)
                        return table.rows
                    }
                }
            ) {

                placeholder("test")

                itemRenderer { user, _ ->
                    template { div {
                        text(user.data.nickName.get())
                    } }
                }

                valueRenderer { it.data.nickName.get() }

            }

        }

    }
}
