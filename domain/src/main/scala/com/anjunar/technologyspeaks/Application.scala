package com.anjunar.technologyspeaks

import com.anjunar.json.mapper.macros.PropertyMacros.describeProperties
import com.anjunar.json.mapper.macros.PropertyMacrosHelper
import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.Application.Schema
import com.anjunar.technologyspeaks.core.{ManagedRule, User}
import com.anjunar.technologyspeaks.rest.types.LinksContainer
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.collection.mutable

class Application(@(JsonbProperty @field) val user: User) extends DTO with LinksContainer

object Application extends SchemaProvider[Schema] {
  class Schema extends EntitySchema[Application]() {
    override val properties: mutable.Map[String, Property[Application, Any]] = PropertyMacrosHelper.describeProperties[Application]
  }
}