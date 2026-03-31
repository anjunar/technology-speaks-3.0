package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, PropertyAccess}
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class JsonMapper(val registry: JsonRegistry) {

  def deserialize[M <: Model[M]](dynamic: Dynamic): M = {
    val entityType = dynamic.selectDynamic("@type").asInstanceOf[String]
    if (entityType == null || js.isUndefined(entityType)) return null.asInstanceOf[M]

    val factory = registry.classes
      .get(entityType)
      .getOrElse(throw IllegalStateException(s"No factory for '@type' = '$entityType'"))
      .asInstanceOf[() => M]

    deserialize(dynamic, factory)
  }

  def deserialize[M <: Model[M]](dynamic: Dynamic, factory: () => M): M = {
    val entity = factory()
    entity.properties.asInstanceOf[Seq[PropertyAccess[M, Any]]].foreach { access =>
      val raw = readField(dynamic, access)
      if (raw != null && !js.isUndefined(raw)) {
        assign(entity, access, deserializeValue(raw, access))
      }
    }
    entity
  }

  private def readField(dynamic: Dynamic, access: PropertyAccess[?, ?]): js.Any = {
    if (access.name == "links") {
      val dollarLinks = dynamic.selectDynamic("$links").asInstanceOf[js.Any]
      if (dollarLinks != null && !js.isUndefined(dollarLinks)) return dollarLinks
    }

    val jsonName = getJsonFieldName(access)
    if (jsonName != access.name) {
      val annotated = dynamic.selectDynamic(jsonName).asInstanceOf[js.Any]
      if (annotated != null && !js.isUndefined(annotated)) return annotated
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
      case Annotation(className, _) => className == "jfx.json.JsonIgnore"
      case null => false
    }
  }

  private def assign[M](entity: M, access: PropertyAccess[M, Any], decoded: Any): Unit = {
    access.get(entity) match {
      case prop: Property[Any @unchecked] =>
        prop.set(decoded)

      case list: ListProperty[Any @unchecked] =>
        decoded match {
          case same: ListProperty[Any @unchecked] if same eq list =>
          case array: js.Array[?] => list.setAll(array.asInstanceOf[js.Array[Any]])
          case iterable: IterableOnce[?] => list.setAll(iterable.iterator)
          case null => list.clear()
          case other => list.setAll(Seq(other))
        }

      case _ =>
        access.set(entity, decoded)
    }
  }

  private def deserializeValue(raw: js.Any, access: PropertyAccess[?, Any]): Any = {
    if (raw == null || js.isUndefined(raw)) return null

    val typeName = extractTypeName(access.genericType)

    registry.deserializeValueByTypeName(raw, typeName).getOrElse {
      if (raw.isInstanceOf[js.Array[js.Any]]) {
        raw.asInstanceOf[js.Array[js.Any]].map(deserializeArrayElement)
      } else if (canDeserialize(raw.asInstanceOf[Dynamic])) {
        deserialize(raw.asInstanceOf[Dynamic])
      } else {
        raw
      }
    }
  }

  private def extractTypeName(tpe: com.anjunar.scala.enterprise.macros.reflection.Type): String = {
    tpe match {
      case sc: com.anjunar.scala.enterprise.macros.reflection.SimpleClass[?] =>
        sc.typeName
      case pt: com.anjunar.scala.enterprise.macros.reflection.ParameterizedType =>
        pt.rawType.getTypeName match {
          case "jfx.core.state.Property" | "jfx.core.state.ListProperty" =>
            pt.typeArguments.headOption.map(extractTypeName).getOrElse(classOf[Object].getName)
          case _ =>
            pt.rawType.getTypeName
        }
      case _ => classOf[Object].getName
    }
  }

  private def deserializeArrayElement(value: js.Any): Any = {
    if (value == null || js.isUndefined(value)) null
    else {
      val dynamicValue = value.asInstanceOf[Dynamic]
      if (canDeserialize(dynamicValue)) deserialize(dynamicValue)
      else tryDeserializeAsModel(dynamicValue).getOrElse(value)
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
            case _ => None
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
        assign(instance, access, deserializeValue(raw, access))
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

    model.properties.asInstanceOf[Seq[PropertyAccess[Any, Any]]].foreach { access =>
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
      case null => null
      case p: ReadOnlyProperty[?] => serializeValue(p.get)
      case m: Model[?] => serialize(m)
      case arr: js.Array[?] => arr.map(serializeValue)
      case v => registry.serializeValue(v).getOrElse(v.asInstanceOf[js.Any])
    }
  }
}
