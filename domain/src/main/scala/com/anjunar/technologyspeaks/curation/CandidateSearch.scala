package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.hibernate.search.AbstractSearch
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field

class CandidateSearch(
  @(JsonbProperty @field)
  val status: String = "",
  @(JsonbProperty @field)
  val resonanceType: String = "",
  @(JsonbProperty @field)
  val query: String = "",
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 50
) extends AbstractSearch(sort, index, limit)
