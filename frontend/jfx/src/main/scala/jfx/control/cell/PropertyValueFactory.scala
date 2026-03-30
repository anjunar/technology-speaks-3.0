package jfx.control.cell

import jfx.control.TableColumn
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{Property, ReadOnlyProperty}
import jfx.form.Model

class PropertyValueFactory[S, T](val property: String)
  extends (TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null) {

  override def apply(features: TableColumn.CellDataFeatures[S, T]): ReadOnlyProperty[T] | Null = {
    val rowValue = features.getValue
    if (rowValue == null) null
    else resolveValue(rowValue)
  }

  private def resolveValue(rowValue: S): ReadOnlyProperty[T] | Null =
    rowValue match {
      case model: Model[?] =>
        val typedModel = model.asInstanceOf[Model[Any]]
        val access = typedModel.properties.find(_.name == property)
        access match {
          case Some(a) => wrapValue(a.asInstanceOf[PropertyAccess[Any, Any]].get(rowValue.asInstanceOf[Any]))
          case None => null
        }

      case _ => null
    }

  private def wrapValue(value: Any): ReadOnlyProperty[T] | Null =
    value match {
      case property: ReadOnlyProperty[?] =>
        property.asInstanceOf[ReadOnlyProperty[T]]

      case null =>
        Property(null.asInstanceOf[T])

      case other =>
        Property(other.asInstanceOf[T])
    }
}
