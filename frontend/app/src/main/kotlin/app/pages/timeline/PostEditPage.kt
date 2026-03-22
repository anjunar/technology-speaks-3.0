package app.pages.timeline

import app.components.shared.componentHeader
import app.domain.core.Data
import app.domain.timeline.Post
import app.domain.timeline.PostCreated
import app.domain.timeline.PostUpdated
import app.services.ApplicationService
import jFx2.controls.button
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
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
import jFx2.layout.vbox
import jFx2.router.PageInfo
import jFx2.state.Property
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["post-edit-page", "container"])
class PostEditPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Posts"
    override val width: Int = - 1
    override val height: Int = -1
    override val resizable: Boolean = true
    override var close: () -> Unit = {}

    private val model = Property(Data(Post(user = Property(ApplicationService.app.get().user))))

    fun model(value : Data<Post>) {
        model.set(value)
    }

    context(scope: NodeScope)
    fun afterBuild() {

        template {

            form(model = model.get().data, clazz = Post::class) {

                onSubmit {
                    if (model.id == null) {
                        val saved = model.save()
                        ApplicationService.messageBus.publish(PostCreated(saved))
                        close()
                    } else {
                        val updated = model.update()
                        ApplicationService.messageBus.publish(PostUpdated(updated))
                        close()
                    }
                }

                style {
                    padding = "10px"
                    height = "calc(100% - 20px)"
                }

                vbox {
                    componentHeader {
                        model(model)
                    }

                    editor("editor") {
                        style {
                            flex = "1"
                        }

                        basePlugin { }
                        headingPlugin { }
                        listPlugin { }
                        linkPlugin { }
                        imagePlugin { }

                        subscribeBidirectional(this@form.model.editor, valueProperty)
                    }

                    button("Senden") {
                        style {
                            width = "100%"
                        }
                        className { "btn-secondary" }
                    }
                }

            }

        }

    }
}