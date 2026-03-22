package jFx2.forms.editor.plugins

import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.template
import jFx2.forms.Select
import jFx2.forms.editor.prosemirror.EditorState
import jFx2.forms.editor.prosemirror.EditorView
import jFx2.forms.editor.prosemirror.NodeSpec
import jFx2.forms.editor.prosemirror.Plugin
import jFx2.forms.editor.prosemirror.PluginKey
import jFx2.forms.editor.prosemirror.setBlockType
import jFx2.forms.editor.prosemirror.PluginSpec
import jFx2.jsObject
import jFx2.forms.option
import jFx2.forms.select
import org.w3c.dom.HTMLDivElement
import kotlin.js.json

@JfxComponentBuilder(classes = ["heading-plugin"])
class HeadingPlugin(override val node: HTMLDivElement) : Component<HTMLDivElement>(), EditorPlugin {

    override lateinit var view: EditorView

    lateinit var selectComponent : Select

    override val name: String = "heading"

    private fun activeHeadingLevel(v: EditorView): Int {
        val state = v.state
        val sel = state.selection
        val headingType = state.schema.nodes["heading"] ?: return 0

        var found = 0
        state.doc.nodesBetween(sel.from, sel.to, { node, _, _, _ ->
            if (node.type == headingType) {
                val lvl = node.attrs["level"] ?: 0
                if (lvl in 1..6) {
                    found = lvl
                    return@nodesBetween false // nicht weiter absteigen
                }
            }
            true
        })

        return found
    }

    private fun setParagraph(v: EditorView) {
        val state = v.state
        val paraType = state.schema.nodes["paragraph"] ?: return

        val dispatch = v::dispatch
        setBlockType(paraType)(state, dispatch, v)
        v.focus()
    }

    private fun setHeading(v: EditorView, level: Int) {
        val state = v.state
        val type = state.schema.nodes["heading"] ?: return

        val attrs = json(
            "level" to level
        )

        val dispatch = v::dispatch
        setBlockType(type, attrs)(state, dispatch, v)
        v.focus()
    }

    private fun jsPluginView(
        onUpdate: (view: EditorView, prevState: EditorState?) -> Unit
    ): dynamic {
        return json(
            "update" to { v: dynamic, prev: dynamic ->
                onUpdate(v, prev)
            }
        )
    }

    private fun levelToValue(level: Int): String =
        if (level in 1..6) "h$level" else "p"

    override fun plugin(): Plugin<*> {
        val spec = jsObject<PluginSpec<Unit>> {
            key = KEY
            view = { _ ->
                jsPluginView { v, prev ->
                    if (prev == null ||
                        prev.doc != v.state.doc ||
                        prev.selection != v.state.selection
                    ) {
                        selectComponent.value.set(levelToValue(activeHeadingLevel(v)))
                    }
                }
            }
        }
        return Plugin(spec)
    }

    override val nodeSpec: NodeSpec? = null

    context(scope: NodeScope)
    fun afterBuild() {

        template {
            selectComponent = select("heading") {

                observeValue { when(it) {
                    "p" -> setParagraph(view)
                    "h1" -> setHeading(view, 1)
                    "h2" -> setHeading(view, 2)
                    "h3" -> setHeading(view, 3)
                    "h4" -> setHeading(view, 4)
                    "h5" -> setHeading(view, 5)
                    "h6" -> setHeading(view, 6)
                } }

                option {
                    value("p")
                    text { "Paragraph" }
                }
                option {
                    value("h1")
                    text { "Heading 1" }
                }
                option {
                    value("h2")
                    text { "Heading 2" }
                }
                option {
                    value("h3")
                    text { "Heading 3" }
                }
                option {
                    value("h4")
                    text { "Heading 4" }
                }
                option {
                    value("h5")
                    text { "Heading 5" }
                }
                option {
                    value("h6")
                    text { "Heading 6" }
                }
            }
        }

    }

    companion object {
        val KEY = PluginKey<Unit>("heading-sync")
    }

}