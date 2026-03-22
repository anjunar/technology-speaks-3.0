package app

import app.services.ApplicationService
import app.support.Api
import jfx.dsl.Scope
import org.scalajs.dom.{HTMLDivElement, document}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.{Dynamic, Promise}

object Main {

  private given ExecutionContext = ExecutionContext.global

  def main(args: Array[String]): Unit = {

    val fontLoader = Dynamic.global.eval("""document.fonts.load("24px 'Material Icons'")""").asInstanceOf[Promise[js.Object]]

    fontLoader.`then`(_ => {
      val root = document.getElementById("root").asInstanceOf[HTMLDivElement | Null]

      if (root == null) {
        ()
      } else {
        ApplicationService.darkMode.observe { enabled =>
          if (enabled) {
            document.documentElement.setAttribute("data-theme", "dark")
          } else {
            document.documentElement.setAttribute("data-theme", "light")
          }
        }

        ApplicationService
          .invoke()
          .recover { case error =>
            Api.logFailure("ApplicationService.invoke", error)
            ApplicationService.app.get
          }
          .foreach { _ =>
            given Scope = Scope.root()

            val shell = AppShell.appShell()
            root.innerHTML = ""
            root.appendChild(shell.element)
            shell.onMount()
          }
      }
    })

  }
}
