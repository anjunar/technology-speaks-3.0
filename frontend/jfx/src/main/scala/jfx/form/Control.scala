package jfx.form

import jfx.core.component.ElementComponent
import jfx.core.state.{ListProperty, Property, ReadOnlyProperty}
import org.scalajs.dom.HTMLElement

trait Control[V, E <: HTMLElement] extends ElementComponent[E] {
  
  val name : String
  
  val standalone : Boolean
  
  val placeholderProperty: Property[String] = Property("")

  val editableProperty: Property[Boolean] = Property(true)

  val focusedProperty: Property[Boolean] = Property(false)

  val dirtyProperty: Property[Boolean] = Property(false)

  val errorsProperty: ListProperty[String] = new ListProperty[String]()

  val invalidProperty: ReadOnlyProperty[Boolean] =
    errorsProperty.map(_.length > 0)

  val valueProperty : ReadOnlyProperty[V]

  def placeholder: String = placeholderProperty.get
  def placeholder_=(value: String): Unit = placeholderProperty.set(value)

  def editable: Boolean = editableProperty.get
  def editable_=(value: Boolean): Unit = editableProperty.set(value)

  def setFocused(value: Boolean): Unit =
    focusedProperty.set(value)

  def setDirty(value: Boolean): Unit =
    dirtyProperty.set(value)

  def setErrors(values: IterableOnce[String]): Unit =
    errorsProperty.setAll(values)

}

object Control {

  def placeholder[V, E <: HTMLElement](using control: Control[V, E]): String =
    control.placeholder

  def placeholder_=[V, E <: HTMLElement](value: String)(using control: Control[V, E]): Unit =
    control.placeholder = value

  def value[V, E <: HTMLElement](using control: Control[V, E]): V =
    control.valueProperty.get

  def value_=[V, E <: HTMLElement](nextValue: V)(using control: Control[V, E]): Unit =
    valueProperty[V, E].set(nextValue)

  def valueProperty[V, E <: HTMLElement](using control: Control[V, E]): Property[V] =
    control.valueProperty.asInstanceOf[Property[V]]

  def editable[V, E <: HTMLElement](using control: Control[V, E]): Boolean =
    control.editable

  def editable_=[V, E <: HTMLElement](value: Boolean)(using control: Control[V, E]): Unit =
    control.editable = value

  def editableProperty[V, E <: HTMLElement](using control: Control[V, E]): Property[Boolean] =
    control.editableProperty
}
