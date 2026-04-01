package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.TypeHelper
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

object SerializerFactory {

  def build[E](clazz: Class[E]): Serializer[E] = {
    val result = clazz match {
      case clazz: Class[E] if classOf[Property[?]].isAssignableFrom(clazz) => new PropertySerializer()
      case clazz: Class[E] if classOf[ListProperty[?]].isAssignableFrom(clazz) => new ListPropertySerializer()
      case clazz: Class[E] if classOf[Model[?]].isAssignableFrom(clazz) => new ModelSerializer()
      case clazz: Class[E] if classOf[String].isAssignableFrom(clazz) => new StringSerializer()
      case clazz: Class[E] if classOf[UUID].isAssignableFrom(clazz) => new UUIDSerializer()
      case clazz: Class[E] if classOf[Number].isAssignableFrom(clazz) => new NumberSerializer()
      case clazz: Class[E] if classOf[Boolean].isAssignableFrom(clazz) => new BooleanSerializer()
      case clazz: Class[E] if classOf[js.Array[?]].isAssignableFrom(clazz) => new ListPropertySerializer()
      case _ => throw new IllegalArgumentException(s"No serializer found for class $clazz")
    }

    result.asInstanceOf[Serializer[E]]
  }

}