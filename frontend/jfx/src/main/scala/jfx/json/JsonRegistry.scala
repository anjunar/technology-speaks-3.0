package jfx.json

import java.util.UUID
import scala.collection.mutable
import scala.scalajs.js

trait JsonRegistry {

  val classes : js.Map[String, () => Any]

  protected val valueFactories: mutable.Map[Class[?], () => Any] =
    mutable.Map(
      classOf[UUID] -> (() => UUID.randomUUID())
    )

  protected val valueDeserializers: mutable.Map[Class[?], js.Any => Any] =
    mutable.Map(
      classOf[UUID] -> ((raw: js.Any) => UUID.fromString(raw.toString))
    )

  protected val valueSerializers: mutable.Map[Class[?], Any => js.Any] =
    mutable.Map(
      classOf[UUID] -> ((value: Any) => value.asInstanceOf[UUID].toString.asInstanceOf[js.Any])
    )

  def createValue(targetType: Class[?]): Option[Any] =
    Option(targetType).flatMap(findByTargetType(valueFactories, _).map(_()))

  def deserializeValue(raw: js.Any, targetType: Class[?]): Option[Any] =
    if (raw == null || js.isUndefined(raw)) None
    else Option(targetType).flatMap(findByTargetType(valueDeserializers, _).map(_(raw)))

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

  private def findByTargetType[V](entries: mutable.Map[Class[?], V], targetType: Class[?]): Option[V] =
    entries.collectFirst {
      case (registeredType, handler) if registeredType.isAssignableFrom(targetType) => handler
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
