package jfx.json

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.json.deserializer.{DeserializerFactory, JsonContext, ModelDeserializer}
import jfx.json.serializer.{JavaContext, ModelSerializer, Serializer, SerializerFactory}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class JsonMapper2 {

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

}
