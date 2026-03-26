package com.anjunar.json.mapper.schema

import com.anjunar.json.mapper.macros.PropertyMacrosHelper
import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.Link.Schema
import com.anjunar.json.mapper.schema.property.Property
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.collection.mutable

class Link(
  @(JsonbProperty @field) val rel: String = null,
  @(JsonbProperty @field) val url: String = null,
  @(JsonbProperty @field) val method: String = null,
  @(JsonbProperty @field)("@type") val id: String = null
) extends DTO

object Link extends SchemaProvider[Schema] {

  class Schema extends EntitySchema[Link]() {
    override val properties: mutable.Map[String, Property[Link, Any]] = PropertyMacrosHelper.describeProperties[Link]
  }

}
