package jFx2

import jFx2.state.Disposable
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

fun EventTarget.on(type: String, listener: (Event) -> Unit): Disposable {
    addEventListener(type, listener)
    return { removeEventListener(type, listener) }
}

@JsName("encodeURIComponent")
external fun encodeURIComponent(str: String): String

@JsName("decodeURIComponent")
external fun decodeURIComponent(str: String): String

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun <T : Any> jsObject(block: T.() -> Unit): T {
    val o = js("({})") as T
    block(o)
    return o
}