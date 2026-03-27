package app.services

import app.domain.Application
import app.services.ApplicationService.MessageBus
import jfx.core.state.{Disposable, Property}
import org.scalajs.dom.window

import scala.util.control.NonFatal

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ApplicationService {

  private val DarkModeStorageKey = "technology-speaks.theme"

  private given ExecutionContext = ExecutionContext.global

  val app: Property[Application] = Property(new Application())

  val darkMode: Property[Boolean] = Property(loadDarkMode())

  val messageBus: MessageBus = new MessageBus

  darkMode.observe { enabled =>
    storeDarkMode(enabled)
  }

  def invoke(): Future[Application] =
    Application.read().map { value =>
      app.set(value)
      value
    }

  private def loadDarkMode(): Boolean =
    try {
      Option(window.localStorage.getItem(DarkModeStorageKey))
        .map(_.trim.toLowerCase)
        .collect {
          case "dark"  => true
          case "light" => false
          case "true"  => true
          case "false" => false
        }
        .getOrElse(true)
    } catch {
      case NonFatal(_) => true
    }

  private def storeDarkMode(enabled: Boolean): Unit =
    try {
      window.localStorage.setItem(DarkModeStorageKey, if (enabled) "dark" else "light")
    } catch {
      case NonFatal(_) => ()
    }
}

object ApplicationService {
  trait Message

  class MessageBus {
    private val listeners = mutable.LinkedHashMap.empty[Int, Message => Unit]
    private var nextId: Int = 1

    def publish(message: Message): Unit = {
      val snapshot = listeners.values.toVector
      snapshot.foreach(listener => listener(message))
    }

    def subscribe(listener: Message => Unit): Disposable = {
      val id = nextId
      nextId += 1
      listeners.update(id, listener)
      () => listeners.remove(id)
    }
  }

}
