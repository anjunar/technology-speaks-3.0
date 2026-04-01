package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import com.anjunar.scala.enterprise.macros.{Annotation, MetaClassLoader}
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
      case clazz: Class[E] if classOf[js.Array[?]].isAssignableFrom(clazz) => new JsArrayDeserializer()
      case _ => throw new IllegalArgumentException(s"No deserializer found for class $clazz")
    }

    result.asInstanceOf[Deserializer[E]]
  }

  def buildFromType(tpe: Type): Deserializer[?] = {
    tpe match {
      case sc: SimpleClass[?] =>
        sc.typeName match {
          case "java.lang.String" | "String" => new StringDeserializer()
          case "scala.Int" | "Int" => new NumberDeserializer()
          case "scala.Double" | "Double" => new NumberDeserializer()
          case "scala.Float" | "Float" => new NumberDeserializer()
          case "scala.Long" | "Long" => new NumberDeserializer()
          case "scala.Boolean" | "Boolean" => new BooleanDeserializer()
          case "scala.scalajs.js.Array" => new JsArrayDeserializer()
          case "java.util.UUID" | "UUID" => new UUIDDeserializer()
          case "jfx.core.state.Property" | "Property" => new PropertyDeserializer()
          case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertyDeserializer()
          case _ =>
            MetaClassLoader.factories.get(sc) match {
              case Some(factory) => new ModelDeserializer()
              case None =>
                MetaClassLoader.getByTypeName(sc.typeName)
                  .flatMap(MetaClassLoader.factories.get) match {
                    case Some(factory) => new ModelDeserializer()
                    case None =>
                      val simpleName = sc.typeName.split('.').last
                      MetaClassLoader.getByTypeName(simpleName)
                        .flatMap(MetaClassLoader.factories.get) match {
                          case Some(factory) => new ModelDeserializer()
                          case None =>
                            MetaClassLoader.factories.collectFirst {
                              case (key, factory) if key.typeName == sc.typeName => new ModelDeserializer()
                            }.getOrElse {
                              findFactoryByJsonType(sc.typeName).getOrElse {
                                if (sc.subTypes.nonEmpty) new ModelDeserializer()
                                else throw new IllegalArgumentException(s"No deserializer found for type '${sc.typeName}'")
                              }
                            }
                        }
                  }
            }
        }
      case pt: ParameterizedType =>
        pt.rawType match {
          case sc: SimpleClass[?] =>
            sc.typeName match {
              case "jfx.core.state.Property" | "Property" => new PropertyDeserializer()
              case "jfx.core.state.ListProperty" | "ListProperty" => new ListPropertyDeserializer()
              case _ => buildFromType(sc)
            }
          case _ => throw new IllegalArgumentException(s"No deserializer found for class $tpe")
        }
      case _ => throw new IllegalArgumentException(s"No deserializer found for class $tpe")
    }
  }

  private def findFactoryByJsonType(typeName: String): Option[Deserializer[?]] = {
    MetaClassLoader.factories.collectFirst {
      case (key, factory) =>
        val clazz = MetaClassLoader.getByTypeName(key.typeName)
        clazz.flatMap { c =>
          c.annotations.collectFirst {
            case Annotation(className, params) if className == "jfx.json.JsonType" =>
              params.get("value") match {
                case Some(jsonTypeValue: String) if jsonTypeValue == typeName => new ModelDeserializer()
              }
          }
        }
    }.flatten
  }

}