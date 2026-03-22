package app.pages.timeline

import app.components.likeable.likeButton
import app.components.shared.componentHeader
import app.domain.core.Data
import app.domain.core.Table
import app.domain.timeline.Post
import app.domain.timeline.PostCreated
import app.domain.timeline.PostUpdated
import app.services.ApplicationService
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.template
import jFx2.forms.editor
import jFx2.forms.editor.plugins.*
import jFx2.forms.form
import jFx2.forms.input
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.layout.vbox
import jFx2.router.PageInfo
import jFx2.router.navigate
import jFx2.virtual.RangeDataProvider
import jFx2.virtual.virtualList
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["posts-page"])
class PostsPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {
    override val name: String = "Posts"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}
    private val provider = PostRangeProvider()

    fun model(table : Table<Data<Post>>) {
        provider.setAll(table.rows, table.size)
    }

    context(scope: NodeScope)
    fun afterBuild() {
        scope.dispose.register(
            ApplicationService.messageBus.subscribe { message ->
                when (message) {
                    is PostCreated -> provider.upsert(message.post)
                    is PostUpdated -> provider.upsert(message.post)
                }
            })

        val formular = Post()

        template {
            style {
                height = "100%"
                width = "100%"
                setProperty("overflow", "hidden")
            }

            vbox {

                form(model = formular, clazz = Post::class) {

                    input("post") {

                        style {
                            margin = "12px"
                            padding = "12px"
                            width = "calc(100% - 48px)"
                            backgroundColor = "var(--color-background-secondary)"
                            fontSize = "24px"
                            borderRadius = "8px"
                        }

                        placeholder = "Nach was ist dir heute?"

                        onClick {
                            navigate("/timeline/posts/post")
                        }

                    }

                }

                div {
                    style {
                        flex = "1"
                        minHeight = "0px"
                    }

                    virtualList(
                        dataProvider = provider,
                        estimateHeightPx = 44,
                        overscanPx = 240,
                        prefetchItems = 80,
                        renderer = { item, index ->

                            template {

                                form(model = item?.data, clazz = Post::class) {

                                    disabled = true

                                    className { "glass-border" }

                                    if (item == null) {
                                        text("Loading...")
                                    } else {
                                        componentHeader {
                                            model(item.data)
                                        }

                                        editor("editor") {
                                            style {
                                                height = "100%"
                                                width = "100%"
                                            }

                                            basePlugin { }
                                            headingPlugin { }
                                            listPlugin { }
                                            linkPlugin { }
                                            imagePlugin { }

                                            subscribeBidirectional(this@form.model.editor, valueProperty)

                                        }


                                        hbox {
                                            style {
                                                columnGap = "8px"
                                                alignItems = "center"
                                            }

                                            likeButton {
                                                model(
                                                    likes = this@form.model.likes, links = item.data.links
                                                )
                                            }

                                            input("comment") {
                                                style {
                                                    flex = "1"
                                                    padding = "8px"
                                                    borderRadius = "6px"
                                                    backgroundColor = "var(--color-background-secondary)"
                                                    border = "1px solid var(--color-background-primary)"
                                                }
                                                placeholder = "Kommentar schreiben..."

                                                onClick {
                                                    navigate("/timeline/posts/post/${item.data.id?.get()}/view")
                                                }

                                            }
                                        }


                                    }

                                }

                            }
                        })
                }
            }
        }
    }

    companion object {
        class PostRangeProvider(
            override val maxItems: Int = 5000, override val pageSize: Int = 50
        ) : RangeDataProvider<Data<Post>>() {

            override suspend fun fetch(index: Int, limit: Int): Table<Data<Post>> {
                val table = Post.list(index, limit)

                knownCount = table.size
                hasKnownCount = true

                return table
            }
        }
    }

}