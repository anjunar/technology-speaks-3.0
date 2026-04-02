package jfx.json.deserializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor, TypeDescriptor}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

object DeserializerFactory {

  def build[E](clazz: Class[E]): Deserializer[E] = (clazz match {
    case c if classOf[Property[?]].isAssignableFrom(c) => new PropertyDeserializer()
    case c if classOf[ListProperty[?]].isAssignableFrom(c) => new ListPropertyDeserializer()
    case c if classOf[Model[?]].isAssignableFrom(c) => new ModelDeserializer()
    case c if classOf[String].isAssignableFrom(c) => new StringDeserializer()
    case c if classOf[UUID].isAssignableFrom(c) => new UUIDDeserializer()
    case c if classOf[Number].isAssignableFrom(c) => new NumberDeserializer()
    case c if classOf[Boolean].isAssignableFrom(c) => new BooleanDeserializer()
    case c if classOf[js.Array[?]].isAssignableFrom(c) => new JsArrayDeserializer()
    case _ => throw new IllegalArgumentException(s"No deserializer for $clazz")
  }).asInstanceOf[Deserializer[E]]

  def buildFromType(tpe: TypeDescriptor): Deserializer[?] = tpe match {
    case cd: ClassDescriptor => cd.typeName match {
      case "java.lang.String" | "String" => new StringDeserializer()
      case "scala.Int" | "Int" | "scala.Double" | "Double" | "scala.Float" | "Float" | "scala.Long" | "Long" => new NumberDeserializer()
      case "scala.Boolean" | "Boolean" => new BooleanDeserializer()
      case "java.util.UUID" | "UUID" => new UUIDDeserializer()
      case "scala.scalajs.js.Array" => new JsArrayDeserializer()
      case "jfx.core.state.Property" | "Property" => new PropertyDeserializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertyDeserializer()
      case _ => new ModelDeserializer()
    }
    case pt: ParameterizedTypeDescriptor => pt.rawType.typeName match {
      case "jfx.core.state.Property" | "Property" => new PropertyDeserializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertyDeserializer()
      case _ => new ModelDeserializer()
    }
    case _ => new ModelDeserializer()
  }
}
