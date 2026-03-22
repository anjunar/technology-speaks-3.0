package jFx2.forms.editor.plugins

import jFx2.controls.Button
import jFx2.controls.button
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.template
import jFx2.forms.editor.prosemirror.EditorState
import jFx2.forms.editor.prosemirror.EditorView
import jFx2.forms.editor.prosemirror.Mark
import jFx2.forms.editor.prosemirror.Node
import jFx2.forms.editor.prosemirror.NodeSpec
import jFx2.forms.editor.prosemirror.Plugin
import jFx2.forms.editor.prosemirror.PluginKey
import jFx2.forms.editor.prosemirror.PluginSpec
import jFx2.forms.editor.prosemirror.Selection
import jFx2.forms.editor.prosemirror.toggleMark
import jFx2.forms.form
import jFx2.forms.input
import jFx2.jsObject
import jFx2.layout.hbox
import jFx2.modals.Viewport
import jFx2.state.Property
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.js.json

class LinkDescriptor(val href : Property<String> = Property(""), val title : Property<String> = Property(""))

@JfxComponentBuilder(classes = ["link-plugin"])
class LinkPlugin(override val node: HTMLDivElement) : Component<HTMLDivElement>(), EditorPlugin {

    override lateinit var view: EditorView

    private lateinit var linkBtn: Button

    override val name: String = "link"

    var onOpenLink: ((attrs: dynamic) -> Unit)? = { attrs ->

        val formular = LinkDescriptor()

        Viewport.addWindow(
            Viewport.Companion.WindowConf(
                "Add Link",
                {

                    form(model = formular, clazz = LinkDescriptor::class) {
                        onSubmit {
                            val href = this@form.model.href.get().trim().orEmpty()
                            val title = this@form.model.href.get().trim().orEmpty()
                            if (href.isBlank()) {
                                removeLink()
                            } else {
                                insertLink(href, if (title.isBlank()) null else title)
                            }
                        }

                        input("href") {
                            placeholder = "https://example.com"
                            if (attrs["href"] != null) {
                                valueProperty.set(attrs["href"] as String)
                            }

                            subscribeBidirectional(this@form.model.href, valueProperty)
                        }

                        input("title") {
                            placeholder = "Title (optional)"
                            if (attrs["title"] != null) {
                                valueProperty.set(attrs["title"] as String)
                            }

                            subscribeBidirectional(this@form.model.title, valueProperty)
                        }

                        hbox {
                            button("Save") {
                                style { marginLeft = "10px" }
                            }

                            button("Remove") {
                                style { marginLeft = "10px" }
                                type("button")
                                onClick { removeLink() }
                            }
                        }
                    }
                }
            )
        )
    }

    private fun jsPluginView(onUpdate: (view: EditorView, prevState: EditorState?) -> Unit): dynamic {
        return json(
            "update" to { v: dynamic, prev: dynamic -> onUpdate(v, prev) }
        )
    }

    override fun plugin(): Plugin<*> {
        val spec = jsObject<PluginSpec<Unit>> {
            key = KEY
            props = jsObject {
                handleDOMEvents = jsObject {
                    dblclick = { _: dynamic, e: dynamic ->
                        val target = e.target as? HTMLElement
                        if (target != null && target.nodeName == "A") {
                            openFromSelection()
                            true
                        } else {
                            false
                        }
                    }
                }
            }
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

    private fun insertLink(href: String, title: String?) {
        val state = view.state
        val type = state.schema.marks["link"] ?: return
        val attrs = json(
            "href" to href,
            "title" to title
        )
        toggleMark(type, attrs)(state, { tr -> view.dispatch(tr) }, view)
        view.focus()
        updateUiFromState()
    }

    private fun removeLink() {
        val state = view.state
        val type = state.schema.marks["link"] ?: return
        if (!isMarkActive("link", state.selection)) return
        toggleMark(type)(state, { tr -> view.dispatch(tr) }, view)
        view.focus()
        updateUiFromState()
    }

    private fun isMarkActive(markTypeName: String, selection: Selection): Boolean {
        val state = view.state
        val from = selection.from
        val to = selection.to
        val empty = selection.empty

        val type = state.schema.marks[markTypeName] ?: return false

        return if (empty) {
            val stored = state.storedMarks
            if (stored != null) {
                type.isInSet(stored) != null
            } else {
                type.isInSet(state.selection._from.marks()) != null
            }
        } else {
            var found = false
            state.doc.nodesBetween(from, to, { node: Node, _, _, _ ->
                if (type.isInSet(node.marks) != null) {
                    found = true
                    false
                } else {
                    true
                }
            })
            found
        }
    }

    private fun activeLinkAttrs(): dynamic {
        val state = view.state
        val selection = state.selection
        val type = state.schema.marks["link"] ?: return json()

        val mark: Mark? = if (selection.empty) {
            val stored = state.storedMarks
            if (stored != null) {
                type.isInSet(stored)
            } else {
                type.isInSet(state.selection._from.marks())
            }
        } else {
            var found: Mark? = null
            state.doc.nodesBetween(selection.from, selection.to, { node: Node, _, _, _ ->
                val current = type.isInSet(node.marks)
                if (current != null) {
                    found = current
                    false
                } else {
                    true
                }
            })
            found
        }

        return mark?.attrs ?: json()
    }

    private fun updateUiFromState() {
        val active = isMarkActive("link", view.state.selection)
        setActive(linkBtn.node, active)
    }

    private fun setActive(btn: HTMLButtonElement, active: Boolean) {
        if (active) btn.classList.add("active") else btn.classList.remove("active")
    }

    private fun openFromSelection() {
        onOpenLink?.invoke(activeLinkAttrs())
    }

    context(scope: NodeScope)
    fun afterBuild() {
        template {
            button("link") {
                className { "material-icons" }
                type("button")
                onClick { openFromSelection() }
            }.also { linkBtn = it }
        }
    }

    companion object {
        val KEY = PluginKey<Unit>("link-plugin")
    }
}