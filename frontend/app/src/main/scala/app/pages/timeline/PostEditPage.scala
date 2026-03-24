package app.pages.timeline

import app.components.shared.ComponentHeader.componentHeader
import app.domain.core.Data
import app.domain.timeline.{Post, PostCreated, PostUpdated}
import app.services.ApplicationService
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.button
import jfx.core.component.ElementComponent.classes
import jfx.dsl.*
import jfx.dsl.Scope.{inject, scope}
import jfx.form.Editor.editor
import jfx.form.{ErrorResponseException, Form}
import jfx.form.Form.form
import jfx.form.editor.plugins.*
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.ExecutionContext

class PostEditPage(val data: Post) extends PageComposite("Posts") {

  override def pageWidth: Int = 360
  override def pageHeight: Int = 600

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("post-edit-page", "container"))

    withDslContext {
      val service = inject[ApplicationService]

      form(data) {
        Form.onSubmit = { (event: Form[Post]) =>
          val isExisting = data.id.get != null
          val request =
            if (isExisting) data.update()
            else data.save()

          request.foreach { saved =>
            if (isExisting) {
              service.messageBus.publish(new PostUpdated(saved))
            } else {
              service.messageBus.publish(new PostCreated(saved))
            }
            close()
          }

          request.failed.foreach {
            case e: ErrorResponseException =>
              Viewport.notify("Fehler beim Speichern", Viewport.NotificationKind.Error)
            case _ =>
              Viewport.notify("Ein unerwarteter Fehler ist aufgetreten", Viewport.NotificationKind.Error)
          }
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

          componentHeader(data) {}

          editor("editor") {
            style {
              flex = "1"
              minHeight = "0px"
            }

            basePlugin {}
            headingPlugin {}
            listPlugin {}
            linkPlugin {}
            imagePlugin {}
          }

          button("Senden") {
            classes = "btn-secondary"
            style {
              width = "100%"
            }
          }
        }
      }
    }
  }
}

object PostEditPage {
  def postEditPage(data: Post, init: PostEditPage ?=> Unit = {}): PostEditPage =
    CompositeSupport.buildPage(new PostEditPage(data))(init)
}
