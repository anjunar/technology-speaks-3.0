package reflect

import jfx.form.Model
import scala.collection.mutable

object ReflectRegistry {
  val factories: mutable.Map[ClassDescriptor, () => Any] = mutable.Map.empty
  val factoriesByTypeName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty

  def registerFactory[T <: Model[T]](descriptor: ClassDescriptor, factory: () => T): Unit = {
    factories.update(descriptor, factory.asInstanceOf[() => Any])
    factoriesByTypeName.update(descriptor.typeName, descriptor)
  }
}
