package app.components.commentable

import app.components.commentable.CommentsSection.CommentReplyCard
import app.components.likeable.LikeButton.likeButton
import app.components.shared.ComponentHeader.{componentHeader, onDelete, onUpdate}
import app.domain.core.AbstractEntity
import app.domain.shared.{FirstComment, SecondComment}
import app.services.ApplicationService
import app.support.Navigation
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.{button, buttonType, buttonType_=, onClick}
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.dsl.*
import jfx.form.Control.valueProperty
import jfx.form.Editable.editableProperty
import jfx.form.Editor.editor
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.editor.plugins.*
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.statement.ForEach.forEach

import scala.concurrent.ExecutionContext

class CommentsSection(val firstComment: FirstComment, val owner: AbstractEntity[?]) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classes = "comments-section"

    withDslContext {
      vbox {
        style {
          rowGap = "10px"
        }

        hbox {
          style {
            justifyContent = "flex-end"
          }

          button("Kommentieren") {
            style {
              display = if (Navigation.linkByRel("updateChildren", firstComment.links).nonEmpty) "inline-flex" else "none"
            }
            buttonType = "button"
            onClick { _ =>
              if (!firstComment.comments.lastOption.exists(_.editable.get)) {
                val reply = new SecondComment()
                reply.editable.set(true)
                reply.user.set(ApplicationService.app.get.user)
                firstComment.comments += reply
              }
            }
          }

        }

        vbox {
          style {
            rowGap = "10px"
          }

          forEach(firstComment.comments) { comment =>
            CommentReplyCard.card(firstComment, comment, owner)
          }
        }
      }
    }
  }
}

object CommentsSection {
  def commentsSection(firstComment: FirstComment, owner: AbstractEntity[?])(init: CommentsSection ?=> Unit = {}): CommentsSection =
    CompositeSupport.buildComposite(new CommentsSection(firstComment, owner))(init)

  final class CommentReplyCard(firstComment: FirstComment,
                               comment: SecondComment,
                               owner: AbstractEntity[?]
                              ) extends DivComposite {

    private given ExecutionContext = ExecutionContext.global

    override protected def compose(using DslContext): Unit = {
      classes = "glass-border"

      withDslContext {
        form(comment) {
          onSubmit_= { (event: Form[SecondComment]) =>
            comment.user.set(ApplicationService.app.get.user)
            firstComment
              .update(owner)
              .foreach(_ => comment.editable.set(false))
          }

          componentHeader(comment) {
            onDelete { () =>
              firstComment.comments -= comment
              firstComment.update(owner)
            }
            onUpdate { () =>
              comment.editable.set(!comment.editable.get)
            }
          }

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

            button("save") {
              classes = Seq("material-icons", "hover")
            }

            subscribeBidirectional(comment.editor, valueProperty)
            subscribeBidirectional(comment.editable, editableProperty)
          }


          likeButton(comment.likes, comment.links) {}
        }
      }
    }
  }

  object CommentReplyCard {
    def card(firstComment: FirstComment,
             comment: SecondComment,
             owner: AbstractEntity[?]): CommentReplyCard =
      CompositeSupport.buildComposite(new CommentReplyCard(firstComment, comment, owner))
  }

}