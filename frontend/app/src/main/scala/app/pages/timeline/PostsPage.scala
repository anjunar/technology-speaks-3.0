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
import jfx.core.state.{Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Input.input
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class PostsPage extends PageComposite("Posts") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 50
  private val postsProperty: RemoteListProperty[Data[Post], RemotePageQuery] =
    RemoteTableList.create[Data[Post]](pageSize = pageSize) { (index, limit) =>
      Post.list(index, limit)
    }

  override protected def compose(using DslContext): Unit = {
    classes = "posts-page"

    style {
      height = "100%"
    }

    addDisposable(
      ApplicationService.messageBus.subscribe {
        case _: PostCreated =>
          reloadPosts()
        case _: PostUpdated =>
          reloadPosts()
        case _ =>
          ()
      }
    )

    withDslContext {
      vbox {
        style {
          height = "100%"
          width = "100%"
        }

        div {
          style {
            padding = "12px"
          }

          val prompt = input("post") {
            style {
              padding = "12px"
              setProperty("width", "calc(100% - 24px)")
              backgroundColor = "var(--color-background-secondary)"
              fontSize = "24px"
              borderRadius = "8px"
            }
          }

          prompt.placeholder = "Nach was ist dir heute?"
          prompt.element.readOnly = true
          prompt.element.onclick = _ => Navigation.navigate("/timeline/posts/post")
        }

        div {
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
      classes = "glass-border"

      withDslContext {
        vbox {
          style {
            rowGap = "10px"
          }

          val header = componentHeader {}
          header.model(data.data)

          val editorField = editor("editor", true) {
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
          addDisposable(Property.subscribeBidirectional(data.data.editor, editorField.valueProperty))

          hbox {
            style {
              columnGap = "8px"
              alignItems = "center"
            }

            val likes = likeButton {}
            likes.model(data.data.likes, data.data.links)

            val commentPrompt = input("comment") {
              style {
                flex = "1"
                padding = "8px"
                borderRadius = "6px"
                backgroundColor = "var(--color-background-secondary)"
                border = "1px solid var(--color-background-primary)"
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
    def card(data: Data[Post]): PostFeedCard =
      CompositeSupport.buildComposite(new PostFeedCard(data))
  }


  def postsPage(init: PostsPage ?=> Unit = {}): PostsPage =
    CompositeSupport.buildPage(new PostsPage)(init)
}