package jFx2.forms.editor.plugins

import jFx2.controls.Button
import jFx2.controls.button
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.template
import jFx2.forms.editor.prosemirror.EditorState
import jFx2.forms.editor.prosemirror.EditorView
import jFx2.forms.editor.prosemirror.Node
import jFx2.forms.editor.prosemirror.NodeSpec
import jFx2.forms.editor.prosemirror.Plugin
import jFx2.forms.editor.prosemirror.PluginKey
import jFx2.forms.editor.prosemirror.PluginSpec
import jFx2.forms.editor.prosemirror.liftListItem
import jFx2.forms.editor.prosemirror.wrapInList
import jFx2.jsObject
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import kotlin.js.json

@JfxComponentBuilder(classes = ["list-plugin"])
class ListPlugin(override val node: HTMLDivElement) : Component<HTMLDivElement>(), EditorPlugin {

    override lateinit var view: EditorView

    private lateinit var bulletBtn: Button

    override val name: String = "bullet-list"

    private fun jsPluginView(onUpdate: (view: EditorView, prevState: EditorState?) -> Unit): dynamic {
        return json(
            "update" to { v: dynamic, prev: dynamic -> onUpdate(v, prev) }
        )
    }

    override fun plugin(): Plugin<*> {
        val spec = jsObject<PluginSpec<Unit>> {
            key = KEY
            view = { _ ->
                jsPluginView { v, prev ->
                    if (prev == null ||
                        prev.doc != v.state.doc ||
                        prev.selection != v.state.selection
                    ) {
                        updateUiFromState()
                    }
                }
            }
        }
        return Plugin(spec)
    }

    override val nodeSpec: NodeSpec? = null

    private fun isBulletListActive(): Boolean {
        val state = view.state
        val selection = state.selection
        val listType = state.schema.nodes["bullet_list"] ?: return false

        var found = false
        state.doc.nodesBetween(selection.from, selection.to, { node: Node, _, _, _ ->
            if (node.type == listType) {
                found = true
                false
            } else {
                true
            }
        })

        return found
    }

    private fun toggleBulletList() {
        val state = view.state
        val listType = state.schema.nodes["bullet_list"] ?: return
        val itemType = state.schema.nodes["list_item"] ?: return

        val command = if (isBulletListActive()) {
            liftListItem(itemType)
        } else {
            wrapInList(listType)
        }

        command(state, { tr -> view.dispatch(tr) }, view)
        view.focus()
        updateUiFromState()
    }

    private fun updateUiFromState() {
        setActive(bulletBtn.node, isBulletListActive())
    }

    private fun setActive(btn: HTMLButtonElement, active: Boolean) {
        if (active) btn.classList.add("active") else btn.classList.remove("active")
    }

    context(scope: NodeScope)
    fun afterBuild() {
        template {
            bulletBtn = button("format_list_bulleted") {
                className { "material-icons" }
                type("button")
                onClick { toggleBulletList() }
            }
        }
    }

    companion object {
        val KEY = PluginKey<Unit>("bullet-list-plugin")
    }
}