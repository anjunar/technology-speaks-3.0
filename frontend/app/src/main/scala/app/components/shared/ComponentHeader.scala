package app.components.shared

import app.domain.core.MediaHelper
import app.domain.shared.OwnerProvider
import app.support.{Navigation, TimeAgo}
import app.ui.{CompositeSupport, DivComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src}
import jfx.control.Link.link
import jfx.core.component.ElementComponent.*
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.statement.Conditional.{conditional, thenDo, elseDo}

class ComponentHeader(val owner : OwnerProvider) extends DivComposite {

  private var onDeleteHandler: () => Unit = () => ()
  private var onUpdateHandler: () => Unit = () => ()
  private var customDeleteHandler = false
  private var customUpdateHandler = false

  def onDelete(fn: () => Unit): Unit = {
    customDeleteHandler = true
    onDeleteHandler = fn
  }

  def onUpdate(fn: () => Unit): Unit = {
    customUpdateHandler = true
    onUpdateHandler = fn
  }

  override protected def compose(using DslContext): Unit = {
    classes = "component-header"

    val user = Option(owner).flatMap(value => Option(value.user.get))
    val userId = user.flatMap(current => Option(current.id.get))
    val media = user.flatMap(current => Option(current.image.get))
    val userName = user.map(_.nickName.get).filter(_.trim.nonEmpty).getOrElse("User")
    val links = Option(owner).map(_.links).getOrElse(ListProperty())

    withDslContext {
      hbox {
        style {
          alignItems = "center"
          columnGap = "10px"
        }

        link(userId.map(id => s"/core/users/user/$id").getOrElse("/core/users")) {

          style {
            display = "inline-flex"
            alignItems = "center"
            justifyContent = "center"
          }


          if (media.isDefined) {
            image {
              style {
                width = "48px"
                height = "48px"
                borderRadius = "50%"
                setProperty("object-fit", "cover")
              }

              src = MediaHelper.thumbnailLink(media.get)
            }
          } else {
            div {
              classes = "material-icons"
              style {
                fontSize = "48px"
              }
              text = "account_circle"
            }
          }

        }

        vbox {
          style {
            justifyContent = "center"
          }

          heading(3) {
            text = userName
            style {
              margin = "0"
            }
          }

          span {
            style {
              fontSize = "10px"
            }
            text = Option(owner).map(value => TimeAgo.format(value.created.get)).getOrElse("")
          }
        }

        div {
          style {
            flex = "1"
          }
        }

        Navigation.linkByRel("read", links).foreach { link =>
          button("open_in_new") {
            buttonType_=("button")
            classes = "material-icons"
            onClick { _ =>
              Navigation.navigate(link.url)
            }
          }
        }

        Navigation.linkByRel("update", owner.links).foreach(link => {
          if (customUpdateHandler) {
            button("edit") {
              buttonType_=("button")
              classes = "material-icons"
              onClick { _ =>
                if (customUpdateHandler) {
                  onUpdateHandler()
                } else {
                  Navigation.navigate(link.url)
                }
              }
            }
          }
        })

        Navigation.linkByRel("delete", owner.links).foreach(link => {
          if (customDeleteHandler) {
            button("delete") {
              buttonType_=("button")
              classes = "material-icons"
              onClick { _ =>
                if (customDeleteHandler) {
                  onDeleteHandler()
                } else {
                  Navigation.navigate(link.url)
                }
              }
            }
          }
        })
      }
    }
  }
}

object ComponentHeader {
  def componentHeader(owner : OwnerProvider)(init: ComponentHeader ?=> Unit = {}): ComponentHeader =
    CompositeSupport.buildComposite(new ComponentHeader(owner))(init)

  def onUpdate(value : () => Unit)(using component: ComponentHeader): Unit =
    component.onUpdate(value)

  def onDelete(value : () => Unit)(using component: ComponentHeader): Unit =
    component.onDelete(value)


}
