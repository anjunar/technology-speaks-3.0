package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.{Annotation, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model
import jfx.json.{JsonHelpers, JsonMapper}

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelSerializer extends Serializer[Model[?]] {

  override def serialize(input: Model[?], context: JavaContext): Dynamic = {
    val out = js.Dictionary[js.Any]()
    getJsonType(input).foreach(t => out.update("@type", t))

    input.meta.properties.foreach { access =>
      if (!JsonHelpers.isIgnored(access)) {
        val value = access.asInstanceOf[PropertyAccess[Any, Any]].get(input)
        access.genericType match {
          case pt: ParameterizedType if isMapType(pt) =>
            value.asInstanceOf[Map[?, ?]].foreach { case (k, v) =>
              out.update(k.toString, serializeValue(v))
            }
          case _ =>
            out.update(JsonHelpers.getJsonFieldName(access), serializeValue(value))
        }
      }
    }
    out.asInstanceOf[js.Dynamic]
  }

  private def isMapType(tpe: com.anjunar.scala.enterprise.macros.reflection.Type): Boolean = tpe match {
    case pt: ParameterizedType => pt.rawType match {
      case sc: SimpleClass[?] => sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
      case _ => false
    }
    case sc: SimpleClass[?] => sc.typeName == "scala.collection.immutable.Map" || sc.typeName == "Map"
    case _ => false
  }

  private def getJsonType(model: Model[?]): Option[String] =
    model.meta.annotations.collectFirst {
      case Annotation(className, params) if className == "jfx.json.JsonType" =>
        params.getOrElse("value", model.getClass.getSimpleName).asInstanceOf[String]
    }

  private def serializeValue(value: Any): js.Any = value match {
    case null => null
    case p: Property[?] => serializeValue(p.get)
    case p: ReadOnlyProperty[?] => serializeValue(p.get)
    case m: Model[?] => JsonMapper.serialize(m)
    case arr: js.Array[?] => arr.map(serializeValue)
    case map: Map[?, ?] =>
      val out = js.Dictionary[js.Any]()
      map.foreach { case (k, v) => out.update(k.toString, serializeValue(v)) }
      out.asInstanceOf[js.Dynamic]
    case v => v.asInstanceOf[js.Any]
  }
}
