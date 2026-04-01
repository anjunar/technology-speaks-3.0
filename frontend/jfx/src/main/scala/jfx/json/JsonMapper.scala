package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, ReadOnlyProperty}
import jfx.json.deserializer.{DeserializerFactory, JsonContext, ModelDeserializer}
import jfx.json.serializer.{JavaContext, ModelSerializer, SerializerFactory}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class JsonMapper {

  def deserialize[M <: Model[M]](json: Dynamic, meta: Type): M = {
    val deserializer = new ModelDeserializer()
    deserializer.deserialize(json, new JsonContext(meta)).asInstanceOf[M]
  }

  def deserializeArray[M <: Model[M]](json: Dynamic, meta: Type): Seq[M] = {
    if (json == null || js.isUndefined(json) || !json.isInstanceOf[js.Array[?]]) return Seq.empty
    json.asInstanceOf[js.Array[Dynamic]].toSeq.map(j => deserialize(j, meta))
  }

  def serialize(model: Model[?]): Dynamic = {
    new ModelSerializer().serialize(model, new JavaContext(model.meta))
  }

  private def getJsonFieldName(access: PropertyAccess[?, ?]): String = {
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "com.anjunar.scala.enterprise.macros.validation.JsonName" =>
          params.getOrElse("value", access.name).asInstanceOf[String]
      }
      .getOrElse(access.name)
  }

  def isIgnored(access: PropertyAccess[?, ?]): Boolean = {
    val name = access.name
    name == "meta" || name == "##" || name.startsWith("$") || access.annotations.exists {
      case Annotation(className, _) => className == "jfx.json.JsonIgnore"
      case null => false
    }
  }

  private def readField(dynamic: Dynamic, access: PropertyAccess[?, ?]): js.Any = {
    val jsonName = getJsonFieldName(access)
    if (jsonName != access.name) {
      val annotated = dynamic.selectDynamic(jsonName).asInstanceOf[js.Any]
      if (annotated != null && !js.isUndefined(annotated)) return annotated
    }
    dynamic.selectDynamic(access.name).asInstanceOf[js.Any]
  }

}
