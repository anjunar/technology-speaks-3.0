package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.TypeHelper
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

object SerializerFactory {

  def build[E](clazz: Class[E]): Serializer[E] = {
    if (clazz == null) {
      return new GenericSerializer().asInstanceOf[Serializer[E]]
    }

    val result = clazz match {
      case clazz: Class[E] if classOf[Property[?]].isAssignableFrom(clazz) => new PropertySerializer()
      case clazz: Class[E] if classOf[ListProperty[?]].isAssignableFrom(clazz) => new ListPropertySerializer()
      case clazz: Class[E] if classOf[Model[?]].isAssignableFrom(clazz) => new ModelSerializer()
      case clazz: Class[E] if classOf[String].isAssignableFrom(clazz) => new StringSerializer()
      case clazz: Class[E] if classOf[UUID].isAssignableFrom(clazz) => new UUIDSerializer()
      case clazz: Class[E] if classOf[Number].isAssignableFrom(clazz) => new NumberSerializer()
      case clazz: Class[E] if classOf[Boolean].isAssignableFrom(clazz) => new BooleanSerializer()
      case clazz: Class[E] if classOf[js.Array[?]].isAssignableFrom(clazz) => new ListPropertySerializer()
      case _ => new GenericSerializer()
    }

    result.asInstanceOf[Serializer[E]]
  }

  def buildFromType(tpe: Type): Serializer[?] = {
    tpe match {
      case sc: SimpleClass[?] => build(sc.runtimeClass.asInstanceOf[Class[Any]])
      case pt: ParameterizedType =>
        pt.rawType match {
          case sc: SimpleClass[?] => build(sc.runtimeClass.asInstanceOf[Class[Any]])
          case _ => new GenericSerializer()
        }
      case _ => new GenericSerializer()
    }
  }

}

class GenericSerializer extends Serializer[Any] {
  override def serialize(input: Any, context: JavaContext): js.Dynamic = input.asInstanceOf[js.Dynamic]
}

