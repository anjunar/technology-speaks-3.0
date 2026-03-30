package com.anjunar.json.mapper.macros

import com.anjunar.json.mapper.schema.DefaultRule
import com.anjunar.json.mapper.schema.property.Property
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.collection.mutable

object PropertyMacrosHelper {

  inline def describeProperties[E]: mutable.LinkedHashMap[String, Property[E, Any]] =
    mutable.LinkedHashMap[String, Property[E, Any]](PropertyMacros.describeProperties[E].map(property => (property.name, new Property(property.asInstanceOf[PropertyAccess[E, Any]], new DefaultRule[E])))*)
  
}
