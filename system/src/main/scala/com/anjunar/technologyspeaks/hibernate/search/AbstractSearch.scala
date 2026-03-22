package com.anjunar.technologyspeaks.hibernate.search

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestSort
import jakarta.json.bind.annotation.JsonbProperty
import org.springframework.web.bind.annotation.RequestParam

import scala.beans.BeanProperty

open class AbstractSearch(
  sortValue: java.util.List[String],
  indexValue: Int,
  limitValue: Int
) {

  @JsonbProperty
  @RestSort
  @RequestParam
  @BeanProperty
  val sort: java.util.List[String] = sortValue

  @JsonbProperty
  @RequestParam
  @BeanProperty
  val index: Int = indexValue

  @JsonbProperty
  @RequestParam
  @BeanProperty
  val limit: Int = limitValue

}
