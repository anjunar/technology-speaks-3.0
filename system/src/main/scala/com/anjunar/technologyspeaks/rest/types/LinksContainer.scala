package com.anjunar.technologyspeaks.rest.types

import com.anjunar.json.mapper.schema.Link
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.Transient

import scala.beans.BeanProperty

object LinksContainer {

  trait Interface {

    def getLinks(): java.util.List[Link]

    def addLinks(value: Link*): Unit = {
      value.filter(_ != null).foreach(link => getLinks().add(link))
    }
  }

  trait Trait extends Interface {

    @JsonbProperty("$links")
    @Transient
    @BeanProperty val links: java.util.List[Link] = new java.util.ArrayList[Link]()
  }

}
