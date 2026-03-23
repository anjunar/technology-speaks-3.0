package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.schema.Link
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.Transient
import java.util

trait LinksContainer {

  @JsonbProperty("$links")
  @Transient
  val links: util.List[Link] = new util.ArrayList[Link]()

  def addLinks(value: Link*): Unit = {
    value.filter(_ != null).foreach(link => links.add(link))
  }

}
