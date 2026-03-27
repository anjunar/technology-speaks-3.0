package app.pages.timeline

import app.components.likeable.LikeButton.likeButton
import app.components.shared.ComponentHeader.componentHeader
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.Data
import app.domain.timeline.{Post, PostCreated, PostUpdated}
import app.pages.timeline.PostsPage.PostFeedCard
import app.services.ApplicationService
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Input.input
import jfx.form.editor.plugins.*
import jfx.layout.Span.span
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class PostsPage(postsProperty : RemoteListProperty[Data[Post], RemotePageQuery]) extends PageComposite("Posts") {

  override def pageWidth: Int = 980
  override def pageHeight: Int = 860

  private val pageSize = 50

  override protected def compose(using DslContext): Unit = {
    classes = "posts-page"

    style {
      height = "100%"
    }

    withDslContext {
      val service = inject[ApplicationService]

      addDisposable(
        service.messageBus.subscribe {
          case _: PostCreated =>
            reloadPosts()
          case _: PostUpdated =>
            reloadPosts()
          case _ =>
            ()
        }
      )

      vbox {
        classes = "posts-page__layout"

        style {
          height = "100%"
          width = "100%"
        }

        div {
          classes = "posts-page__composer"

          vbox {
            classes = "posts-page__composer-copy"

            span {
              classes = "posts-page__eyebrow"
              text = "Resonanz"
            }

            span {
              classes = "posts-page__title"
              text = "Gedanken im Fluss"
            }
          }

          val prompt = input("post") {
            classes = "posts-page__prompt"
          }

          prompt.placeholder = "Nach was ist dir heute?"
          prompt.element.readOnly = true
          prompt.element.onclick = _ => Navigation.navigate("/timeline/posts/post")
        }

        div {
          classes = "posts-page__feed"

          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(postsProperty, estimateHeightPx = 240, overscanPx = 240, prefetchItems = 80) { (data, _) =>
            if (data == null) {
              val card = loadingCard {}
              card.minHeight("180px")
              card
            } else {
              PostFeedCard.card(data)
            }
          }
        }
      }
    }
  }

  private def reloadPosts(): Unit =
    RemoteTableList.reloadFirstPage(postsProperty, pageSize = pageSize)
}

object PostsPage {

  final class PostFeedCard(data: Data[Post]) extends DivComposite {

    override protected def compose(using DslContext): Unit = {
      classes = "post-card"

      withDslContext {
        vbox {
          classes = "post-card__body"

          style {
            rowGap = "10px"
          }

          componentHeader(data.data) {}

          val editorField = editor("editor", true) {
            classes = "post-card__editor"
            style {
              width = "100%"
            }

            basePlugin {}
            headingPlugin {}
            listPlugin {}
            linkPlugin {}
            imagePlugin {}
          }

          editorField.editableProperty.set(false)
          subscribeBidirectional(data.data.editor, editorField.valueProperty)

          hbox {
            classes = "post-card__footer"

            style {
              columnGap = "8px"
              alignItems = "center"
            }

            likeButton(data.data.likes, data.data.links) {}

            val commentPrompt = input("comment") {
              classes = "post-card__comment-input"
              style {
                flex = "1"
              }
            }

            commentPrompt.placeholder = "Kommentar schreiben..."
            commentPrompt.element.readOnly = true
            commentPrompt.element.onclick = _ => Navigation.navigate(s"/timeline/posts/post/${data.data.id.get}/view")
          }
        }
      }
    }
  }

  object PostFeedCard {
    def card(data: Data[Post])(using Scope): PostFeedCard =
      CompositeSupport.buildComposite(new PostFeedCard(data))
  }


  def postsPage(postsProperty : RemoteListProperty[Data[Post], RemotePageQuery])(using Scope)(init: PostsPage ?=> Unit = {}): PostsPage =
    CompositeSupport.buildPage(new PostsPage(postsProperty))(init)
}
