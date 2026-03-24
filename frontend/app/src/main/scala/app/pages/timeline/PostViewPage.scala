package app.pages.timeline

import app.components.commentable.FirstCommentCard.firstCommentCard
import app.components.likeable.LikeButton.likeButton
import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{AbstractEntity, Data, Table}
import app.domain.shared.FirstComment
import app.domain.timeline.Post
import app.pages.timeline.PostViewPage.PostViewPostCard
import app.services.ApplicationService
import app.support.{RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Input.input
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

import scala.concurrent.{ExecutionContext, Future}

class PostViewPage(val model: Post) extends PageComposite("Post") {

  override def pageWidth: Int = 360 
  override def pageHeight: Int = 600

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 40

  private val itemsProperty: ListProperty[Data[? <: AbstractEntity[?]]] = ListProperty()
  private val commentsProperty: RemoteListProperty[FirstComment, RemotePageQuery] =
    RemoteTableList.createMapped[Data[FirstComment], FirstComment](pageSize = pageSize) { (index, limit) =>
      if (model.id.get != null) {
        FirstComment.list(index, limit, model)
      } else {
        Future.successful(new Table[Data[FirstComment]]())
      }
    }(_.data)

  override protected def compose(using DslContext): Unit = {
    classes = Seq("post-view-page", "container")

    syncItems()
    addDisposable(commentsProperty.observe(_ => syncItems()))

    if (model.id.get != null) {
      RemoteTableList.reloadFirstPage(commentsProperty, pageSize = pageSize)
    } else {
      commentsProperty.clear()
    }

    withDslContext {
      val service = inject[ApplicationService]

      vbox {
        style {
          height = "100%"
          width = "100%"
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(itemsProperty, estimateHeightPx = 240, overscanPx = 240, prefetchItems = 40) { (item, _) =>
            if (item == null) {
              val card = loadingCard {}
              card.minHeight("160px")
              card
            } else {
              item.data match {
                case post: Post =>
                  PostViewPostCard.card(post)
                case comment: FirstComment =>
                  firstCommentCard(
                    comment = comment,
                    owner = model,
                    onPersist = saved => replaceComment(comment, saved),
                    onDeleteCompleted = saved => removeComment(saved)
                  )
                case _ =>
                  val card = loadingCard {}
                  card.minHeight("160px")
                  card
              }
            }
          }
        }

        val prompt = input("newComment") {
          style {
            padding = "12px"
            width = "calc(100% - 48px)"
            backgroundColor = "var(--color-background-secondary)"
            fontSize = "24px"
            borderRadius = "8px"
            margin = "12px"
          }
        }

        prompt.placeholder = "Neuer Kommentar..."
        prompt.element.readOnly = true
        prompt.element.onclick = _ => appendNewComment(service)
        prompt.element.style.display =
          if (model.id.get != null) "block" else "none"
      }
    }
  }

  private def appendNewComment(service : ApplicationService): Unit =
    if (!commentsProperty.lastOption.exists(_.editable.get)) {
      val comment = new FirstComment()
      comment.editable.set(true)
      comment.user.set(service.app.get.user)
      commentsProperty += comment
      syncItems()
    }

  private def replaceComment(previous: FirstComment, next: FirstComment): Unit = {
    val index = commentsProperty.indexWhere(_ eq previous)
    if (index >= 0) commentsProperty.update(index, next)
    else commentsProperty += next
    syncItems()
  }

  private def removeComment(comment: FirstComment): Unit =
    commentsProperty.indexWhere(_ eq comment) match {
      case index if index >= 0 =>
        commentsProperty.remove(index)
        syncItems()
      case _ =>
        ()
    }

  private def syncItems(): Unit =
    itemsProperty.setAll(
      Seq(new Data(model).asInstanceOf[Data[? <: AbstractEntity[?]]]) ++
        commentsProperty.iterator.map(comment => new Data(comment).asInstanceOf[Data[? <: AbstractEntity[?]]])
    )
}

object PostViewPage {

  final class PostViewPostCard(post: Post) extends DivComposite {

    override protected def compose(using DslContext): Unit = {
      classes = "glass-border"

      withDslContext {
        vbox {
          style {
            rowGap = "10px"
            padding = "10px"
          }

          componentHeader(post) {}

          val editorField = editor("editor", true) {
            basePlugin {}
            headingPlugin {}
            listPlugin {}
            linkPlugin {}
            imagePlugin {}
          }

          editorField.editableProperty.set(false)
          subscribeBidirectional(post.editor, editorField.valueProperty)

          likeButton(post.likes, post.links) {}
        }
      }
    }
  }

  object PostViewPostCard {
    def card(post: Post)(using Scope): PostViewPostCard =
      CompositeSupport.buildComposite(new PostViewPostCard(post))
  }

  def postViewPage(model: Post, init: PostViewPage ?=> Unit = {})(using Scope): PostViewPage =
    CompositeSupport.buildPage(new PostViewPage(model))(init)
}
