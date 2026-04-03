package app.components.likeable

import app.domain.core.Link
import app.domain.shared.Like
import app.services.ApplicationService
import app.support.{Api, Navigation}
import app.ui.{CompositeSupport, DivComposite}
import reflect.macros.ReflectMacros.reflectType
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{Disposable, ListProperty, Property}
import jfx.dsl.*
import jfx.json.JsonMapper
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}


class LikeButton(val likes: ListProperty[Like], val links: ListProperty[Link], val rel: String = "like") extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val likedProperty: Property[Boolean] = Property(false)
  private val countProperty: Property[Int] = Property(0)
  private val busyProperty: Property[Boolean] = Property(false)

  private var likesObserver: Disposable = () => ()

  override protected def compose(using DslContext): Unit = {
    classes = "like-button"

    withDslContext {
      val service = inject[ApplicationService]
      var iconButtonRef: jfx.action.Button | Null = null
      var countRef: jfx.layout.Div | Null = null

      hbox {
        classes = "like-button__row"

        style {
          alignItems = "center"
          columnGap = "6px"
        }

        iconButtonRef = button("favorite_border") {
          buttonType_=("button")
          classes = Seq("material-icons", "hover", "like-button__toggle")
          onClick { _ =>
            if (!busyProperty.get) {
              Navigation.linkByRel(rel, links).foreach { link =>
                busyProperty.set(true)
                Api
                  .link(link)
                  .invoke
                  .raw[js.Any]
                  .onComplete {
                    case Success(raw) =>
                      likes.setAll(deserializeLikes(raw))
                      busyProperty.set(false)
                    case Failure(error) =>
                      Api.logFailure("Like", error)
                      busyProperty.set(false)
                  }
              }
            }
          }
        }

        countRef = div {
          classes = "like-button__count"
          text = "0"
        }
      }

      addDisposable(
        likes.observe(likes =>
          recomputeState(service)
        )
      )

      addDisposable(service.app.observe(_ => recomputeState(service)))
      addDisposable(
        likedProperty.observe { liked =>
          if (iconButtonRef != null) {
            iconButtonRef.nn.textContent = if (liked) "favorite" else "favorite_border"
            if (liked) iconButtonRef.nn.element.classList.add("active")
            else iconButtonRef.nn.element.classList.remove("active")
          }
        }
      )
      addDisposable(countProperty.observe(count => if (countRef != null) countRef.nn.textContent = count.toString))
      addDisposable(busyProperty.observe(busy => if (iconButtonRef != null) iconButtonRef.nn.element.disabled = busy))
    }
  }

  private def recomputeState(service: ApplicationService): Unit = {
    val currentUserId = Option(service.app.get.user.id.get)
    countProperty.set(likes.length)
    likedProperty.set(currentUserId.exists(id => likes.exists(like => like.user != null && like.user.id.get == id)))
  }

  private def deserializeLikes(raw: js.Any): Seq[Like] =
    if (raw == null || js.isUndefined(raw)) {
      Seq.empty
    } else if (js.Array.isArray(raw.asInstanceOf[js.Any])) {
      raw
        .asInstanceOf[js.Array[js.Any]]
        .iterator
        .collect {
          case value if value != null && !js.isUndefined(value) =>
            JsonMapper.deserialize(value.asInstanceOf[js.Dynamic], reflectType[Like]).asInstanceOf[Like]
        }
        .toSeq
    } else {
      Seq.empty
    }
}

object LikeButton {
  def likeButton(likes: ListProperty[Like], links: ListProperty[Link], rel: String = "like")(init: LikeButton ?=> Unit = {})(using Scope): LikeButton =
    CompositeSupport.buildComposite(new LikeButton(likes, links, rel))(init)
}
