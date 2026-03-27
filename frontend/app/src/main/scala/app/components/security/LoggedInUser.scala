package app.components.security

import app.domain.core.MediaHelper
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.control.Image.{image, src}
import jfx.core.component.ElementComponent.*
import jfx.layout.Div.div
import jfx.dsl.*
import jfx.dsl.Scope.inject
import jfx.layout.HBox.hbox
import jfx.statement.ObserveRender.observeRender

class LoggedInUser extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "logged-in-user"


    withDslContext {
      val service = inject[ApplicationService]

      observeRender(service.app) { app =>
        val label =
          Option(app.user.info.get)
            .map(info => s"${info.firstName.get} ${info.lastName.get}".trim)
            .filter(_.nonEmpty)
            .getOrElse(app.user.nickName.get)

        hbox {
          classes = "logged-in-user-inner"

          style {
            alignItems = "center"
            columnGap = "10px"
          }

          if (app.user.image.get != null) {
            image {
              classes = "logged-in-user-avatar"
              style {
                width = "32px"
                height = "32px"
              }

              src = MediaHelper.thumbnailLink(app.user.image.get)
            }
          } else {
            div {
              classes = Seq("material-icons", "logged-in-user-avatar")
              style {
                fontSize = "32px"
              }

              text = "account_circle"
            }
          }

          div {
            classes = "logged-in-user-label"
            text = label
          }
        }
      }
    }
  }
}

object LoggedInUser {
  def loggedInUser(init: LoggedInUser ?=> Unit = {})(using Scope): LoggedInUser =
    CompositeSupport.buildComposite(new LoggedInUser)(init)
}
