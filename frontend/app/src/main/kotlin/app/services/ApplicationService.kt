package app.services

import app.domain.Application
import app.domain.core.Data
import app.domain.timeline.Post
import jFx2.client.JsonClient
import jFx2.state.Property
import jFx2.state.Disposable
import kotlinx.browser.document

object ApplicationService {

    val app = Property(Application())

    val darkMode = Property(true)

    init {
        darkMode.observe {
            if (it) {
                document.documentElement?.setAttribute("data-theme", "dark")
            } else {
                document.documentElement?.setAttribute("data-theme", "light")
            }
        }
    }

    interface Message

    class MessageBus {
        private val listeners = LinkedHashMap<Int, (Message) -> Unit>()
        private var nextId = 1

        fun publish(message: Message) {
            val snapshot = listeners.values.toList()
            snapshot.forEach { runCatching { it(message) } }
        }

        fun subscribe(listener: (Message) -> Unit): Disposable {
            val id = nextId++
            listeners[id] = listener
            return Disposable { listeners.remove(id) }
        }
    }

    val messageBus = MessageBus()

    suspend fun invoke() {
        app.set(Application.read())
    }

}
