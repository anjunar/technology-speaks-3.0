package jfx.json

import com.anjunar.scala.enterprise.macros.{MetaClassLoader, TypeHelper}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, Type}
import jfx.json.deserializer.{DeserializerFactory, JsonContext}
import jfx.json.serializer.{JavaContext, SerializerFactory}
import jfx.core.meta.ClassLoader

import scala.scalajs.js.Dynamic

class JsonMapper2 {
  
  def deserialize[E](json: Dynamic, javaType : Class[E]) : E = {

    val scalaType = MetaClassLoader.classes(javaType)
    
    DeserializerFactory.build(javaType)
      .deserialize(json, new JsonContext(scalaType))
      .asInstanceOf[E]
  }
  
  def serialize[E](model: E) : Dynamic = {
    val javaType = model.getClass.asInstanceOf[Class[E]]

    val scalaType = MetaClassLoader.classes(javaType)
    
    SerializerFactory.build(javaType)
      .serialize(model, new JavaContext(scalaType))
  }

}
