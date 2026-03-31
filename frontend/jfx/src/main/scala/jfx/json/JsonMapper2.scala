package jfx.json

import com.anjunar.scala.enterprise.macros.{MetaClassLoader, TypeHelper}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, Type}
import jfx.json.deserializer.{DeserializerFactory, JsonContext}
import jfx.json.serializer.{JavaContext, SerializerFactory}
import jfx.core.meta.ClassLoader

import scala.scalajs.js.Dynamic

class JsonMapper2 {
  
  def deserialize[E](json: Dynamic, javaType : Type) : E = {
    DeserializerFactory.build(TypeHelper.rawType(javaType))
      .deserialize(json, new JsonContext(javaType))
      .asInstanceOf[E]
  }
  
  def serialize[E](model: E, javaType : Type) : Dynamic = {
    SerializerFactory.build(TypeHelper.rawType(javaType).asInstanceOf[Class[Any]])
      .serialize(model, new JavaContext(javaType))
  }

}
