package app.pages.security

import app.domain.documents.Document
import app.domain.security.ConfirmCode
import app.services.ApplicationService
import app.support.Api
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType_=, onClick}
import jfx.control.Heading.heading
import jfx.control.Image.{image, src_=}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.layout.Div.div
import jfx.layout.HBox.hbox

import scala.concurrent.ExecutionContext

class ConfirmPage extends PageComposite("Bestaetigen", pageResizable = false) {

  private given ExecutionContext = ExecutionContext.global

  private val confirmForm = new ConfirmCode()

  override protected def compose(using DslContext): Unit = {
    classProperty += "confirm-page"

    withDslContext {
      val service = inject[ApplicationService]

      form(confirmForm) {
        onSubmit_= { (event : Form[ConfirmCode])  =>
          Api
            .post[app.support.JsonResponse](s"/service/security/confirm?code=${confirmForm.confirm.get}")
            .flatMap(_ => {
              service.invoke()
            })
            .foreach(_ => close())
        }

        image {
          style {
            jfx.dsl.width_=("500px")
          }
          src_=("/app/security/confirm.png")
        }

        inputContainer("Bestaetigen") {
          input("confirm") {}
        }

        hbox {
          style {
            justifyContent = "center"
          }
          heading(3) {
            text = "Bitte bestaetige deinen Email-Code"
          }
        }

        div {
          classes = "button-container"

          button("Abbrechen") {
            buttonType_=("button")
            classes = "btn-secondary"
            onClick(_ => close())
          }

          button("Bestaetigen") {
            classes = "btn-danger"
          }
        }
      }
    }
  }
}

object ConfirmPage {
  def confirmPage(init: ConfirmPage ?=> Unit = {}): ConfirmPage =
    CompositeSupport.buildPage(new ConfirmPage)(init)
}
