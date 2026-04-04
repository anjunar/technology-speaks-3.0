package app.support

import jfx.router.RouteContext
import org.scalajs.dom.window

object LayoutResolver {
  private val LayoutQueryParam = "layout"
  private val MobileBreakpointPx = 900

  def resolve(using context: RouteContext): LayoutMode =
    queryOverride(using context).getOrElse(autoDetect())

  def queryOverrideFromNavigation: Option[LayoutMode] =
    Navigation.queryParam(LayoutQueryParam).flatMap(LayoutMode.parse)

  def autoDetect(): LayoutMode =
    if (window.innerWidth < MobileBreakpointPx) LayoutMode.Mobile
    else LayoutMode.Desktop

  def queryOverride(using context: RouteContext): Option[LayoutMode] =
    context.queryParams.get(LayoutQueryParam).flatMap(LayoutMode.parse)

  def withMode(path: String, mode: LayoutMode): String = {
    val separator =
      if (path.contains("?")) "&"
      else "?"
    s"$path${separator}$LayoutQueryParam=${mode.queryValue}"
  }
}
