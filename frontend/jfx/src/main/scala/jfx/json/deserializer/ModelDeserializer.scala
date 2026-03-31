package jfx.json.deserializer

import com.anjunar.scala.enterprise.macros.{MetaClassLoader, PropertyAccess}
import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass}
import jfx.core.state.{ListProperty, Property}
import jfx.form.Model

import scala.scalajs.js
import scala.scalajs.js.Dynamic

class ModelDeserializer extends Deserializer[Model[?]] {

  override def deserialize(json: Dynamic, context: JsonContext): Any = {
    val modelType = context.resolvedType match {
      case sc: SimpleClass[?] => sc
      case pt: ParameterizedType =>
        pt.rawType match {
          case sc: SimpleClass[?] => sc
          case _ => throw new IllegalArgumentException(s"Expected SimpleClass, got ${context.resolvedType}")
        }
      case _ => throw new IllegalArgumentException(s"Expected SimpleClass or ParameterizedType, got ${context.resolvedType}")
    }

    val factory = MetaClassLoader.factories.getOrElse(modelType, throw IllegalStateException(s"No factory registered for type '${modelType.typeName}'"))

    val model = factory().asInstanceOf[Model[?]]
    populateModel(model, json, context)
    model
  }

  private def populateModel(model: Model[?], json: Dynamic, parentContext: JsonContext): Unit = {
    model.meta.properties.foreach { property =>
      val fieldName = property.name
      val rawValue = json.selectDynamic(fieldName).asInstanceOf[js.Any]

      if (rawValue != null && !js.isUndefined(rawValue)) {
        val deserializer = DeserializerFactory.buildFromType(property.genericType)
        val elemContext = new JsonContext(property.genericType)
        val decoded = deserializer.deserialize(rawValue.asInstanceOf[Dynamic], elemContext)
        assignValue(model, property.asInstanceOf[PropertyAccess[Any, Any]], decoded)
      }
    }
  }

  private def assignValue(model: Model[?], property: PropertyAccess[Any, Any], decoded: Any): Unit = {
    property.get(model) match {
      case p: Property[Any @unchecked] =>
        p.set(decoded)
      case list: ListProperty[Any @unchecked] =>
        list.clear()
        decoded match {
          case values: js.Array[?] =>
            var i = 0
            while (i < values.length) {
              list.addOne(values(i))
              i += 1
            }
          case values: Iterable[?] =>
            values.foreach(v => list.addOne(v))
          case single =>
            list.addOne(single)
        }
      case _ =>
    }
  }

}
