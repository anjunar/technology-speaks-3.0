package app.pages.security

import app.services.ApplicationService
import app.support.Api
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class LogoutPage extends PageComposite("Abmelden", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "logout-page"

    withDslContext {

      val service = inject[ApplicationService]

      vbox {
        image {
          style {
            jfx.dsl.width_=("500px")
          }
          src_=("/app/security/logout.png")
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Moechtest du dich wirklich abmelden?"
          }
        }

        div {
          classes = "button-container"

          button("Abbrechen") {
            buttonType_=("button")
            classes = "btn-secondary"
            onClick(_ => close())
          }

          button("Abmelden") {
            classes = "btn-danger"
            onClick { _ =>
              Api
                .post[app.support.JsonResponse]("/service/security/logout")
                .flatMap(_ => {
                  service.invoke()
                })
                .foreach(_ => close())
            }
          }
        }
      }
    }
  }
}

object LogoutPage {
  def logoutPage(init: LogoutPage ?=> Unit = {})(using Scope): LogoutPage =
    CompositeSupport.buildPage(new LogoutPage)(init)
}
