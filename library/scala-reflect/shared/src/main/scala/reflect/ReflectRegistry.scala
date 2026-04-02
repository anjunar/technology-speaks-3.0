package reflect

import scala.collection.mutable

object ReflectRegistry {

  private val descriptorsByName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty
  private val descriptorsBySimpleName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty
  private val factories: mutable.Map[String, () => Any] = mutable.Map.empty

  inline def register[T](inline factory: () => T): ClassDescriptor = {
    val descriptor = reflect.macros.ReflectMacros.reflectWithAccessors[T]
    factories += descriptor.typeName -> (factory.asInstanceOf[() => Any])
    registerWithAccessors(descriptor)
    descriptor
  }

  def registerWithAccessors[T](descriptor: ClassDescriptor): Unit = {
    val typeName = descriptor.typeName
    descriptorsByName += typeName -> descriptor

    val simpleName = descriptor.simpleName
    if !descriptorsBySimpleName.contains(simpleName) then
      descriptorsBySimpleName += simpleName -> descriptor
  }

  def registerByTypeName(typeName: String, descriptor: ClassDescriptor, factory: Option[() => Any] = None): Unit = {
    descriptorsByName += typeName -> descriptor

    val simpleName = descriptor.simpleName
    if !descriptorsBySimpleName.contains(simpleName) then
      descriptorsBySimpleName += simpleName -> descriptor

    factory.foreach { f =>
      factories += typeName -> f
    }
  }

  def loadClass(typeName: String): Option[ClassDescriptor] =
    descriptorsByName.get(typeName).orElse(descriptorsBySimpleName.get(typeName))

  def loadClassBySimpleName(simpleName: String): Option[ClassDescriptor] =
    descriptorsBySimpleName.get(simpleName)

  def createInstance(typeName: String): Option[Any] =
    factories.get(typeName).map(_())

  def getPropertyAccessor(typeName: String, propertyName: String): Option[PropertyAccessor[Any, Any]] =
    descriptorsByName.get(typeName).flatMap(_.properties.find(_.name == propertyName).flatMap(_.accessor))

  def isAssignableFrom(subType: String, superType: String): Boolean = {
    if subType == superType then return true

    loadClass(subType) match {
      case Some(descriptor) =>
        if descriptor.baseTypes.contains(superType) then true
        else descriptor.baseTypes.exists(bt => isAssignableFrom(bt, superType))
      case None => false
    }
  }

  def getSubTypes(superType: String): List[ClassDescriptor] =
    descriptorsByName.values.filter(_.baseTypes.contains(superType)).toList

  def getAllRegistered: Iterable[ClassDescriptor] = descriptorsByName.values

  def contains(typeName: String): Boolean = descriptorsByName.contains(typeName)

  def clear(): Unit = {
    descriptorsByName.clear()
    descriptorsBySimpleName.clear()
    factories.clear()
  }

}
