package app.components.commentable

import app.components.likeable.LikeButton.likeButton
import app.components.shared.ComponentHeader.componentHeader
import app.domain.core.AbstractEntity
import app.domain.shared.{FirstComment, SecondComment}
import app.services.ApplicationService
import app.support.Navigation
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.Property
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.editor.plugins.*
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import jfx.statement.ForEach.forEach
import org.scalajs.dom.Node

import scala.concurrent.ExecutionContext

class CommentsSection extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val firstCommentProperty: Property[FirstComment] = Property(new FirstComment())
  private val ownerProperty: Property[AbstractEntity[?] | Null] = Property(null)
  private val contentProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)

  def model(value: FirstComment, owner: AbstractEntity[?]): Unit = {
    firstCommentProperty.set(if (value == null) new FirstComment() else value)
    ownerProperty.set(owner)
  }

  override protected def compose(using DslContext): Unit = {
    classProperty += "comments-section"

    addDisposable(firstCommentProperty.observe(_ => refreshContent()))
    addDisposable(ownerProperty.observe(_ => refreshContent()))

    withDslContext {
      dynamicOutlet(contentProperty)
    }
  }

  private def refreshContent(): Unit = {
    ownerProperty.get match {
      case null =>
        contentProperty.set(null)
      case owner =>
        contentProperty.set(CommentsSectionContent.content(firstCommentProperty.get, owner))
    }
  }
}

object CommentsSection {
  def commentsSection(init: CommentsSection ?=> Unit = {}): CommentsSection =
    CompositeSupport.buildComposite(new CommentsSection)(init)
}

private final class CommentsSectionContent(
  firstComment: FirstComment,
  owner: AbstractEntity[?]
) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      vbox {
        style {
          rowGap = "10px"
        }

        hbox {
          style {
            justifyContent = "flex-end"
          }

          val addButton = button("Kommentieren") {
            buttonType_=("button")
            onClick { _ =>
              if (!firstComment.comments.lastOption.exists(_.editable.get)) {
                val reply = new SecondComment()
                reply.editable.set(true)
                reply.user.set(ApplicationService.app.get.user)
                firstComment.comments += reply
              }
            }
          }

          addButton.element.style.display =
            if (Navigation.linkByRel("updateChildren", firstComment.links).nonEmpty) "inline-flex" else "none"
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

private object CommentsSectionContent {
  def content(firstComment: FirstComment, owner: AbstractEntity[?]): CommentsSectionContent =
    CompositeSupport.buildComposite(new CommentsSectionContent(firstComment, owner))
}

private final class CommentReplyCard(
  firstComment: FirstComment,
  comment: SecondComment,
  owner: AbstractEntity[?]
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      form(comment) {
        onSubmit_= { _ =>
          comment.user.set(ApplicationService.app.get.user)
          firstComment
            .update(owner)
            .foreach(_ => comment.editable.set(false))
        }

        val header = componentHeader {}
        header.model(comment)
        header.onDelete { () =>
          firstComment.comments -= comment
          firstComment.update(owner)
          ()
        }
        header.onUpdate { () =>
          comment.editable.set(!comment.editable.get)
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

        addDisposable(jfx.core.state.Property.subscribeBidirectional(comment.editor, editorField.valueProperty))
        addDisposable(jfx.core.state.Property.subscribeBidirectional(comment.editable, editorField.editableProperty))

        val likes = likeButton {}
        likes.model(comment.likes, comment.links)
      }
    }
  }
}

private object CommentReplyCard {
  def card(
    firstComment: FirstComment,
    comment: SecondComment,
    owner: AbstractEntity[?]
  ): CommentReplyCard =
    CompositeSupport.buildComposite(new CommentReplyCard(firstComment, comment, owner))
}
