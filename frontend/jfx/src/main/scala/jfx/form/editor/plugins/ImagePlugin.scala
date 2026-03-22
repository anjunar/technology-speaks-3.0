package jfx.form.editor.plugins

import jfx.action.Button
import jfx.control.Image
import jfx.dsl.*
import jfx.form.{Form, Input}
import jfx.form.editor.prosemirror.*
import jfx.layout.{Div, HBox, Viewport}
import org.scalajs.dom.{Element, Event, FileReader, HTMLInputElement, HTMLElement, document, window}

import scala.scalajs.js

class ImagePlugin extends AbstractEditorPlugin("image-plugin") {

  override val name: String = "image"

  private var structureInitialized = false

  var onOpenImage: js.Function1[js.Dynamic, Unit] | Null =
    ((attrs: js.Dynamic) => {
      given Scope = currentPluginScope

      Viewport.addWindow(
        new Viewport.WindowConf(
          "Add Image",
          Viewport.captureComponent {
            Form.form(new Dimensions()) {
              val formComponent = summon[Form[Dimensions]]
              var previewImage: Image | Null = null

              formComponent.onSubmit = { _ =>
                if (previewImage != null) {
                  insertImage(
                    previewImage.nn.src,
                    formComponent.valueProperty.get.width.get.toInt,
                    formComponent.valueProperty.get.height.get.toInt
                  )
                }
              }

              Div.div {
                style {
                  display = "flex"
                  justifyContent = "center"
                  width = "320px"
                  height = "240px"
                }

                previewImage = Image.image {
                  val current = summon[Image]
                  attrString(attrs, "src").foreach(current.src = _)

                  style {
                    maxWidth = "320px"
                    maxHeight = "240px"
                  }

                  window.setTimeout(
                    () => syncDimensionsFromPreview(formComponent.valueProperty.get, current),
                    100
                  )
                }
              }

              Div.div {
                val fileInput = document.createElement("input").asInstanceOf[HTMLInputElement]
                fileInput.`type` = "file"
                summon[Div].element.appendChild(fileInput)

                fileInput.onchange = (_: Event) => {
                  if (previewImage != null) {
                    val reader = new FileReader()

                    reader.onload = (_: Event) => {
                      previewImage.nn.src = reader.result.asInstanceOf[String]
                      window.setTimeout(
                        () => syncDimensionsFromPreview(formComponent.valueProperty.get, previewImage.nn),
                        100
                      )
                    }

                    scala.Option(fileInput.files)
                      .flatMap(files => scala.Option(files.item(0)))
                      .foreach(reader.readAsDataURL)
                  }
                }
              }

              HBox.hbox {
                Input.input("width") {
                  val current = summon[Input]
                  current.element.`type` = "number"
                  current.placeholder = "Width"
                }

                Input.input("height") {
                  val current = summon[Input]
                  current.element.`type` = "number"
                  current.placeholder = "Height"
                }
              }

              Button.button("Submit") {
                summon[Button].element.style.marginLeft = "10px"
              }
            }
          }
        )
      )
    }): js.Function1[js.Dynamic, Unit]

  override val nodeSpec: NodeSpec = {
    val attrs = js.Dynamic.literal(
      src = js.Dynamic.literal(default = null),
      alt = js.Dynamic.literal(default = null),
      title = js.Dynamic.literal(default = null),
      style = js.Dynamic.literal(default = null)
    )

    val parseRule = (new js.Object).asInstanceOf[ParseRule]
    parseRule.tag = "img[src]"
    parseRule.getAttrs = { (dom: org.scalajs.dom.Node) =>
      val element = dom.asInstanceOf[Element]
      js.Dynamic.literal(
        "src" -> element.getAttribute("src"),
        "alt" -> element.getAttribute("alt"),
        "title" -> element.getAttribute("title"),
        "style" -> element.getAttribute("style")
      )
    }

    val spec = (new js.Object).asInstanceOf[NodeSpec]
    spec.inline = true
    spec.group = "inline"
    spec.draggable = true
    spec.attrs = attrs
    spec.parseDOM = js.Array(parseRule)
    spec.toDOM = { (node: js.Dynamic) =>
      js.Array[js.Any](
        "img",
        js.Dynamic.literal(
          "src" -> node.attrs.selectDynamic("src"),
          "alt" -> node.attrs.selectDynamic("alt"),
          "title" -> node.attrs.selectDynamic("title"),
          "style" -> node.attrs.selectDynamic("style")
        )
      )
    }
    spec
  }

  override protected def mountContent(): Unit =
    if (!structureInitialized) {
      structureInitialized = true

      withPluginContext {
        Button.button("image") {
          val current = summon[Button]
          current.buttonType = "button"
          current.classProperty += "material-icons"
          current.addClick { _ =>
            openFromSelection()
          }
        }
      }
    }

  override def plugin(): Plugin[js.Any] = {
    val spec = (new js.Object).asInstanceOf[PluginSpec[js.Any]]
    spec.key = new PluginKey[js.Any]("image-plugin")
    spec.props =
      js.Dynamic.literal(
        handleDOMEvents = js.Dynamic.literal(
          dblclick = { (_: js.Any, event: js.Dynamic) =>
            val target = event.target.asInstanceOf[js.Any]
            if (
              target != null &&
              !js.isUndefined(target) &&
              target.isInstanceOf[HTMLElement] &&
              target.asInstanceOf[HTMLElement].nodeName == "IMG"
            ) {
              openFromSelection()
              true
            } else {
              false
            }
          }
        )
      )

    new Plugin[js.Any](spec)
  }

  private def insertImage(source: String, width: Int, height: Int): Unit =
    selectNodeType(view.state.schema.nodes, "image").foreach { imageType =>
      val attrs = js.Dynamic.literal(
        "src" -> source,
        "style" -> s"width:${width}px;height:${height}px;"
      )

      val transaction =
        view.state.tr
          .replaceSelectionWith(imageType.create(attrs), false)
          .scrollIntoView()

      view.dispatch(transaction)
      view.focus()
    }

  private def openFromSelection(): Unit = {
    val imageType = selectNodeType(view.state.schema.nodes, "image").orNull

    if (onOpenImage != null && imageType != null) {
      val attrs =
        view.state.selection match {
          case nodeSelection: NodeSelection if nodeSelection.node.nodeType == imageType =>
            nodeSelection.node.attrs
          case _ =>
            js.Dynamic.literal()
        }

      onOpenImage.nn.apply(attrs)
    }
  }

  private def syncDimensionsFromPreview(dimensions: Dimensions, image: Image): Unit = {
    dimensions.width.set(image.element.width.toDouble)
    dimensions.height.set(image.element.height.toDouble)
  }

  private def attrString(attrs: js.Dynamic, key: String): scala.Option[String] = {
    val value = attrs.selectDynamic(key).asInstanceOf[js.Any]
    if (value == null || js.isUndefined(value)) scala.None
    else scala.Some(value.toString)
  }

  private def selectNodeType(source: js.Dynamic, key: String): scala.Option[NodeType] = {
    val value = source.selectDynamic(key).asInstanceOf[js.Any]
    if (value == null || js.isUndefined(value)) scala.None
    else scala.Some(value.asInstanceOf[NodeType])
  }
}

object ImagePlugin {

  def imagePlugin(init: ImagePlugin ?=> Unit = {}): ImagePlugin =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new ImagePlugin()
      component.captureScope(currentScope)

      DslRuntime.withComponentContext(ComponentContext(Some(component), currentContext.enclosingForm)) {
        given Scope = currentScope
        given ImagePlugin = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}
