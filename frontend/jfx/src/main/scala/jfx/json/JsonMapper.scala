package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.json.deserializer.{Deserializer, DeserializerFactory, JsonContext, ModelDeserializer}
import jfx.json.serializer.{JavaContext, ModelSerializer, Serializer, SerializerFactory}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class JsonMapper {

  def deserialize[M <: Model[M]](json: Dynamic): M = {
    val rawClass = guessModelClass(json)
    val javaType = rawClass match {
      case sc: SimpleClass[?] => sc
      case _ => throw new IllegalArgumentException(s"Cannot determine type from JSON")
    }
    deserialize(json, javaType).asInstanceOf[M]
  }

  private def guessModelClass(json: Dynamic): SimpleClass[?] = {
    val fieldNames = js.Object.keys(json.asInstanceOf[js.Object]).asInstanceOf[js.Array[String]].toSet

    val candidates = MetaClassLoader.factories.toSeq
      .collect {
        case (sc: SimpleClass[?], factory) =>
          factory() match {
            case model: Model[?] =>
              val modelProps = model.meta.properties.map(p => getJsonFieldName(p)).toSet
              val matches = modelProps.count(fieldNames.contains)
              (sc, matches)
            case _ => (sc, 0)
          }
      }
      .filter(_._2 > 0)
      .sortBy(-_._2)

    candidates.headOption.map(_._1).getOrElse {
      throw new IllegalArgumentException(s"Cannot find matching model for JSON fields: ${fieldNames.mkString(", ")}")
    }
  }

  def deserialize[E](json: Dynamic, javaType: Type): E = {
    val rawClass = getRawClass(javaType)
    val deserializer =
      if (rawClass != null && classOf[Model[?]].isAssignableFrom(rawClass)) {
        new ModelDeserializer()
      } else {
        DeserializerFactory.build(rawClass)
      }
    deserializer.deserialize(json, new JsonContext(javaType)).asInstanceOf[E]
  }

  def deserializeArray[M <: Model[M]](json: Dynamic, javaType: Type): Seq[M] = {
    if (json == null || js.isUndefined(json) || !json.isInstanceOf[js.Array[?]]) return Seq.empty
    json.asInstanceOf[js.Array[Dynamic]].toSeq.map(j => deserialize(j, javaType).asInstanceOf[M])
  }

  def serialize[M <: Model[M]](model: M): Dynamic = {
    serialize(model, model.meta)
  }

  def serialize[E](model: E, javaType: Type): Dynamic = {
    val rawClass = getRawClass(javaType)
    if (rawClass != null && classOf[Model[?]].isAssignableFrom(rawClass)) {
      new ModelSerializer().serialize(model.asInstanceOf[Model[?]], new JavaContext(javaType))
    } else {
      val serializer = SerializerFactory.build(rawClass).asInstanceOf[Serializer[AnyRef]]
      serializer.serialize(model.asInstanceOf[AnyRef], new JavaContext(javaType))
    }
  }

  private def getRawClass(javaType: Type): Class[?] = {
    javaType match {
      case sc: SimpleClass[?] =>
        sc.runtimeClass
      case pt: ParameterizedType =>
        getRawClass(pt.rawType)
      case _ => null
    }
  }

  private def getJsonFieldName(access: PropertyAccess[?, ?]): String = {
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "jfx.json.JsonType" =>
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
