package app.components.shared

import app.domain.shared.OwnerProvider
import app.support.{Navigation, TimeAgo}
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.image
import jfx.control.Link.link
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox

class ComponentHeader extends DivComposite {

  private val valueProperty: Property[OwnerProvider | Null] = Property(null)
  private var onDeleteHandler: () => Unit = () => ()
  private var onUpdateHandler: () => Unit = () => ()
  private var customDeleteHandler = false
  private var customUpdateHandler = false

  def model(next: OwnerProvider): Unit =
    valueProperty.set(next)

  def onDelete(fn: () => Unit): Unit = {
    customDeleteHandler = true
    onDeleteHandler = fn
  }

  def onUpdate(fn: () => Unit): Unit = {
    customUpdateHandler = true
    onUpdateHandler = fn
  }

  override protected def compose(using DslContext): Unit = {
    classProperty += "component-header"

    withDslContext {
      var avatarIconRef: jfx.layout.Div | Null = null
      var avatarImageRef: jfx.control.Image | Null = null
      var nameHeadingRef: jfx.control.Heading | Null = null
      var createdRef: jfx.layout.Span | Null = null
      var userLinkRef: jfx.control.Link | Null = null
      var readButtonRef: jfx.action.Button | Null = null
      var updateButtonRef: jfx.action.Button | Null = null
      var deleteButtonRef: jfx.action.Button | Null = null

      hbox {
        style {
          alignItems = "center"
          columnGap = "10px"
        }

        userLinkRef = link("/core/users") {
          style {
            display = "inline-flex"
            alignItems = "center"
            justifyContent = "center"
          }

          avatarIconRef = div {
            classes = "material-icons"
            style {
              fontSize = "48px"
            }
            text = "account_circle"
          }

          avatarImageRef = image {
            style {
              width = "48px"
              height = "48px"
              borderRadius = "50%"
              setProperty("object-fit", "cover")
              display = "none"
            }
          }
        }

        vbox {
          style {
            justifyContent = "center"
          }

          nameHeadingRef = heading(3) {
            text = "User"
          }

          createdRef = span {
            style {
              fontSize = "10px"
            }
          }
        }

        div {
          style {
            flex = "1"
          }
        }

        readButtonRef = button("open_in_new") {
          buttonType_=("button")
          classes = "material-icons"
          onClick { _ =>
            valueProperty.get match {
              case null =>
                ()
              case owner =>
                Navigation.linkByRel("read", owner.links).foreach(link => Navigation.navigate(link.url))
            }
          }
        }

        updateButtonRef = button("edit") {
          buttonType_=("button")
          classes = "material-icons"
          onClick { _ =>
            valueProperty.get match {
              case null =>
                ()
              case owner =>
                if (customUpdateHandler) {
                  onUpdateHandler()
                } else {
                  Navigation.linkByRel("update", owner.links).foreach(link => Navigation.navigate(link.url))
                }
            }
          }
        }

        deleteButtonRef = button("delete") {
          buttonType_=("button")
          classes = "material-icons"
          onClick { _ =>
            valueProperty.get match {
              case null =>
                ()
              case owner =>
                if (customDeleteHandler) {
                  onDeleteHandler()
                } else {
                  Navigation.linkByRel("delete", owner.links).foreach(link => Navigation.navigate(link.url))
                }
            }
          }
        }
      }

      addDisposable(
        valueProperty.observe { owner =>
          val user = Option(owner).flatMap(value => Option(value.user.get))
          val userId = user.flatMap(current => Option(current.id.get))
          val image = user.flatMap(current => Option(current.image.get))
          val userName = user.map(_.nickName.get).filter(_.trim.nonEmpty).getOrElse("User")
          val links = Option(owner).map(_.links).getOrElse(jfx.core.state.ListProperty())

          if (userLinkRef != null) {
            userLinkRef.nn.href = userId.map(id => s"/core/users/user/$id").getOrElse("/core/users")
          }

          if (avatarImageRef != null && avatarIconRef != null) {
            image match {
              case Some(media) =>
                avatarImageRef.nn.src = media.thumbnailLink()
                avatarImageRef.nn.element.style.display = "block"
                avatarIconRef.nn.element.style.display = "none"
              case None =>
                avatarImageRef.nn.src = ""
                avatarImageRef.nn.element.style.display = "none"
                avatarIconRef.nn.element.style.display = "flex"
            }
          }

          if (nameHeadingRef != null) {
            nameHeadingRef.nn.textContent = userName
          }

          if (createdRef != null) {
            createdRef.nn.textContent = Option(owner).map(value => TimeAgo.format(value.created.get)).getOrElse("")
          }

          if (readButtonRef != null) {
            readButtonRef.nn.element.style.display =
              if (Navigation.linkByRel("read", links).nonEmpty) "inline-flex" else "none"
          }

          if (updateButtonRef != null) {
            updateButtonRef.nn.element.style.display =
              if (customUpdateHandler || Navigation.linkByRel("update", links).nonEmpty) "inline-flex" else "none"
          }

          if (deleteButtonRef != null) {
            deleteButtonRef.nn.element.style.display =
              if (customDeleteHandler || Navigation.linkByRel("delete", links).nonEmpty) "inline-flex" else "none"
          }
        }
      )
    }
  }
}

object ComponentHeader {
  def componentHeader(init: ComponentHeader ?=> Unit = {}): ComponentHeader =
    CompositeSupport.buildComposite(new ComponentHeader)(init)
}
