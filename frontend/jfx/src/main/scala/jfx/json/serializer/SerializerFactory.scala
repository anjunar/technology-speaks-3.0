package jfx.json.serializer

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import java.util.UUID
import scala.scalajs.js

object SerializerFactory {

  def build[E](clazz: Class[E]): Serializer[E] = (clazz match {
    case c if classOf[Property[?]].isAssignableFrom(c) => new PropertySerializer()
    case c if classOf[ListProperty[?]].isAssignableFrom(c) => new ListPropertySerializer()
    case c if classOf[Model[?]].isAssignableFrom(c) => new ModelSerializer()
    case c if classOf[String].isAssignableFrom(c) => new StringSerializer()
    case c if classOf[UUID].isAssignableFrom(c) => new UUIDSerializer()
    case c if classOf[Number].isAssignableFrom(c) => new NumberSerializer()
    case c if classOf[Boolean].isAssignableFrom(c) => new BooleanSerializer()
    case c if classOf[js.Array[?]].isAssignableFrom(c) => new JsArraySerializer()
    case _ => throw new IllegalArgumentException(s"No serializer for $clazz")
  }).asInstanceOf[Serializer[E]]

  def buildFromType(tpe: Type): Serializer[?] = tpe match {
    case sc: SimpleClass[?] => sc.typeName match {
      case "java.lang.String" | "String" => new StringSerializer()
      case "scala.Int" | "Int" | "scala.Double" | "Double" | "scala.Float" | "Float" | "scala.Long" | "Long" => new NumberSerializer()
      case "scala.Boolean" | "Boolean" => new BooleanSerializer()
      case "java.util.UUID" | "UUID" => new UUIDSerializer()
      case "jfx.core.state.Property" | "Property" => new PropertySerializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertySerializer()
      case _ => new ModelSerializer()
    }
    case pt: ParameterizedType => pt.rawType match {
      case sc: SimpleClass[?] => sc.typeName match {
        case "jfx.core.state.Property" | "Property" => new PropertySerializer()
        case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertySerializer()
        case _ => new ModelSerializer()
      }
      case _ => new ModelSerializer()
    }
    case _ => new ModelSerializer()
  }
}
