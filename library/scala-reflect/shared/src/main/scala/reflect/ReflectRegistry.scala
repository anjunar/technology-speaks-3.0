package reflect

import reflect.macros.PropertySupport
import scala.collection.mutable

object ReflectRegistry {

  private val descriptorsByName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty
  private val descriptorsBySimpleName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty
  private val factories: mutable.Map[String, () => Any] = mutable.Map.empty
  private val propertyAccessors: mutable.Map[String, Map[String, PropertyAccessor[Any, Any]]] = mutable.Map.empty

  inline def register[T](inline factory: () => T): ClassDescriptor = {
    val descriptor = reflect.macros.ReflectMacros.reflect[T]
    val props = PropertySupport.extractPropertiesWithAccessors[T]
    val accessors = props.map(p => p.name -> p.accessor.asInstanceOf[PropertyAccessor[T, Any]]).toMap
    registerWithAccessors(descriptor, factory, accessors)
    descriptor
  }

  def registerWithAccessors[T](descriptor: ClassDescriptor, factory: () => T, accessors: Map[String, PropertyAccessor[T, Any]]): Unit = {
    val typeName = descriptor.typeName
    descriptorsByName += typeName -> descriptor

    val simpleName = descriptor.simpleName
    if !descriptorsBySimpleName.contains(simpleName) then
      descriptorsBySimpleName += simpleName -> descriptor

    factories += typeName -> (factory.asInstanceOf[() => Any])
    propertyAccessors += typeName -> accessors.asInstanceOf[Map[String, PropertyAccessor[Any, Any]]]
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
    propertyAccessors.get(typeName).flatMap(_.get(propertyName))

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
    propertyAccessors.clear()
  }

}
