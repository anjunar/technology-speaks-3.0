@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package app.pages.timeline

import app.components.commentable.commentsSection
import app.components.likeable.likeButton
import app.components.shared.componentHeader
import app.domain.core.AbstractEntity
import app.domain.core.Data
import app.domain.core.Table
import app.domain.shared.FirstComment
import app.domain.timeline.Post
import app.services.ApplicationService
import jFx2.controls.button
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
import jFx2.forms.editor.plugins.basePlugin
import jFx2.forms.editor.plugins.headingPlugin
import jFx2.forms.editor.plugins.imagePlugin
import jFx2.forms.editor.plugins.linkPlugin
import jFx2.forms.editor.plugins.listPlugin
import jFx2.forms.form
import jFx2.forms.input
import jFx2.layout.div
import jFx2.layout.vbox
import jFx2.router.PageInfo
import jFx2.state.JobRegistry
import jFx2.state.Property
import jFx2.virtual.RangeDataProvider
import jFx2.virtual.virtualList
import org.w3c.dom.HTMLDivElement

private class RangeProvider(
    private val post: Post,
    override val maxItems: Int = 5000,
    override val pageSize: Int = 50
) : RangeDataProvider<Data<out AbstractEntity>>() {

    override suspend fun fetch(index: Int, limit: Int): Table<out Data<out AbstractEntity>> =
        FirstComment.list(index, limit, post)
}

@JfxComponentBuilder(classes = ["post-view-page", "container"])
class PostViewPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Post"
    override val width: Int = - 1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    private var createRel: String = "save"

    private val model = Property(Data(Post(user = Property(ApplicationService.app.get().user))))

    fun model(value : Data<Post>) {
        model.set(value)
    }

    context(scope: NodeScope)
    fun afterBuild() {

        val provider = RangeProvider(model.get().data)
        provider.upsert(model.get())

        template {

            vbox {

                div {

                    style {
                        flex = "1"
                        minHeight = "0px"
                    }

                    virtualList(
                        dataProvider = provider,
                        estimateHeightPx = 240,
                        overscanPx = 240,
                        prefetchItems = 40,
                        renderer = { item, _ ->

                            template {
                                if (item == null) {
                                    div {
                                        className { "glass-border" }

                                        style {
                                            height = "200px"
                                        }

                                        componentHeader {

                                        }

                                        vbox {
                                            style {
                                                justifyContent = "center"
                                                alignItems = "center"
                                            }
                                            text("Laden...")
                                        }
                                    }
                                } else {


                                    when(item.data) {
                                        is Post -> {
                                            form(model = item.data, clazz = Post::class) {

                                                disabled = true

                                                style {
                                                    padding = "10px"
                                                    height = "calc(100% - 20px)"
                                                }

                                                vbox {
                                                    componentHeader {
                                                        model(model)
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
                                        is FirstComment -> {
                                            vbox {

                                                className { "glass-border" }

                                                componentHeader {
                                                    model(item.data)

                                                    onDelete {
                                                        JobRegistry.instance.launch("Comment Remove", "Comment") {
                                                            item.data.delete(this@PostViewPage.model.get().data)
                                                            provider.remove(item)
                                                        }
                                                    }

                                                    onUpdate {
                                                        val editable = item.data.editable
                                                        editable.set(! editable.get())
                                                    }
                                                }

                                                form(model = item.data, clazz = FirstComment::class) {

                                                    subscribeBidirectional(this@form.model.editable, editable)

                                                    onSubmit {
                                                        if (model.id == null) {
                                                            model.save(this@PostViewPage.model.get().data)
                                                        } else {
                                                            model.update(this@PostViewPage.model.get().data)
                                                        }
                                                        item.data.editable.set(false)
                                                    }

                                                    editor("editor") {

                                                        basePlugin { }
                                                        headingPlugin { }
                                                        listPlugin { }
                                                        linkPlugin { }
                                                        imagePlugin { }

                                                        button("save") {
                                                            className { "material-icons" }
                                                        }

                                                        subscribeBidirectional(this@form.model.editor, valueProperty)
                                                    }

                                                }

                                                likeButton {
                                                    model(item.data.likes, item.data.links)
                                                }

                                                commentsSection {
                                                    model(item.data, this@PostViewPage.model.get().data)
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                input("newComment") {
                    style {
                        margin = "12px"
                        padding = "12px"
                        width = "calc(100% - 48px)"
                        backgroundColor = "var(--color-background-secondary)"
                        fontSize = "24px"
                        borderRadius = "8px"
                    }

                    placeholder = "Neuer Kommentar..."
                    onClick {
                        val lastItem = provider.getOrNull(provider.items.size - 1)
                        if (lastItem!!.data is FirstComment) {
                            if (! lastItem.data.editable.get()) {
                                val firstComment = FirstComment()
                                firstComment.editable.set(true)
                                firstComment.user = Property(ApplicationService.app.get().user)
                                provider.upsert(Data(firstComment))
                            }
                        } else {
                            val firstComment = FirstComment()
                            firstComment.editable.set(true)
                            firstComment.user = Property(ApplicationService.app.get().user)
                            provider.upsert(Data(firstComment))
                        }
                    }
                }
            }


        }

    }
}