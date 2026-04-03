package app.domain.curation

import jfx.core.state.Property

class SourceRef(
  var sourceType: String = "",
  var sourceId: String = "",
  var sourceVersion: String | Null = null
)
