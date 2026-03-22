package app.components.likeable

import app.domain.core.Link
import app.domain.shared.Like
import app.services.ApplicationService
import app.support.{Api, AppJson, Navigation}
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.core.component.ElementComponent.*
import jfx.core.state.{Disposable, ListProperty, Property}
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

class LikeButton extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  private val likesProperty: Property[ListProperty[Like]] = Property(ListProperty())
  private val linksProperty: Property[ListProperty[Link]] = Property(ListProperty())
  private val relProperty: Property[String] = Property("like")
  private val likedProperty: Property[Boolean] = Property(false)
  private val countProperty: Property[Int] = Property(0)
  private val busyProperty: Property[Boolean] = Property(false)

  private var likesObserver: Disposable = () => ()

  def model(likes: ListProperty[Like], links: ListProperty[Link], rel: String = "like"): Unit = {
    likesProperty.set(if (likes == null) ListProperty() else likes)
    linksProperty.set(if (links == null) ListProperty() else links)
    relProperty.set(Option(rel).map(_.trim).filter(_.nonEmpty).getOrElse("like"))
  }

  override protected def compose(using DslContext): Unit = {
    classProperty += "like-button"

    withDslContext {
      var iconButtonRef: jfx.action.Button | Null = null
      var countRef: jfx.layout.Div | Null = null

      hbox {
        style {
          alignItems = "center"
          columnGap = "6px"
        }

        iconButtonRef = button("favorite_border") {
          buttonType_=("button")
          classes = Seq("material-icons", "hover")
          onClick { _ =>
            if (!busyProperty.get) {
              Navigation.linkByRel(relProperty.get, linksProperty.get).foreach { link =>
                busyProperty.set(true)
                Api
                  .requestJson(link.method, Navigation.prefixedServiceUrl(link.url))
                  .onComplete {
                    case Success(raw) =>
                      likesProperty.get.setAll(deserializeLikes(raw))
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
          text = "0"
        }
      }

      addDisposable(
        likesProperty.observe { likes =>
          likesObserver.dispose()
          likesObserver = likes.observe(_ => recomputeState())
          recomputeState()
        }
      )

      addDisposable(ApplicationService.app.observe(_ => recomputeState()))
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

  private def recomputeState(): Unit = {
    val likes = likesProperty.get
    val currentUserId = Option(ApplicationService.app.get.user.id.get).filter(_.trim.nonEmpty)
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
            AppJson.mapper.deserialize(value.asInstanceOf[js.Dynamic]).asInstanceOf[Like]
        }
        .toSeq
    } else {
      Seq.empty
    }
}

object LikeButton {
  def likeButton(init: LikeButton ?=> Unit = {}): LikeButton =
    CompositeSupport.buildComposite(new LikeButton)(init)
}
