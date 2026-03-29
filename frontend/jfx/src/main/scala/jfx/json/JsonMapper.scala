package jfx.json

import jfx.core.state.{ListProperty, Property, PropertyAccess, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class JsonMapper(val registry: JsonRegistry) {

  def deserialize[M <: Model[M]](dynamic: Dynamic): M = {
    val entityTypeDynamic = dynamic.selectDynamic("@type")
    
    if (entityTypeDynamic == null || js.isUndefined(entityTypeDynamic)) {
      return null.asInstanceOf[M]
    }
    
    val entityType = entityTypeDynamic.asInstanceOf[String]
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
      val raw = readField(dynamic, access.name)

      if (raw != null && !js.isUndefined(raw)) {
        val currentValue = access.get(entity).orNull
        val decoded = deserializeForAccess(raw, access, currentValue)
        assign(entity, access, decoded)
      } else if (shouldCreateValue(access, entity)) {
        createDefaultForAccess(access).foreach(assign(entity, access, _))
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

  private def deserializeForAccess[M](
                                       raw: js.Any,
                                       access: PropertyAccess[M, Any],
                                       currentValue: Any
                                     ): Any = {
    val effectiveTargetType = effectiveTargetTypeOf(access, currentValue)
    deserializeValue(raw, effectiveTargetType, currentValue)
  }

  private def effectiveTargetTypeOf[M](
                                        access: PropertyAccess[M, Any],
                                        currentValue: Any
                                      ): Class[?] =
    currentValue match {
      case _: Property[?] =>
        innerValueTypeOf(access).getOrElse(classOf[Object])

      case _: ListProperty[?] =>
        innerValueTypeOf(access).getOrElse(classOf[Object])

      case _ =>
        rawTargetTypeOf(access)
    }

  private def rawTargetTypeOf[M](access: PropertyAccess[M, Any]): Class[?] =
    Option(access.propertyType).getOrElse(classOf[Object])

  private def innerValueTypeOf[M](access: PropertyAccess[M, Any]): Option[Class[?]] =
    Option(access.valueType)

  private def createDefaultForAccess[M](access: PropertyAccess[M, Any]): Option[Any] =
    innerValueTypeOf(access)
      .flatMap(registry.createValue)
      .orElse(Option(rawTargetTypeOf(access)).flatMap(registry.createValue))

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
          case same: ListProperty[Any @unchecked] if same.eq(list) =>
            ()

          case array: js.Array[?] =>
            list.setAll(array.asInstanceOf[js.Array[Any]])

          case iterable: IterableOnce[?] =>
            list.setAll(iterable.iterator)

          case null =>
            list.clear()

          case other =>
            list.setAll(Seq(other))
        }

      case _ =>
        access.set(entity, decoded)
    }

  private def deserializeValue(raw: js.Any, targetType: Class[?], currentValue: Any): Any =
    if (raw == null || js.isUndefined(raw)) {
      raw
    } else {
      registry.deserializeValue(raw, targetType).getOrElse {
        if (raw.isInstanceOf[js.Array[?]]) {
          val items = raw.asInstanceOf[js.Array[js.Any]].map(value => deserializeArrayElement(value))
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
      }
    }

  private def shouldCreateValue[M](access: PropertyAccess[M, Any], entity: M): Boolean = {
    val currentValue = access.get(entity).orNull
    createDefaultForAccess(access).nonEmpty && isEmptyValue(currentValue)
  }

  private def isEmptyValue(currentValue: Any): Boolean =
    currentValue match {
      case null => true
      case property: Property[Any @unchecked] => property.get == null
      case _ => false
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

  private def canDeserialize(value: Dynamic): Boolean = {
    val rawType = value.selectDynamic("@type").asInstanceOf[js.Any]

    rawType != null &&
      !js.isUndefined(rawType) &&
      registry.classes.get(rawType.asInstanceOf[String]).isDefined
  }

  def serialize(model: Model[?]): Dynamic = {
    val out = js.Dictionary[js.Any]()

    out.update("@type", model.getClass.getSimpleName)

    val props = model.properties.asInstanceOf[js.Array[PropertyAccess[Any, Any]]]
    props
      .filter(access => access.name != "links")
      .foreach { access =>
        access.get(model) match {
          case Some(propertyOrValue) =>
            val serialized = serializeValue(propertyOrValue)
            if (serialized != null && !js.isUndefined(serialized)) {
              out.update(registry.serializeFieldName(access.name), serialized)
            }
          case None =>
            ()
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
        registry.serializeValue(v).getOrElse(v.asInstanceOf[js.Any])
    }
}