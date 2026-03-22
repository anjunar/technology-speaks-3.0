package jfx.form.editor.plugins

import jfx.action.Button
import jfx.dsl.*
import jfx.form.editor.prosemirror.*

import scala.scalajs.js

class BasePlugin extends AbstractEditorPlugin("base-plugin") {

  override val name: String = "base"

  override val nodeSpec: NodeSpec | Null = null

  private var structureInitialized = false
  private var boldButton: Button | Null = null
  private var italicButton: Button | Null = null
  private var undoButton: Button | Null = null
  private var redoButton: Button | Null = null

  override protected def mountContent(): Unit =
    if (!structureInitialized) {
      structureInitialized = true

      withPluginContext {
        jfx.layout.HBox.hbox {
          Button.button("format_bold") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "material-icons"
            current.addClick { _ =>
              toggleMarkCommand("strong")
            }
            boldButton = current
          }

          Button.button("format_italic") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "material-icons"
            current.addClick { _ =>
              toggleMarkCommand("em")
            }
            italicButton = current
          }

          Button.button("undo") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "material-icons"
            current.addClick { _ =>
              undoCommand()
            }
            undoButton = current
          }

          Button.button("redo") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "material-icons"
            current.addClick { _ =>
              redoCommand()
            }
            redoButton = current
          }
        }
      }

      updateUiFromState()
    }

  override def plugin(): Plugin[js.Any] = {
    val spec = (new js.Object).asInstanceOf[PluginSpec[js.Any]]
    spec.key = new PluginKey[js.Any]("base-sync")
    spec.view = syncPluginView { (currentView, previousState) =>
      if (
        previousState == null ||
        previousState.doc != currentView.state.doc ||
        previousState.selection != currentView.state.selection
      ) {
        updateUiFromState()
      }
    }

    new Plugin[js.Any](spec)
  }

  override protected def onViewChanged(nextView: EditorView | Null): Unit =
    updateUiFromState()

  private def toggleMarkCommand(markName: String): Unit = {
    val markType = selectMarkType(view.state.schema.marks, markName)
    markType.foreach { resolvedMarkType =>
      Commands.toggleMark(resolvedMarkType)(
        view.state,
        dispatch(view),
        view
      )
      view.focus()
      updateUiFromState()
    }
  }

  private def isMarkActive(markTypeName: String): Boolean = {
    val state = view.state
    val selection = state.selection
    val markType = selectMarkType(state.schema.marks, markTypeName)

    markType.exists { resolvedMarkType =>
      if (selection.empty) {
        val storedMarks = state.storedMarks
        if (storedMarks != null) {
          resolvedMarkType.isInSet(storedMarks) != null
        } else {
          resolvedMarkType.isInSet(selection.fromResolved.marks()) != null
        }
      } else {
        var found = false

        state.doc.nodesBetween(
          selection.from,
          selection.to,
          { (node, _, _, _) =>
            if (resolvedMarkType.isInSet(node.marks) != null) {
              found = true
              false
            } else {
              true
            }
          }
        )

        found
      }
    }
  }

  private def undoCommand(): Unit = {
    History.undo(view.state, dispatch(view), view)
    view.focus()
    updateUiFromState()
  }

  private def redoCommand(): Unit = {
    History.redo(view.state, dispatch(view), view)
    view.focus()
    updateUiFromState()
  }

  private def updateUiFromState(): Unit =
    if (viewIsReady && boldButton != null && italicButton != null) {
      setActive(boldButton, isMarkActive("strong") || isMarkActive("bold"))
      setActive(italicButton, isMarkActive("em") || isMarkActive("italic"))

      if (undoButton != null) {
        undoButton.nn.element.disabled = false
      }

      if (redoButton != null) {
        redoButton.nn.element.disabled = false
      }
    }

  private def setActive(button: Button | Null, active: Boolean): Unit =
    if (button != null) {
      if (active) button.nn.element.classList.add("active")
      else button.nn.element.classList.remove("active")
    }

  private def selectMarkType(source: js.Dynamic, key: String): scala.Option[MarkType] = {
    val value = source.selectDynamic(key).asInstanceOf[js.Any]
    if (value == null || js.isUndefined(value)) scala.None
    else scala.Some(value.asInstanceOf[MarkType])
  }

  private def dispatch(editorView: EditorView): DispatchFn =
    (transaction: Transaction) => editorView.dispatch(transaction)

  private def viewIsReady: Boolean =
    try {
      view
      true
    } catch {
      case _: IllegalStateException => false
    }
}

object BasePlugin {

  def basePlugin(init: BasePlugin ?=> Unit = {}): BasePlugin =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new BasePlugin()
      component.captureScope(currentScope)

      DslRuntime.withComponentContext(ComponentContext(Some(component), currentContext.enclosingForm)) {
        given Scope = currentScope
        given BasePlugin = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
