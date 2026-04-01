package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader, PropertyAccess}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelSerializer extends Serializer[Model[?]] {

  override def serialize(input: Model[?], context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()

    val jsonType = getJsonType(input)
    if (jsonType.isDefined) {
      out.update("@type", jsonType.get)
    }

    input.meta.properties.foreach { access =>
      if (!isIgnored(access)) {
        val value = access.asInstanceOf[PropertyAccess[Any, Any]].get(input)
        
        access.genericType match {
          case pt: com.anjunar.scala.enterprise.macros.reflection.ParameterizedType 
            if isMapType(pt) =>
            value match {
              case map: Map[?, ?] =>
                map.foreach { case (key, mapValue) =>
                  val serialized = serializeValue(mapValue, null)
                  out.update(key.toString, serialized)
                }
              case _ =>
            }
          case _ =>
            val serialized = serializeValue(value, access)
            out.update(getJsonFieldName(access), serialized)
        }
      }
    }
    out.asInstanceOf[js.Dynamic]
  }

  private def isMapType(tpe: com.anjunar.scala.enterprise.macros.reflection.Type): Boolean = {
    tpe match {
      case pt: com.anjunar.scala.enterprise.macros.reflection.ParameterizedType =>
        pt.rawType match {
          case sc: com.anjunar.scala.enterprise.macros.reflection.SimpleClass[?] => 
            sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
          case _ => false
        }
      case sc: com.anjunar.scala.enterprise.macros.reflection.SimpleClass[?] => 
        sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
      case _ => false
    }
  }

  private def getJsonType(model: Model[?]): Option[String] = {
    model.meta.annotations
      .collectFirst {
        case Annotation(className, params) if className == "jfx.json.JsonType" =>
          params.getOrElse("value", model.getClass.getSimpleName).asInstanceOf[String]
      }
  }

  private def getJsonFieldName(access: PropertyAccess[?, ?]): String = {
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "com.anjunar.scala.enterprise.macros.validation.JsonName" =>
          params.getOrElse("value", access.name).asInstanceOf[String]
      }
      .getOrElse(access.name)
  }

  private def isIgnored(access: PropertyAccess[?, ?]): Boolean = {
    val name = access.name
    name == "meta" || name == "##" || name.startsWith("$") || access.annotations.exists {
      case Annotation(className, _) => className == "jfx.json.JsonIgnore"
      case null => false
    }
  }

  private def serializeValue(value: Any, access: PropertyAccess[?, ?]): js.Any = {
    value match {
      case null => null
      case p: Property[?] =>
        val inner = p.get
        if (inner == null) null
        else serializeValue(inner, access)
      case p: ReadOnlyProperty[?] => serializeValue(p.get, access)
      case m: Model[?] => serialize(m, new JavaContext(null))
      case arr: js.Array[?] => arr.map(serializeValue(_, access))
      case map: Map[?, ?] => serializeMap(map)
      case v => value.asInstanceOf[js.Any]
    }
  }

  private def serializeMap(map: Map[?, ?]): js.Dynamic = {
    val out = js.Dictionary[js.Any]()
    map.foreach { case (key, value) =>
      val serialized = serializeValue(value, null)
      out.update(key.toString, serialized)
    }
    out.asInstanceOf[js.Dynamic]
  }

}
