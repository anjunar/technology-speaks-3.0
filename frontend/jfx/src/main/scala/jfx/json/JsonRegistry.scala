package jfx.json

import java.util.UUID
import scala.collection.mutable
import scala.scalajs.js

trait JsonRegistry {

  val classes : js.Map[String, () => Any]

  protected val valueFactories: mutable.Map[String, () => Any] =
    mutable.Map(
      classOf[UUID].getName -> (() => UUID.randomUUID())
    )

  protected val valueDeserializers: mutable.Map[String, js.Any => Any] =
    mutable.Map(
      classOf[UUID].getName -> ((raw: js.Any) => UUID.fromString(raw.toString))
    )

  protected val valueSerializers: mutable.Map[Class[?], Any => js.Any] =
    mutable.Map(
      classOf[UUID] -> ((value: Any) => value.asInstanceOf[UUID].toString.asInstanceOf[js.Any])
    )

  def createValueByTypeName(targetTypeName: String): Option[Any] =
    valueFactories.get(targetTypeName).map(_())

  def deserializeValueByTypeName(raw: js.Any, targetTypeName: String): Option[Any] =
    if (raw == null || js.isUndefined(raw)) None
    else valueDeserializers.get(targetTypeName).map(_(raw))

  def serializeValue(value: Any): Option[js.Any] =
    if (value == null || js.isUndefined(value.asInstanceOf[js.Any])) None
    else {
      try {
        val runtimeType = value.asInstanceOf[AnyRef].getClass
        if (runtimeType == null) None
        else findByRuntimeType(valueSerializers, runtimeType).map(_(value))
      } catch {
        case _: Throwable => None
      }
    }

  private def findByRuntimeType[V](entries: mutable.Map[Class[?], V], runtimeType: Class[?]): Option[V] =
    if (runtimeType == null) None
    else entries.collectFirst {
      case (registeredType, handler) if registeredType.isAssignableFrom(runtimeType) => handler
    }

  def normalizeFieldName(name: String): String =
    name match {
      case "$links" => "links"
      case other    => other
    }

  def serializeFieldName(name: String): String =
    name match {
      case "links" => "$links"
      case other   => other
    }

}
