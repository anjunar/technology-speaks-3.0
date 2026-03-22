package app.components.security

import app.services.ApplicationService
import app.ui.{CompositeSupport, DivComposite}
import jfx.core.component.ElementComponent.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

class LoggedInUser extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "logged-in-user"

    val user = ApplicationService.app.get.user
    val label =
      Option(user.info.get)
        .map(info => s"${info.firstName.get} ${info.lastName.get}".trim)
        .filter(_.nonEmpty)
        .getOrElse(user.nickName.get)

    withDslContext {
      hbox {
        div {
          text = "account_circle"
          classes = "material-icons"
        }
        div {
          text = label
        }
      }
    }
  }
}

object LoggedInUser {
  def loggedInUser(init: LoggedInUser ?=> Unit = {}): LoggedInUser =
    CompositeSupport.buildComposite(new LoggedInUser)(init)
}
