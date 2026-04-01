package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

object DeserializerFactory {

  def build[E](clazz: Class[E]): Deserializer[E] = {
    val result = clazz match {
      case clazz: Class[E] if classOf[Property[?]].isAssignableFrom(clazz) => new PropertyDeserializer()
      case clazz: Class[E] if classOf[ListProperty[?]].isAssignableFrom(clazz) => new ListPropertyDeserializer()
      case clazz: Class[E] if classOf[Model[?]].isAssignableFrom(clazz) => new ModelDeserializer()
      case clazz: Class[E] if classOf[String].isAssignableFrom(clazz) => new StringDeserializer()
      case clazz: Class[E] if classOf[UUID].isAssignableFrom(clazz) => new UUIDDeserializer()
      case clazz: Class[E] if classOf[Number].isAssignableFrom(clazz) => new NumberDeserializer()
      case clazz: Class[E] if classOf[Boolean].isAssignableFrom(clazz) => new BooleanDeserializer()
      case clazz: Class[E] if classOf[js.Array[?]].isAssignableFrom(clazz) => new ListPropertyDeserializer()
      case _ => throw new IllegalArgumentException(s"No deserializer found for class $clazz")
    }

    result.asInstanceOf[Deserializer[E]]
  }

  def buildFromType(tpe: Type): Deserializer[?] = {
    tpe match {
      case sc: SimpleClass[?] => build(sc.runtimeClass.asInstanceOf[Class[Any]])
      case pt: ParameterizedType =>
        pt.rawType match {
          case sc: SimpleClass[?] => build(sc.runtimeClass.asInstanceOf[Class[Any]])
          case _ => throw new IllegalArgumentException(s"No deserializer found for class $tpe")
        }
      case _ => throw new IllegalArgumentException(s"No deserializer found for class $tpe")
    }
  }

}