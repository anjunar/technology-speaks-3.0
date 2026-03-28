package jfx.core.state

import jfx.form.validators.Validator

import scala.reflect.ClassTag

trait PropertyAccess[T, V] {
  val name: String
  val classTag: ClassTag[V]
  def propertyType: Class[?] = classTag.runtimeClass
  def valueType: Class[?] | Null = null
  def get(obj: T): Option[V]
  def set(obj: T, value: V): Unit

  def validators: Vector[Validator[Any]] = Vector.empty

  def withValidator[Value](validator: Validator[Value]): PropertyAccess[T, V] = {
    val delegate = this
    new PropertyAccess[T, V] {
      override val name: String = delegate.name
      override val classTag: ClassTag[V] = delegate.classTag
      override def propertyType: Class[?] = delegate.propertyType
      override def valueType: Class[?] | Null = delegate.valueType
      override def get(obj: T): Option[V] = delegate.get(obj)
      override def set(obj: T, value: V): Unit = delegate.set(obj, value)
      override val validators: Vector[Validator[Any]] =
        delegate.validators :+ validator.asInstanceOf[Validator[Any]]
    }
  }
}
