package app

import app.services.ApplicationService
import app.support.Api
import jfx.dsl.{DslRuntime, Scope}
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
      val service = new ApplicationService()

      if (root == null) {
        ()
      } else {
        service.darkMode.observe { enabled =>
          val rootElement = document.documentElement
          rootElement.asInstanceOf[js.Dynamic].dataset.theme =
            if (enabled) "dark" else "light"
          rootElement.style.setProperty(
            "--image-app-background",
            if (enabled) """url("stars.jpg")""" else """url("beach.png")"""
          )
        }

        service
          .invoke()
          .recover { case error =>
            Api.logFailure("ApplicationService.invoke", error)
            service.app.get
          }
          .foreach { _ =>
            val scope = Scope.root()
            scope.singleton(service)

            DslRuntime.withScope(scope) {
              
              val shell = AppShell.appShell()(using scope)
              root.innerHTML = ""
              root.appendChild(shell.element)
              shell.onMount()
            }
          }
      }
    })

  }
}
