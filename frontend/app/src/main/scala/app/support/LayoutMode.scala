package app.support

enum LayoutMode(val queryValue: String) {
  case Desktop extends LayoutMode("desktop")
  case Mobile extends LayoutMode("mobile")
}

object LayoutMode {
  def parse(value: String): Option[LayoutMode] =
    Option(value)
      .map(_.trim.toLowerCase)
      .collect {
        case "desktop" => LayoutMode.Desktop
        case "mobile" => LayoutMode.Mobile
      }
}
