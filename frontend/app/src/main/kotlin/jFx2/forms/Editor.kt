@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")

package jFx2.forms

import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.renderComponent
import jFx2.core.dsl.renderFields
import jFx2.core.dsl.style
import jFx2.core.rendering.condition
import jFx2.core.template
import jFx2.forms.editor.EditorNode
import jFx2.forms.editor.plugins.EditorPlugin
import jFx2.forms.editor.prosemirror.DOMSerializer
import jFx2.forms.editor.prosemirror.EditorState
import jFx2.forms.editor.prosemirror.EditorStateConfig
import jFx2.forms.editor.prosemirror.Node
import jFx2.forms.editor.prosemirror.Plugin
import jFx2.forms.editor.prosemirror.Schema
import jFx2.forms.editor.prosemirror.addListNodes
import jFx2.forms.editor.prosemirror.baseKeymap
import jFx2.forms.editor.prosemirror.chainCommands
import jFx2.forms.editor.prosemirror.exitCode
import jFx2.forms.editor.prosemirror.history
import jFx2.forms.editor.prosemirror.keymap
import jFx2.forms.editor.prosemirror.liftListItem
import jFx2.forms.editor.prosemirror.newlineInCode
import jFx2.forms.editor.prosemirror.redo
import jFx2.forms.editor.prosemirror.sinkListItem
import jFx2.forms.editor.prosemirror.splitBlock
import jFx2.forms.editor.prosemirror.splitListItem
import jFx2.forms.editor.prosemirror.undo
import jFx2.jsObject
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.layout.hr
import jFx2.layout.vbox
import jFx2.forms.editor.prosemirror.schema as basicSchema
import jFx2.state.Disposable
import jFx2.state.Property
import org.w3c.dom.HTMLDivElement
import kotlin.js.json
import kotlin.js.unsafeCast
import jFx2.forms.editor.prosemirror.EditorView as ProseMirrorEditorView

@JfxComponentBuilder(classes = ["editor"])
class Editor(override val node: HTMLDivElement, override val name: String) : FormField<EditorNode?, HTMLDivElement>() {

    val valueProperty = Property<EditorNode?>(null)
    val editable = Property(true)

    private var editorSchema: Schema? = null
    private var editorPlugins: Array<Plugin<Any?>> = emptyArray()
    private var editorView: ProseMirrorEditorView? = null
    private var domSerializer: DOMSerializer? = null

    override var disabled: Boolean
        get() = ! editable.get()
        set(value) {
            editable.set(!value)
        }

    private fun parseDoc(schema: Schema, value: EditorNode?): Node? {
        return runCatching {
            schema.asDynamic().nodeFromJSON(value).unsafeCast<Node>()
        }.getOrNull()
    }

    private fun serializeDoc(doc: Node): EditorNode {
        val rawJson = doc.asDynamic().toJSON()
        return rawJson.unsafeCast<EditorNode>()
    }

    private fun ensureSchema(): Schema {
        val existing = editorSchema
        if (existing != null) return existing

        val specs: dynamic = {}
        this@Editor.children.filterIsInstance<EditorPlugin>().forEach {
            val p = it
            if (p.nodeSpec != null) {
                specs[p.name] = p.nodeSpec
            }
        }

        val customNodes =
            addListNodes(
                basicSchema.spec.nodes.append(specs),
                "paragraph block*",
                "block"
            )


        val customSchema = Schema(
            jsObject {
                nodes = customNodes
                marks = basicSchema.spec.marks
            }
        )

        editorSchema = customSchema
        domSerializer = DOMSerializer.fromSchema(customSchema)
        return customSchema
    }

    fun createState(initialValue: EditorNode? = valueProperty.get()): EditorState {

        val pluginInstances = this@Editor.children.filterIsInstance<EditorPlugin>().map { it.plugin() as Plugin<Any?> }

        val customSchema = ensureSchema()

        val itemType = customSchema.nodes["list_item"] ?: error("list_item missing in schema")

        val extraKeys = json(
            "Enter" to chainCommands(
                splitListItem(itemType),
                newlineInCode,
                splitBlock,
                exitCode
            ),

            "Tab" to sinkListItem(itemType),
            "Shift-Tab" to liftListItem(itemType),

            "Mod-z" to undo,
            "Mod-y" to redo
        )

        val defaultPlugins = arrayOf(
            history(),
            keymap(extraKeys),
            keymap(baseKeymap)
        )

        val allPlugins = defaultPlugins + pluginInstances

        editorSchema = customSchema
        editorPlugins = allPlugins

        val initialDoc = parseDoc(customSchema, initialValue)

        val cfg = jsObject<EditorStateConfig> {
            schema = customSchema
            this.plugins = allPlugins
            if (initialDoc != null) {
                doc = initialDoc
            }
        }

        return EditorState.create(cfg)
    }

    private fun clear(node: HTMLDivElement) {
        while (node.firstChild != null) {
            node.removeChild(node.firstChild!!)
        }
    }

    private fun renderInto(mount: HTMLDivElement, value: EditorNode?) {
        val schema = ensureSchema()
        val doc = parseDoc(schema, value)

        clear(mount)
        if (doc == null) return

        val serializer = domSerializer ?: DOMSerializer.fromSchema(schema).also { domSerializer = it }
        mount.appendChild(serializer.serializeFragment(doc.content))
    }

    context(scope : NodeScope)
    fun afterBuild() {
        val ui = scope.ui

        template {
            condition(editable) {
                then {

                    vbox {
                        style {
                            flex = "1"
                            minHeight = "0px"
                        }

                        hbox {

                            style {
                                alignItems = "center"
                                marginTop = "8px"
                            }

                            renderFields(*this@Editor.children.filter { it is EditorPlugin }.toTypedArray())

                            renderComponent(
                                div {
                                    style {
                                        flex = "1"
                                    }
                                }
                            )

                            renderFields(*this@Editor.children.filter { it !is EditorPlugin }.toTypedArray())
                        }

                        hr {
                            style {
                                marginTop = "8px"
                            }
                        }

                        div {

                            style {
                                flex = "1"
                                minHeight = "0px"
                            }

                            val initialValue = valueProperty.get()
                            val initialState = createState(initialValue)

                            var lastSeenValue = initialValue
                            lateinit var view: ProseMirrorEditorView
                            view = ProseMirrorEditorView(this@div.node, jsObject {
                                state = initialState
                                dispatchTransaction = { tr ->
                                    val newState = view.state.apply(tr)
                                    view.updateState(newState)

                                    if (tr.docChanged) {
                                        val next = serializeDoc(newState.doc)
                                        lastSeenValue = next
                                        valueProperty.set(next)
                                        ui.build.flush()
                                    }
                                }
                            })

                            editorView = view

                            this@Editor.children.filterIsInstance<EditorPlugin>().forEach {
                                it.view = view
                            }

                            onDispose { runCatching { view.destroy() } }

                            if (initialValue == null) {
                                val next = serializeDoc(view.state.doc)
                                lastSeenValue = next
                                valueProperty.set(next)
                            }

                            onDispose(
                                valueProperty.observe { v ->
                                    if (v == lastSeenValue) return@observe

                                    val schema = editorSchema ?: return@observe
                                    val plugins = editorPlugins

                                    val doc = parseDoc(schema, v)

                                    val nextState = EditorState.create(
                                        jsObject {
                                            this.schema = schema
                                            this.plugins = plugins
                                            if (doc != null) {
                                                this.doc = doc
                                            }
                                        }
                                    )

                                    view.updateState(nextState)
                                    lastSeenValue = v
                                }
                            )
                        }

                    }

                }
                elseDo {
                    div {
                        val mount = this@div.node
                        mount.classList.add("ProseMirror")
                        mount.setAttribute("contenteditable", "false")

                        val initialValue = valueProperty.get()
                        var lastSeenValue = initialValue

                        renderInto(mount, initialValue)
                        onDispose(
                            valueProperty.observe { v ->
                                if (v == lastSeenValue) return@observe
                                renderInto(mount, v)
                                lastSeenValue = v
                            }
                        )
                    }
                }
            }

        }


    }

    override fun read(): EditorNode? {
        return valueProperty.get()
    }

    override fun observeValue(listener: (EditorNode?) -> Unit): Disposable = valueProperty.observe(listener)

}

