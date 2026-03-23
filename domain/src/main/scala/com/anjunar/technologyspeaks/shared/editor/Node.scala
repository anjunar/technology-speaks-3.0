package com.anjunar.technologyspeaks.shared.editor

import com.anjunar.json.mapper.provider.DTO
import jakarta.json.bind.annotation.JsonbProperty

import scala.beans.BeanProperty

class Node extends DTO {

  @JsonbProperty("type")
    var nodeType: String = null

  @JsonbProperty
    var content: java.util.List[Node] = new java.util.ArrayList[Node]()

  @JsonbProperty
    var attrs: java.util.Map[String, Any] = new java.util.HashMap[String, Any]()

  @JsonbProperty
    var text: String = null

  @JsonbProperty
    var marks: java.util.List[Node] = new java.util.ArrayList[Node]()

}
