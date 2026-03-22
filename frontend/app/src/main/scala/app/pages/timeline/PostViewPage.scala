package app.pages.timeline

import app.components.commentable.FirstCommentCard.firstCommentCard
import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.shared.FirstComment
import app.domain.timeline.Post
import app.services.ApplicationService
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Input.input
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import jfx.virtual.virtualList
import org.scalajs.dom.Node

import scala.concurrent.{ExecutionContext, Future}

class PostViewPage extends PageComposite("Post") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 40

  private val modelProperty: Property[Data[Post]] =
    Property(new Data[Post](new Post(user = Property(ApplicationService.app.get.user))))
  private val commentsProperty: RemoteListProperty[FirstComment, RemotePageQuery] =
    RemoteTableList.createMapped[Data[FirstComment], FirstComment](pageSize = pageSize) { (index, limit) =>
      val post = modelProperty.get.data
      if (post.id.get != null) {
        FirstComment.list(index, limit, post)
      } else {
        Future.successful(new Table[Data[FirstComment]]())
      }
    }(_.data)
  private val contentProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)

  def model(value: Data[Post]): Unit =
    modelProperty.set(value)

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("post-view-page", "container"))

    addDisposable(
      modelProperty.observe { value =>
        contentProperty.set(PostViewContent.content(value.data, commentsProperty, appendNewComment, replaceComment, removeComment))
        reloadComments()
      }
    )

    withDslContext {
      dynamicOutlet(contentProperty)
    }
  }

  private def reloadComments(): Unit =
    if (modelProperty.get.data.id.get != null) {
      RemoteTableList.reloadFirstPage(commentsProperty, pageSize = pageSize)
    } else {
      commentsProperty.clear()
    }

  private def appendNewComment(): Unit =
    if (!commentsProperty.lastOption.exists(_.editable.get)) {
      val comment = new FirstComment()
      comment.editable.set(true)
      comment.user.set(ApplicationService.app.get.user)
      commentsProperty += comment
    }

  private def replaceComment(previous: FirstComment, next: FirstComment): Unit = {
    val index = commentsProperty.indexWhere(_ eq previous)
    if (index >= 0) commentsProperty.update(index, next)
    else commentsProperty += next
  }

  private def removeComment(comment: FirstComment): Unit =
    commentsProperty.indexWhere(_ eq comment) match {
      case index if index >= 0 =>
        commentsProperty.remove(index)
      case _ =>
        ()
    }
}

object PostViewPage {
  def postViewPage(init: PostViewPage ?=> Unit = {}): PostViewPage =
    CompositeSupport.buildPage(new PostViewPage)(init)
}

private final class PostViewContent(
  post: Post,
  comments: ListProperty[FirstComment],
  appendNewComment: () => Unit,
  replaceComment: (FirstComment, FirstComment) => Unit,
  removeComment: FirstComment => Unit
) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      vbox {
        style {
          rowGap = "12px"
          height = "100%"
          padding = "12px"
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(comments, estimateHeightPx = 240, overscanPx = 240, prefetchItems = 40) { (comment, _) =>
            if (comment == null) {
              val card = loadingCard {}
              card.minHeight("160px")
              card
            } else {
              firstCommentCard(
                comment = comment,
                owner = post,
                onPersist = saved => replaceComment(comment, saved),
                onDeleteCompleted = saved => removeComment(saved)
              )
            }
          }
        }

        val prompt = input("newComment") {
          style {
            padding = "12px"
            width = "calc(100% - 24px)"
            backgroundColor = "var(--color-background-secondary)"
            fontSize = "24px"
            borderRadius = "8px"
          }
        }

        prompt.placeholder = "Neuer Kommentar..."
        prompt.element.readOnly = true
        prompt.element.onclick = _ => appendNewComment()
      }
    }
  }
}

private object PostViewContent {
  def content(
    post: Post,
    comments: ListProperty[FirstComment],
    appendNewComment: () => Unit,
    replaceComment: (FirstComment, FirstComment) => Unit,
    removeComment: FirstComment => Unit
  ): PostViewContent =
    CompositeSupport.buildComposite(new PostViewContent(post, comments, appendNewComment, replaceComment, removeComment))
}

private final class PostViewCard(post: Post) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      vbox {
        style {
          rowGap = "10px"
        }

        val header = componentHeader {}
        header.model(post)

        val editorField = editor("editor") {
          basePlugin {}
          headingPlugin {}
          listPlugin {}
          linkPlugin {}
          imagePlugin {}
        }

        editorField.editableProperty.set(false)
        addDisposable(Property.subscribeBidirectional(post.editor, editorField.valueProperty))
      }
    }
  }
}

private object PostViewCard {
  def card(post: Post): PostViewCard =
    CompositeSupport.buildComposite(new PostViewCard(post))
}
