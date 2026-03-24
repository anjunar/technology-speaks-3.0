package app.components.commentable

import app.components.likeable.LikeButton.likeButton
import app.components.commentable.CommentsSection.commentsSection
import app.components.shared.ComponentHeader.{componentHeader, onDelete, onUpdate}
import app.domain.core.AbstractEntity
import app.domain.documents.Document
import app.domain.shared.FirstComment
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.button
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property
import jfx.core.state.Property.subscribeBidirectional
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.editor.plugins.*

import scala.concurrent.ExecutionContext

class FirstCommentCard(
  comment: FirstComment,
  owner: AbstractEntity[?],
  onPersist: FirstComment => Unit,
  onDeleteCompleted: FirstComment => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classes = "glass-border"

    withDslContext {
      val service = injectFromDsl[ApplicationService]

      form(comment) {
        onSubmit_= { (event : Form[FirstComment])  =>
          comment.user.set(service.app.get.user)

          val request =
            if (comment.id.get != null) comment.update(owner)
            else comment.save(owner)

          request.foreach { saved =>
            comment.editable.set(false)
            onPersist(saved.data)
          }
        }

        componentHeader(comment) {
          onDelete { () =>
            val request =
              if (comment.id.get != null) comment.delete(owner)
              else scala.concurrent.Future.successful(())

            request.foreach(_ => onDeleteCompleted(comment))
          }
          onUpdate { () =>
            comment.editable.set(!comment.editable.get)
          }
        }

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

        subscribeBidirectional(comment.editor, editorField.valueProperty)
        subscribeBidirectional(comment.editable, editorField.editableProperty)

        likeButton(comment.likes, comment.links) {}

        commentsSection(comment, owner) {}
      }
    }
  }
}


object FirstCommentCard {
  def firstCommentCard(
    comment: FirstComment,
    owner: AbstractEntity[?],
    onPersist: FirstComment => Unit = _ => (),
    onDeleteCompleted: FirstComment => Unit = _ => ()
  ): FirstCommentCard =
    CompositeSupport.buildComposite(new FirstCommentCard(comment, owner, onPersist, onDeleteCompleted))
}
