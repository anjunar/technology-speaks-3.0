package com.anjunar.technologyspeaks.curation

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.Embeddable

import scala.annotation.meta.field

@Embeddable
class SourceRef(
  @(JsonbProperty @field)
  var sourceType: String = null,
  @(JsonbProperty @field)
  var sourceId: String = null,
  @(JsonbProperty @field)
  var sourceVersion: String = null
) {
  def this() = this(null, null, null)
}
