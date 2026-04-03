package app.domain.curation

import app.domain.core.AbstractLink
import jfx.json.JsonType

@JsonType("curation-space")
class CurationLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET"
) extends AbstractLink {

  override def name: String = "Verdichtungsraum"

  override def icon: String = "grain"
}
