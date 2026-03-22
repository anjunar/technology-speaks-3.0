package jfx.control

import jfx.core.component.ManagedElementComponent
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.HTMLImageElement

class Image extends ManagedElementComponent[HTMLImageElement] {

  override val element: HTMLImageElement = newElement("img")

  def src: String =
    element.src

  def src_=(value: String): Unit =
    element.src =
      if (value == null) ""
      else value

  def alt: String =
    element.alt

  def alt_=(value: String): Unit =
    element.alt =
      if (value == null) ""
      else value
}

object Image {

  def image(init: Image ?=> Unit = {}): Image =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new Image()
      DslRuntime.withComponentContext(ComponentContext(None, currentContext.enclosingForm)) {
        given Scope = currentScope
        given Image = component
        init
      }
      DslRuntime.attach(component, currentContext)
      component
    }

  def src(using image: Image): String =
    image.src

  def src_=(value: String)(using image: Image): Unit =
    image.src = value

  def alt(using image: Image): String =
    image.alt

  def alt_=(value: String)(using image: Image): Unit =
    image.alt = value
}
