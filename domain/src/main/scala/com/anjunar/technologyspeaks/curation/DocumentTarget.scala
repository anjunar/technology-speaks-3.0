package com.anjunar.technologyspeaks.curation

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.Embeddable

import scala.annotation.meta.field

@Embeddable
class DocumentTarget(
  @(JsonbProperty @field)
  var documentId: String = null,
  @(JsonbProperty @field)
  var sectionId: String = null
) {
  def this() = this(null, null)
}
