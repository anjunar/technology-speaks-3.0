package jfx.json

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
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
    val props = entity.properties.asInstanceOf[Seq[PropertyAccess[M, Any]]]

    props.foreach { access =>
      val raw = readField(dynamic, access.name)

      if (raw != null && !js.isUndefined(raw)) {
        val currentValue = access.get(entity)
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
    currentValue match {
      case property: Property[Any @unchecked] =>
        deserializeValue(raw, property.get)

      case list: ListProperty[Any @unchecked] =>
        deserializeValue(raw, null)

      case _ =>
        deserializeValue(raw, null)
    }
  }

  private def createDefaultForAccess[M](access: PropertyAccess[M, Any]): Option[Any] = {
    // In the frontend, properties are already initialized with default values
    // in the case class constructors, so we don't need to create defaults here.
    None
  }

  private def assign[M](
                         entity: M,
                         access: PropertyAccess[M, Any],
                         decoded: Any
                       ): Unit = {
    val accessValue = access.get(entity)

    accessValue match {
      case property: Property[Any @unchecked] =>
        property.set(decoded)

      case list: ListProperty[Any @unchecked] =>
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
  }

  private def deserializeValue(raw: js.Any, currentValue: Any): Any =
    if (raw == null || js.isUndefined(raw)) {
      null
    } else {
      val targetType = currentValue match {
        case null => classOf[Object]
        case _ => currentValue.getClass
      }
      
      registry.deserializeValue(raw, targetType).getOrElse {
        if (raw.isInstanceOf[js.Array[?]]) {
          raw.asInstanceOf[js.Array[js.Any]].map(value => deserializeArrayElement(value))
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
    val currentValue = access.get(entity)
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
      null
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

    val props = model.properties.asInstanceOf[Seq[PropertyAccess[Any, Any]]]
    props
      .filter(access => access.name != "links")
      .foreach { access =>
        val value = access.get(model)
        if (value != null) {
          val serialized = serializeValue(value)
          if (serialized != null && !js.isUndefined(serialized)) {
            out.update(registry.serializeFieldName(access.name), serialized)
          }
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