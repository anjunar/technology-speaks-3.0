package app.support

import scala.scalajs.js

object TimeAgo {

  def format(raw: String): String = {
    val normalized = Option(raw).map(_.trim).getOrElse("")

    if (normalized.isEmpty) {
      ""
    } else {
      val parsed = js.Date.parse(normalized)

      if (parsed.isNaN) {
        normalized
      } else {
        val deltaMs = math.max(0.0, js.Date.now() - parsed)
        val minutes = math.floor(deltaMs / 60000.0).toLong
        val hours = math.floor(deltaMs / 3600000.0).toLong
        val days = math.floor(deltaMs / 86400000.0).toLong

        if (days > 0) s"vor $days Tagen"
        else if (hours > 0) s"vor $hours Stunden"
        else s"vor ${math.max(0L, minutes)} Minuten"
      }
    }
  }
}
