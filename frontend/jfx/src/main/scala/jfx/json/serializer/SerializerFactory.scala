package jfx.json.serializer

import reflect.{ClassDescriptor, ParameterizedTypeDescriptor, TypeDescriptor}
import jfx.core.state.{ListProperty, Property}

import java.util.UUID
import scala.scalajs.js

object SerializerFactory {

  def build(typeName: String): Serializer[?] = {
    typeName match {
      case "java.lang.String" | "String" => new StringSerializer()
      case "scala.Int" | "Int" | "scala.Double" | "Double" | "scala.Float" | "Float" | "scala.Long" | "Long" => new NumberSerializer()
      case "scala.Boolean" | "Boolean" => new BooleanSerializer()
      case "java.util.UUID" | "UUID" => new UUIDSerializer()
      case "jfx.core.state.Property" | "Property" => new PropertySerializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertySerializer()
      case _ => new ModelSerializer()
    }
  }

  def build[E](clazz: Class[E]): Serializer[E] = (clazz match {
    case c if classOf[Property[?]].isAssignableFrom(c) => new PropertySerializer()
    case c if classOf[ListProperty[?]].isAssignableFrom(c) => new ListPropertySerializer()
    case c if classOf[String].isAssignableFrom(c) => new StringSerializer()
    case c if classOf[UUID].isAssignableFrom(c) => new UUIDSerializer()
    case c if classOf[Number].isAssignableFrom(c) => new NumberSerializer()
    case c if classOf[Boolean].isAssignableFrom(c) => new BooleanSerializer()
    case c if classOf[js.Array[?]].isAssignableFrom(c) => new JsArraySerializer()
    case _ =>  new ModelSerializer()
  }).asInstanceOf[Serializer[E]]

  def buildFromType(tpe: TypeDescriptor): Serializer[?] = tpe match {
    case cd: ClassDescriptor => cd.typeName match {
      case "java.lang.String" | "String" => new StringSerializer()
      case "scala.Int" | "Int" | "scala.Double" | "Double" | "scala.Float" | "Float" | "scala.Long" | "Long" => new NumberSerializer()
      case "scala.Boolean" | "Boolean" => new BooleanSerializer()
      case "java.util.UUID" | "UUID" => new UUIDSerializer()
      case "jfx.core.state.Property" | "Property" => new PropertySerializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertySerializer()
      case _ => new ModelSerializer()
    }
    case pt: ParameterizedTypeDescriptor => pt.rawType.typeName match {
      case "jfx.core.state.Property" | "Property" => new PropertySerializer()
      case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertySerializer()
      case _ => new ModelSerializer()
    }
    case _ => new ModelSerializer()
  }
}
