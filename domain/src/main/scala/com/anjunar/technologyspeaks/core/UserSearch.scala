package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.search.AbstractSearch
import jakarta.json.bind.annotation.JsonbProperty
import org.springframework.web.bind.annotation.RequestParam

import scala.beans.BeanProperty

class UserSearch(
  nameValue: String = null,
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit) {

  @JsonbProperty
  @RequestParam
  @BeanProperty
  val name: String = nameValue

}
