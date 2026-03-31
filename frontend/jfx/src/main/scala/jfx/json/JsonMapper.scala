package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class JsonMapper(val registry: JsonRegistry) {

  def deserialize[M <: Model[M]](dynamic: Dynamic): M = {
    val entityType = dynamic.selectDynamic("@type").asInstanceOf[String]

    if (entityType == null || js.isUndefined(entityType)) {
      return null.asInstanceOf[M]
    }

    val factory = registry.classes
      .get(entityType)
      .getOrElse(throw IllegalStateException(s"No JsonMapper factory registered for @type '$entityType'"))
      .asInstanceOf[() => M]

    deserialize(dynamic, factory)
  }

  def deserialize[M <: Model[M]](dynamic: Dynamic, factory: () => M): M = {
    val entity = factory()
    val props = entity.properties.asInstanceOf[Seq[PropertyAccess[M, Any]]]

    props.foreach { access =>
      val raw = readField(dynamic, access)

      if (raw != null && !js.isUndefined(raw)) {
        val currentValue = access.get(entity)
        val decoded = deserializeForAccess(raw, access, currentValue)
        assign(entity, access, decoded)
      }
    }

    entity
  }

  private def readField(dynamic: Dynamic, access: PropertyAccess[?, ?]): js.Any = {
    if (access.name == "links") {
      val dollarLinks = dynamic.selectDynamic("$links").asInstanceOf[js.Any]
      if (dollarLinks != null && !js.isUndefined(dollarLinks)) {
        return dollarLinks
      }
    }

    val jsonFieldName = getJsonFieldName(access)
    if (jsonFieldName != access.name) {
      val annotated = dynamic.selectDynamic(jsonFieldName).asInstanceOf[js.Any]
      if (annotated != null && !js.isUndefined(annotated)) {
        return annotated
      }
    }

    dynamic.selectDynamic(access.name).asInstanceOf[js.Any]
  }

  private def getJsonFieldName(access: PropertyAccess[?, ?]): String = {
    access.annotations
      .collectFirst {
        case Annotation(className, params) if className == "com.anjunar.scala.enterprise.macros.validation.JsonName" =>
          params.getOrElse("value", access.name).asInstanceOf[String]
      }
      .getOrElse(access.name)
  }

  private def isIgnored(access: PropertyAccess[?, ?]): Boolean = {
    access.annotations.exists {
      case Annotation(className, _) =>
        className == "jfx.json.JsonIgnore"
      case _ => false
    }
  }

  @tailrec
  private def typeNameFromType(tpe: Type): String = {
    val types = Seq("jfx.core.state.Property", "jfx.core.state.ListProperty")

    tpe match {
      case sc: SimpleClass[?] =>
        sc.typeName
      case pt: ParameterizedType =>
        pt.typeArguments.headOption match {
          case Some(arg) if types.contains(pt.rawType.getTypeName) =>
            typeNameFromType(arg)
          case _ =>
            typeNameFromType(pt.rawType)
        }
      case _ => classOf[Object].getName
    }
  }

  private def deserializeForAccess[M](raw: js.Any, access: PropertyAccess[M, Any], currentValue: Any): Any = {
    val targetTypeName = typeNameFromType(access.genericType)
    deserializeValue(raw, targetTypeName, currentValue)
  }

  private def assign[M](entity: M, access: PropertyAccess[M, Any], decoded: Any): Unit = {
    access.get(entity) match {
      case property: Property[Any @unchecked] =>
        property.set(decoded)

      case list: ListProperty[Any @unchecked] =>
        decoded match {
          case same: ListProperty[Any @unchecked] if same.eq(list) =>

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

  private def deserializeValue(raw: js.Any, targetTypeName: String, currentValue: Any): Any = {
    if (raw == null || js.isUndefined(raw)) {
      null
    } else {
      registry.deserializeValueByTypeName(raw, targetTypeName).getOrElse {
        raw match {
          case array: js.Array[js.Any @unchecked] =>
            val items = array.map((x: js.Any) => deserializeArrayElement(x))
            currentValue match {
              case list: ListProperty[Any @unchecked] =>
                list.setAll(items)
                list
              case _ =>
                items
            }
          case _ => if (canDeserialize(raw.asInstanceOf[Dynamic])) {
            deserialize(raw.asInstanceOf[Dynamic])
          } else {
            raw
          }
        }
      }
    }
  }

  private def deserializeArrayElement(value: js.Any): Any = {
    if (value == null || js.isUndefined(value)) {
      null
    } else {
      val dynamicValue = value.asInstanceOf[Dynamic]
      if (canDeserialize(dynamicValue)) {
        deserialize(dynamicValue)
      } else {
        tryDeserializeAsModel(dynamicValue).getOrElse(value)
      }
    }
  }

  private def tryDeserializeAsModel(dynamic: Dynamic): Option[Any] = {
    val fieldNames = js.Object.keys(dynamic.asInstanceOf[js.Object]).asInstanceOf[js.Array[String]]

    registry.classes.toSeq
      .flatMap { case (_, factory) =>
        try {
          factory() match {
            case model: Model[?] =>
              val props = model.properties
              val matches = props.count(p => fieldNames.contains(getJsonFieldName(p)))
              if (matches > 0) Some((matches, deserializeAsModel(dynamic, model)))
              else None
            case _ =>
              None
          }
        } catch {
          case _: Throwable => None
        }
      }
      .sortBy(-_._1)
      .map(_._2)
      .headOption
  }

  private def deserializeAsModel[M](dynamic: Dynamic, instance: M): M = {
    val props = instance.asInstanceOf[Model[M]].properties.asInstanceOf[Seq[PropertyAccess[M, Any]]]

    props.foreach { access =>
      val raw = readField(dynamic, access)
      if (raw != null && !js.isUndefined(raw)) {
        val currentValue = access.get(instance)
        val decoded = deserializeForAccess(raw, access, currentValue)
        assign(instance, access, decoded)
      }
    }

    instance
  }

  private def canDeserialize(value: Dynamic): Boolean = {
    val rawType = value.selectDynamic("@type")
    rawType != null && !js.isUndefined(rawType) && registry.classes.get(rawType.asInstanceOf[String]).isDefined
  }

  def serialize(model: Model[?]): Dynamic = {
    val out = js.Dictionary[js.Any]()
    out.update("@type", model.getClass.getSimpleName)

    val props = model.properties.asInstanceOf[Seq[PropertyAccess[Any, Any]]]
    props.foreach { access =>
      if (!isIgnored(access)) {
        val value = access.get(model)
        if (value != null) {
          val serialized = serializeValue(value)
          if (serialized != null && !js.isUndefined(serialized)) {
            out.update(getJsonFieldName(access), serialized)
          }
        }
      }
    }

    out.asInstanceOf[js.Dynamic]
  }

  private def serializeValue(value: Any): js.Any = {
    value match {
      case null =>
        null

      case p: ReadOnlyProperty[?] =>
        serializeValue(p.get)

      case m: Model[?] =>
        serialize(m)

      case arr: js.Array[?] =>
        arr.map(serializeValue)

      case v =>
        registry.serializeValue(v).getOrElse(v.asInstanceOf[js.Any])
    }
  }
}
