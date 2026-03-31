package jfx.json

import com.anjunar.scala.enterprise.macros.{Annotation, PropertyAccess}
import jfx.core.meta.ClassLoader
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class JsonMapper(val registry: JsonRegistry) {

  def deserialize[M <: Model[M]](dynamic: Dynamic): M = {
    val entityType = dynamic.selectDynamic("@type").asInstanceOf[String]
    if (entityType == null || js.isUndefined(entityType)) return null.asInstanceOf[M]

    val factory = findFactory(entityType)
      .getOrElse(throw IllegalStateException(s"No factory for '@type' = '$entityType'"))
      .asInstanceOf[() => M]

    deserialize(dynamic, factory)
  }

  private def findFactory(entityType: String): Option[() => Any] = {
    registry.classes.get(entityType).orElse {
      ClassLoader.getFactory(entityType)
    }
  }

  def deserialize[M <: Model[M]](dynamic: Dynamic, clazz: Class[M]): M = {
    registry.deserializeValueByTypeName(dynamic, clazz.getName).map(_.asInstanceOf[M]).getOrElse {
      val factory = registry.classes
        .get(clazz.getSimpleName)
        .orElse {
          ClassLoader.getFactory(clazz.getSimpleName)
        }
        .getOrElse(throw IllegalStateException(s"No factory for class = '${clazz.getName}'"))
        .asInstanceOf[() => M]

      deserialize(dynamic, factory)
    }
  }

  def deserializeArray[M <: Model[M]](dynamic: Dynamic, clazz: Class[M]): Seq[M] = {
    if (dynamic == null || js.isUndefined(dynamic) || !dynamic.isInstanceOf[js.Array[?]]) return Seq.empty
    dynamic.asInstanceOf[js.Array[Dynamic]].toSeq.map(d => deserialize(d, clazz))
  }

  def deserialize[M <: Model[M]](dynamic: Dynamic, factory: () => M): M = {
    val entity = factory()
    entity.meta.properties.foreach { (access : PropertyAccess[M, ?]) =>
      if (!isIgnored(access)) {
        val raw = readField(dynamic, access)
        if (raw != null && !js.isUndefined(raw)) {
          assign(entity, access.asInstanceOf[PropertyAccess[M, Any]], deserializeValue(raw, access.asInstanceOf[PropertyAccess[M, Any]]))
        }
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
    val name = access.name
    name == "meta" || name == "##" || name.startsWith("$") || access.annotations.exists {
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
        val elementTypeName = extractElementTypeName(access)
        raw.asInstanceOf[js.Array[js.Any]].map(v => deserializeArrayElement(v, elementTypeName))
      } else if (canDeserialize(raw.asInstanceOf[Dynamic])) {
        deserialize(raw.asInstanceOf[Dynamic])
      } else {
        raw
      }
    }
  }

  private def extractElementTypeName(access: PropertyAccess[?, Any]): String = {
    extractTypeName(access.genericType) match {
      case "jfx.core.state.ListProperty" | "jfx.core.state.Property" =>
        access.genericType match {
          case pt: com.anjunar.scala.enterprise.macros.reflection.ParameterizedType =>
            pt.typeArguments.headOption.map(extractTypeName).getOrElse(classOf[Object].getName)
          case _ => classOf[Object].getName
        }
      case other => other
    }
  }

  private def extractTypeName(tpe: com.anjunar.scala.enterprise.macros.reflection.Type): String = {
    tpe match {
      case sc: com.anjunar.scala.enterprise.macros.reflection.SimpleClass[?] =>
        sc.typeName
      case pt: com.anjunar.scala.enterprise.macros.reflection.ParameterizedType =>
        val rawTypeName = extractTypeName(pt.rawType)
        rawTypeName match {
          case "jfx.core.state.Property" | "jfx.core.state.ListProperty" =>
            pt.typeArguments.headOption.map(extractTypeName).getOrElse(classOf[Object].getName)
          case _ =>
            rawTypeName
        }
      case _ => classOf[Object].getName
    }
  }

  private def deserializeArrayElement(value: js.Any, elementTypeName: String): Any = {
    if (value == null || js.isUndefined(value)) null
    else {
      val dynamicValue = value.asInstanceOf[Dynamic]
      if (canDeserialize(dynamicValue)) deserialize(dynamicValue)
      else {
        val factory = registry.classes.get(elementTypeName).orElse {
          ClassLoader.getFactory(elementTypeName)
        }
        factory match {
          case Some(factory) =>
            factory() match {
              case model: Model[?] =>
                deserializeAsModel(dynamicValue, model)
              case _ => value
            }
          case None =>
            tryDeserializeAsModel(dynamicValue).getOrElse(value)
        }
      }
    }
  }

  private def tryDeserializeAsModel(dynamic: Dynamic): Option[Any] = {
    val fieldNames = js.Object.keys(dynamic.asInstanceOf[js.Object]).asInstanceOf[js.Array[String]]

    val registryResults = registry.classes.toSeq
      .flatMap { case (_, factory) =>
        try {
          factory() match {
            case model: Model[?] =>
              val props = model.meta.properties
              val matches = props.count(p => fieldNames.contains(getJsonFieldName(p)))
              if (matches > 0) Some((matches, deserializeAsModel(dynamic, model)))
              else None
            case _ => None
          }
        } catch {
          case _: Throwable => None
        }
      }

    val classLoaderResults = ClassLoader.classes.toSeq
      .flatMap { case (_, (clazz, factory)) =>
        try {
          factory() match {
            case model: Model[?] =>
              val props = model.meta.properties
              val matches = props.count(p => fieldNames.contains(getJsonFieldName(p)))
              if (matches > 0) Some((matches, deserializeAsModel(dynamic, model)))
              else None
            case _ => None
          }
        } catch {
          case _: Throwable => None
        }
      }

    (registryResults ++ classLoaderResults).sortBy(-_._1).map(_._2).headOption
  }

  private def deserializeAsModel[M](dynamic: Dynamic, instance: M): M = {
    val props = instance.asInstanceOf[Model[M]].meta.properties
    props.foreach { access =>
      if (!isIgnored(access)) {
        val raw = readField(dynamic, access)
        if (raw != null && !js.isUndefined(raw)) {
          assign(instance, access.asInstanceOf[PropertyAccess[M, Any]], deserializeValue(raw, access.asInstanceOf[PropertyAccess[?, Any]]))
        }
      }
    }
    instance
  }

  private def canDeserialize(value: Dynamic): Boolean = {
    val rawType = value.selectDynamic("@type")
    rawType != null && !js.isUndefined(rawType) && (
      registry.classes.get(rawType.asInstanceOf[String]).isDefined ||
      ClassLoader.containsTypeName(rawType.asInstanceOf[String])
    )
  }

  def serialize(model: Model[?]): Dynamic = {
    val out = js.Dictionary[js.Any]()
    val typeName = ClassLoader.classes.toSeq
      .collectFirst { case (name, (clazz, _)) if clazz == model.getClass => name }
      .getOrElse(model.getClass.getSimpleName)
    out.update("@type", typeName)

    model.meta.properties.foreach { access =>
      if (!isIgnored(access)) {
        val value = access.asInstanceOf[PropertyAccess[Any, Any]].get(model)
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
