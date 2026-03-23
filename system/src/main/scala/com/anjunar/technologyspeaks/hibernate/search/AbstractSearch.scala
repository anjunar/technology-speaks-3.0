package com.anjunar.technologyspeaks.hibernate.search

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestSort
import jakarta.json.bind.annotation.JsonbProperty
import org.springframework.web.bind.annotation.RequestParam

open class AbstractSearch(
  sortValue: java.util.List[String],
  indexValue: Int,
  limitValue: Int
) {

  @JsonbProperty
  @RestSort
  @RequestParam
  val sort: java.util.List[String] = sortValue

  @JsonbProperty
  @RequestParam
  val index: Int = indexValue

  @JsonbProperty
  @RequestParam
  val limit: Int = limitValue

}
