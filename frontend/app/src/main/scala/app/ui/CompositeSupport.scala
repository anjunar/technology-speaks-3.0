package app.ui

import jfx.core.component.CompositeComponent
import jfx.router.PageInfo
import org.scalajs.dom.HTMLDivElement

abstract class DivComposite extends CompositeComponent[HTMLDivElement] {
  protected type DslContext = CompositeComponent.DslContext

  override val element: HTMLDivElement = newElement("div")
}

abstract class PageComposite(pageTitle: String, pageResizable: Boolean = true)
    extends DivComposite, PageInfo {

  override def name: String = pageTitle

  override def resizable: Boolean = pageResizable
}

object CompositeSupport {

  inline def buildComposite[C <: DivComposite](component: C): C =
    CompositeComponent.composite(component)

  inline def buildComposite[C <: DivComposite](component: C)(init: C ?=> Unit): C = {
    given C = component
    init
    CompositeComponent.composite(component)
  }

  inline def buildPage[C <: PageComposite](component: C): C =
    buildComposite(component)

  inline def buildPage[C <: PageComposite](component: C)(init: C ?=> Unit): C =
    buildComposite(component)(init)
}
