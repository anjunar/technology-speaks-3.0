@file:OptIn(ExperimentalUuidApi::class)

package app.components.commentable

import app.components.likeable.likeButton
import app.components.shared.componentHeader
import app.domain.core.AbstractEntity
import app.domain.shared.FirstComment
import app.domain.shared.SecondComment
import app.services.ApplicationService
import jFx2.controls.button
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.rendering.foreach
import jFx2.core.rendering.observeRender
import jFx2.core.template
import jFx2.forms.editor
import jFx2.forms.editor.plugins.*
import jFx2.forms.form
import jFx2.layout.div
import jFx2.layout.vbox
import jFx2.state.JobRegistry
import jFx2.state.Property
import org.w3c.dom.HTMLDivElement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JfxComponentBuilder(classes = ["comments-section"])
class CommentsSection(override val node: HTMLDivElement) : Component<HTMLDivElement>() {

    private val firstComment = Property(FirstComment())
    private lateinit var owner : AbstractEntity

    fun model(value : FirstComment, owner : AbstractEntity) {
        this.firstComment.set(value)
        this.owner = owner
    }

    context(scope: NodeScope)
    fun afterBuild() {
        val createLink = firstComment.get().links.get().firstOrNull { it.rel == "updateChildren" } ?: return

        val busy = Property(false)

        template {

            div {
                vbox {

                    style {
                        alignItems = "flex-end"
                    }

                    button("Kommentieren") {
                        type("button")
                        onClick {
                            val lastComment = firstComment.get().comments.lastOrNull()
                            if (lastComment != null && lastComment.editable.get()) return@onClick
                            val secondComment = SecondComment()
                            secondComment.id = Property(Uuid.generateV4().toString())
                            secondComment.editable.set(true)
                            firstComment.get().comments.add(secondComment)
                        }

                    }
                }
            }

            observeRender(firstComment) { firstComment ->
                foreach(firstComment.comments, {key -> key.id!!.get()}) { comment, index ->

                    form(model = comment, clazz = SecondComment::class) {

                        subscribeBidirectional(this@form.model.editable, editable)

                        vbox {
                            onSubmit {
                                busy.set(true)
                                try {
                                    this@CommentsSection.firstComment.get().update(owner)
                                    comment.user = Property(ApplicationService.app.get().user)
                                    comment.editable.set(false)
                                } finally {
                                    busy.set(false)
                                }
                            }

                            className { "glass-border" }

                            componentHeader {
                                model(this@form.model)

                                onDelete {
                                    JobRegistry.instance.launch("Comment Remove", "Comment") {
                                        firstComment.comments.remove(comment)
                                        this@CommentsSection.firstComment.get().update(owner)
                                    }
                                }

                                onUpdate {
                                    val editable = this@form.model.editable
                                    editable.set(! editable.get())
                                }

                            }

                            editor("editor") {

                                style {
                                    flex = "1"
                                    minHeight = "0px"
                                }

                                basePlugin { }
                                headingPlugin { }
                                listPlugin { }
                                linkPlugin { }
                                imagePlugin { }

                                button("save") {
                                    className { "material-icons hover" }
                                }

                                subscribeBidirectional(this@form.model.editor, valueProperty)
                            }

                            likeButton {
                                model(this@form.model.likes, this@form.model.links)
                            }

                        }


                    }
                }
            }

        }
    }
}