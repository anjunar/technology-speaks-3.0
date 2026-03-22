package app.components.security

import app.domain.core.MediaHelper
import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.control.Image.{image, src}
import jfx.core.component.ElementComponent.*
import jfx.layout.Div.div
import jfx.dsl.*
import jfx.layout.HBox.hbox
import jfx.statement.ObserveRender.observeRender

class LoggedInUser extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "logged-in-user"


    withDslContext {

      observeRender(ApplicationService.app) { app =>
        val label =
          Option(app.user.info.get)
            .map(info => s"${info.firstName.get} ${info.lastName.get}".trim)
            .filter(_.nonEmpty)
            .getOrElse(app.user.nickName.get)

        hbox {

          style {
            alignItems = "center"
            columnGap = "10px"
          }

          if (app.user.image.get != null) {
            image {
              style {
                width = "32px"
                height = "32px"
              }

              src = MediaHelper.thumbnailLink(app.user.image.get)
            }
          } else {
            div {
              style {
                fontSize = "32px"
              }

              text = "account_circle"
              classes = "material-icons"
            }
          }

          div {
            text = label
          }
        }
      }

    }
  }
}

object LoggedInUser {
  def loggedInUser(init: LoggedInUser ?=> Unit = {}): LoggedInUser =
    CompositeSupport.buildComposite(new LoggedInUser)(init)
}
