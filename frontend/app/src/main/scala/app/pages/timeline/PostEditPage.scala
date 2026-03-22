package app.pages.timeline

import app.components.shared.ComponentHeader.componentHeader
import app.domain.core.Data
import app.domain.timeline.{Post, PostCreated, PostUpdated}
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.button
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.Property
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.editor.plugins.*
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import org.scalajs.dom.Node

import scala.concurrent.ExecutionContext

class PostEditPage extends PageComposite("Posts") {

  private val modelProperty: Property[Data[Post]] =
    Property(new Data[Post](new Post(user = Property(ApplicationService.app.get.user))))
  private val contentProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)

  def model(value: Data[Post]): Unit =
    modelProperty.set(value)

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("post-edit-page", "container"))

    addDisposable(modelProperty.observe(value => contentProperty.set(PostEditContent.content(value, handleSaved))))

    withDslContext {
      dynamicOutlet(contentProperty)
    }
  }

  private def handleSaved(saved: Data[Post]): Unit = {
    if (Option(saved.data.id.get).exists(_.trim.nonEmpty) && Option(modelProperty.get.data.id.get).exists(_.trim.nonEmpty)) {
      ApplicationService.messageBus.publish(new PostUpdated(saved))
    } else {
      ApplicationService.messageBus.publish(new PostCreated(saved))
    }

    close()
  }
}

object PostEditPage {
  def postEditPage(init: PostEditPage ?=> Unit = {}): PostEditPage =
    CompositeSupport.buildPage(new PostEditPage)(init)
}

private final class PostEditContent(
  data: Data[Post],
  onSaved: Data[Post] => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      form(data.data) {
        onSubmit_= { _ =>
          val isExisting = Option(data.data.id.get).exists(_.trim.nonEmpty)
          val request =
            if (isExisting) data.data.update()
            else data.data.save()

          request.foreach(onSaved)
        }

        style {
          padding = "10px"
          height = "calc(100% - 20px)"
        }

        vbox {
          style {
            rowGap = "10px"
            height = "100%"
          }

          val header = componentHeader {}
          header.model(data.data)

          val editorField = editor("editor") {
            style {
              flex = "1"
              minHeight = "0px"
            }

            basePlugin {}
            headingPlugin {}
            listPlugin {}
            linkPlugin {}
            imagePlugin {}

            button("save") {
              classes = Seq("material-icons", "hover")
            }
          }

          addDisposable(Property.subscribeBidirectional(data.data.editor, editorField.valueProperty))

          button("Senden") {
            classes = Seq("btn-secondary")
            style {
              width = "100%"
            }
          }
        }
      }
    }
  }
}

private object PostEditContent {
  def content(data: Data[Post], onSaved: Data[Post] => Unit): PostEditContent =
    CompositeSupport.buildComposite(new PostEditContent(data, onSaved))
}
