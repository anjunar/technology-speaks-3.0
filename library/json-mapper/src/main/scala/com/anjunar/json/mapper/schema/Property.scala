package com.anjunar.json.mapper.schema

import com.anjunar.scala.universe.TypeResolver
import jakarta.json.bind.annotation.JsonbProperty
import scala.beans.BeanProperty

class Property[T, V](
  propertyType: Class[?],
  collectionType: Class[?],
  var rule: VisibilityRule[T]
) {

  @JsonbProperty("type")   val typeName: String = propertyType.getSimpleName

  @JsonbProperty   val schema: EntitySchema[?] =
    if (classOf[java.util.Collection[?]].isAssignableFrom(propertyType) && collectionType != null) {
      fetchCompanionSchema(collectionType)
    } else {
      fetchCompanionSchema(propertyType)
    }

  private def fetchCompanionSchema(clazz: Class[?]): EntitySchema[?] = {
    val companionInstance = TypeResolver.companionInstance(clazz)
    if (companionInstance != null && companionInstance.isInstanceOf[SchemaProvider]) {
      companionInstance.asInstanceOf[SchemaProvider].schema()
    } else {
      null
    }
  }

}
