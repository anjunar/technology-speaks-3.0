package app.services

import app.domain.Application
import app.services.ApplicationService.MessageBus
import jfx.core.state.{Disposable, Property}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ApplicationService {

  private given ExecutionContext = ExecutionContext.global

  val app: Property[Application] = Property(new Application())

  val darkMode: Property[Boolean] = Property(true)

  val messageBus: MessageBus = new MessageBus

  def invoke(): Future[Application] =
    Application.read().map { value =>
      app.set(value)
      value
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
