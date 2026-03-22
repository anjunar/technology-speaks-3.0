package com.anjunar.technologyspeaks.shared.editor

import com.anjunar.json.mapper.provider.DTO
import jakarta.json.bind.annotation.JsonbProperty

import scala.beans.BeanProperty

class Node extends DTO {

  @JsonbProperty("type")
  @BeanProperty
  var nodeType: String = null

  @JsonbProperty
  @BeanProperty
  var content: java.util.List[Node] = new java.util.ArrayList[Node]()

  @JsonbProperty
  @BeanProperty
  var attrs: java.util.Map[String, Any] = new java.util.HashMap[String, Any]()

  @JsonbProperty
  @BeanProperty
  var text: String = null

  @JsonbProperty
  @BeanProperty
  var marks: java.util.List[Node] = new java.util.ArrayList[Node]()

}
