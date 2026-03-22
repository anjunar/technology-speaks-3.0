package jfx.core.state

import scala.reflect.ClassTag

trait PropertyAccess[T, V] {
  val name: String
  val classTag: ClassTag[V]
  def propertyType: Class[?] = classTag.runtimeClass
  def valueType: Class[?] | Null = null
  def get(obj: T): Option[V]
  def set(obj: T, value: V): Unit
}
