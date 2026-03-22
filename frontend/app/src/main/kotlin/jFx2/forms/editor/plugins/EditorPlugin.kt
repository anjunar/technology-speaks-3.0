package jFx2.forms.editor.plugins

import jFx2.forms.editor.prosemirror.EditorView
import jFx2.forms.editor.prosemirror.NodeSpec
import jFx2.forms.editor.prosemirror.Plugin

interface EditorPlugin {

    val name : String

    var view : EditorView

    val nodeSpec : NodeSpec?

    fun plugin() : Plugin<*>

}