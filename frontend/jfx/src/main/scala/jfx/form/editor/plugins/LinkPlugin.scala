package jfx.form.editor.plugins

import jfx.action.Button
import jfx.dsl.*
import jfx.form.{Form, Input}
import jfx.form.editor.prosemirror.*
import jfx.layout.{HBox, Viewport}
import org.scalajs.dom.HTMLElement

import scala.scalajs.js

class LinkPlugin extends AbstractEditorPlugin("link-plugin") {

  override val name: String = "link"

  override val nodeSpec: NodeSpec | Null = null

  private var structureInitialized = false
  private var linkButton: Button | Null = null

  var onOpenLink: js.Function1[js.Dynamic, Unit] | Null =
    ((attrs: js.Dynamic) => {
      given Scope = currentPluginScope

      Viewport.addWindow(
        new Viewport.WindowConf(
          "Add Link",
          Viewport.captureComponent {
            Form.form(new LinkDescriptor()) {
              val formComponent = summon[Form[LinkDescriptor]]

              formComponent.onSubmit = { _ =>
                val href = formComponent.valueProperty.get.href.get.trim
                val title = formComponent.valueProperty.get.title.get.trim

                if (href.isBlank) {
                  removeLink()
                } else {
                  insertLink(href, if (title.isBlank) null else title)
                }
              }

              Input.input("href") {
                val current = summon[Input]
                current.placeholder = "https://example.com"
                attrString(attrs, "href").foreach(current.valueProperty.set)
              }

              Input.input("title") {
                val current = summon[Input]
                current.placeholder = "Title (optional)"
                attrString(attrs, "title").foreach(current.valueProperty.set)
              }

              HBox.hbox {
                Button.button("Save") {
                  summon[Button].element.style.marginLeft = "10px"
                }

                Button.button("Remove") {
                  val current = summon[Button]
                  current.buttonType = "button"
                  current.element.style.marginLeft = "10px"
                  current.addClick { _ =>
                    removeLink()
                  }
                }
              }
            }
          }
        )
      )
    }): js.Function1[js.Dynamic, Unit]

  override protected def mountContent(): Unit =
    if (!structureInitialized) {
      structureInitialized = true

      withPluginContext {
        Button.button("link") {
          val current = summon[Button]
          current.buttonType = "button"
          current.classProperty += "material-icons"
          current.addClick { _ =>
            openFromSelection()
          }
          linkButton = current
        }
      }

      updateUiFromState()
    }

  override def plugin(): Plugin[js.Any] = {
    val spec = (new js.Object).asInstanceOf[PluginSpec[js.Any]]
    spec.key = new PluginKey[js.Any]("link-plugin")
    spec.props =
      js.Dynamic.literal(
        handleDOMEvents = js.Dynamic.literal(
          dblclick = { (_: js.Any, event: js.Dynamic) =>
            val target = event.target.asInstanceOf[js.Any]
            if (
              target != null &&
              !js.isUndefined(target) &&
              target.isInstanceOf[HTMLElement] &&
              target.asInstanceOf[HTMLElement].nodeName == "A"
            ) {
              openFromSelection()
              true
            } else {
              false
            }
          }
        )
      )

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

  private def insertLink(href: String, title: String | Null): Unit =
    selectMarkType(view.state.schema.marks, "link").foreach { linkType =>
      Commands.toggleMark(
        linkType,
        js.Dynamic.literal(
          "href" -> href,
          "title" -> title
        )
      )(
        view.state,
        dispatch(view),
        view
      )
      view.focus()
      updateUiFromState()
    }

  private def removeLink(): Unit =
    selectMarkType(view.state.schema.marks, "link").foreach { linkType =>
      if (isMarkActive("link", view.state.selection)) {
        Commands.toggleMark(linkType)(
          view.state,
          dispatch(view),
          view
        )
        view.focus()
        updateUiFromState()
      }
    }

  private def isMarkActive(markTypeName: String, selection: Selection): Boolean = {
    val state = view.state
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

  private def activeLinkAttrs(): js.Dynamic = {
    val state = view.state
    val selection = state.selection
    val linkType = selectMarkType(state.schema.marks, "link").orNull

    if (linkType == null) {
      js.Dynamic.literal()
    } else {
      val mark =
        if (selection.empty) {
          val storedMarks = state.storedMarks
          if (storedMarks != null) {
            linkType.isInSet(storedMarks)
          } else {
            linkType.isInSet(selection.fromResolved.marks())
          }
        } else {
          var found: Mark | Null = null

          state.doc.nodesBetween(
            selection.from,
            selection.to,
            { (node, _, _, _) =>
              val current = linkType.isInSet(node.marks)
              if (current != null) {
                found = current
                false
              } else {
                true
              }
            }
          )

          found
        }

      if (mark == null) {
        js.Dynamic.literal()
      } else {
        mark.attrs
      }
    }
  }

  private def updateUiFromState(): Unit =
    if (viewIsReady && linkButton != null) {
      if (isMarkActive("link", view.state.selection)) linkButton.nn.element.classList.add("active")
      else linkButton.nn.element.classList.remove("active")
    }

  private def openFromSelection(): Unit =
    if (onOpenLink != null && viewIsReady) {
      onOpenLink.nn.apply(activeLinkAttrs())
    }

  private def attrString(attrs: js.Dynamic, key: String): scala.Option[String] = {
    val value = attrs.selectDynamic(key).asInstanceOf[js.Any]
    if (value == null || js.isUndefined(value)) scala.None
    else scala.Some(value.toString)
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

object LinkPlugin {

  def linkPlugin(init: LinkPlugin ?=> Unit = {}): LinkPlugin =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new LinkPlugin()
      component.captureScope(currentScope)

      DslRuntime.withComponentContext(ComponentContext(Some(component), currentContext.enclosingForm)) {
        given Scope = currentScope
        given LinkPlugin = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
