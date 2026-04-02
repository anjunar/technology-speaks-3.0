package jfx.control.cell

import reflect.macros.PropertySupport
import jfx.control.TableColumn
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
        val allProps = PropertySupport.extractPropertiesWithAccessors[Model[?]]
        val prop = allProps.find(_.name == property)
        prop match {
          case Some(p) => wrapValue(p.accessor.asInstanceOf[reflect.PropertyAccessor[Any, Any]].get(model.asInstanceOf[Any]))
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
