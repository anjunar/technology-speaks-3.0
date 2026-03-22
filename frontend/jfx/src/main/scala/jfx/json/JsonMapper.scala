package jfx.json

import jfx.core.state.{ListProperty, Property, PropertyAccess, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic


class JsonMapper(val registry : JsonRegistry) {

  def deserialize[M <: Model[M]](dynamic : Dynamic): M = {
    val entityType = dynamic.selectDynamic("@type").asInstanceOf[String]
    val factory =
      registry.classes
        .get(entityType)
        .getOrElse(throw IllegalStateException(s"No JsonMapper factory registered for @type '$entityType'"))
        .asInstanceOf[() => M]

    deserialize(dynamic, factory)
  }

  def deserialize[M <: Model[M]](dynamic: Dynamic, factory: () => M): M = {
    val entity = factory()
    val props = entity.properties.asInstanceOf[js.Array[PropertyAccess[M, Any]]]

    props.foreach { access =>
      val value = readField(dynamic, access.name)
      if (value != null && !js.isUndefined(value)) {
        val decoded = deserializeValue(value, access.get(entity).orNull)
        assign(entity, access, decoded)
      }
    }

    entity
  }

  private def readField(dynamic: Dynamic, fieldName: String): js.Any = {
    val direct = dynamic.selectDynamic(fieldName).asInstanceOf[js.Any]
    if (direct != null && !js.isUndefined(direct)) {
      direct
    } else {
      dynamic.selectDynamic(registry.serializeFieldName(fieldName)).asInstanceOf[js.Any]
    }
  }

  private def assign[M](
    entity: M,
    access: PropertyAccess[M, Any],
    decoded: Any
  ): Unit =
    access.get(entity) match {
      case Some(property: Property[Any @unchecked]) =>
        property.set(decoded)

      case Some(list: ListProperty[Any @unchecked]) =>
        decoded match {
          case value: ListProperty[Any @unchecked] if value.eq(list) =>
            ()
          case array: js.Array[?] =>
            list.setAll(array.asInstanceOf[js.Array[Any]])
          case iterable: IterableOnce[?] =>
            list.setAll(iterable.iterator.asInstanceOf[Iterator[Any]])
          case null =>
            list.clear()
          case other =>
            list.setAll(Seq(other.asInstanceOf[Any]))
        }

      case _ =>
        access.set(entity, decoded)
    }

  private def deserializeValue(raw: js.Any, currentValue: Any): Any =
    if (raw == null || js.isUndefined(raw)) {
      raw
    } else if (raw.isInstanceOf[js.Array[?]]) {
      val items = raw.asInstanceOf[js.Array[js.Any]].map(deserializeArrayElement)
      currentValue match {
        case list: ListProperty[Any @unchecked] =>
          list.setAll(items.asInstanceOf[js.Array[Any]])
          list
        case _ =>
          items
      }
    } else {
      val dynamicValue = raw.asInstanceOf[Dynamic]

      if (canDeserialize(dynamicValue)) {
        deserialize(dynamicValue)
      } else {
        raw
      }
    }

  private def deserializeArrayElement(value: js.Any): Any =
    if (value == null || js.isUndefined(value)) {
      value
    } else {
      val dynamicValue = value.asInstanceOf[Dynamic]

      if (!canDeserialize(dynamicValue)) {
        value
      } else {
        deserialize(dynamicValue)
      }
    }

  private def hasType(value: Dynamic): Boolean = {
    val rawType = value.selectDynamic("@type").asInstanceOf[js.Any]
    rawType != null && !js.isUndefined(rawType)
  }

  private def canDeserialize(value: Dynamic): Boolean = {
    val rawType = value.selectDynamic("@type").asInstanceOf[js.Any]

    rawType != null &&
    !js.isUndefined(rawType) &&
    registry.classes.get(rawType.asInstanceOf[String]).isDefined
  }

  def serialize(model : Model[?]): Dynamic = {
    val out = js.Dictionary[js.Any]()

    out.update("@type", model.getClass.getSimpleName)

    val props = model.properties.asInstanceOf[js.Array[PropertyAccess[Any, Any]]]
    props.foreach { access =>
      access.get(model) match {
        case Some(propertyOrValue) =>
          val serialized = serializeValue(propertyOrValue)
          if (serialized != null && !js.isUndefined(serialized)) {
            out.update(registry.serializeFieldName(access.name), serialized)
          }
        case None => ()
      }
    }

    out.asInstanceOf[js.Dynamic]
  }

  private def serializeValue(value: Any): js.Any =
    value match {
      case null =>
        null

      case p: ReadOnlyProperty[?] =>
        serializeValue(p.get)

      case m: Model[?] =>
        serialize(m)

      case arr: js.Array[?] =>
        arr.map(elem => serializeValue(elem))

      case v =>
        v.asInstanceOf[js.Any]
    }

}
