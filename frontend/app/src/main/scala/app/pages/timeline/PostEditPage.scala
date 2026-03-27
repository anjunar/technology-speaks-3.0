package app.pages.timeline

import app.components.shared.ComponentHeader.componentHeader
import app.domain.core.Data
import app.domain.timeline.{Post, PostCreated, PostUpdated}
import app.services.ApplicationService
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.button
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.dsl.Scope.{inject, scope}
import jfx.form.Editor.editor
import jfx.form.{ErrorResponseException, Form}
import jfx.form.Form.form
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.ExecutionContext

class PostEditPage(val data: Post) extends PageComposite("Posts") {

  override def pageWidth: Int = 980
  override def pageHeight: Int = 860

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("post-edit-page"))

    withDslContext {
      val service = inject[ApplicationService]

      data.user.set(service.app.get.user)

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

        classes = "post-edit-page__form"

        vbox {
          classes = "post-edit-page__layout"

          vbox {
            classes = "post-edit-page__hero"

            style {
              height = "auto"
            }

            span {
              classes = "post-edit-page__eyebrow"
              text = "Resonanz"
            }

            span {
              classes = "post-edit-page__title"
              text = if (data.id.get != null) "Beitrag bearbeiten" else "Neuen Beitrag verfassen"
            }
          }

          div {
            classes = "post-edit-page__editor-shell"

            componentHeader(data) {}

            editor("editor") {
              classes = "post-edit-page__editor"
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

            hbox {
              classes = "post-edit-page__actions"

              button("Senden") {
                classes = "post-edit-page__submit"
                style {
                  width = "100%"
                }
              }
            }
          }
        }
      }
    }
  }
}

object PostEditPage {
  def postEditPage(data: Post, init: PostEditPage ?=> Unit = {})(using Scope): PostEditPage =
    CompositeSupport.buildPage(new PostEditPage(data))(init)
}
