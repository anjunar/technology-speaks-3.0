package jfx.core.meta

import reflect.*
import reflect.macros.ReflectMacros
import scala.collection.mutable

class PackageClassLoader(val packageName: String, parent: ReflectClassLoader) {

  private val loader: ReflectClassLoader = ReflectClassLoader.createWithParent(parent)

  inline def register[T](inline factory: () => T): ClassDescriptor = {
    val descriptor = ReflectMacros.reflectWithAccessors[T]
    loader.register[T](descriptor, factory)
    // Also register in ReflectRegistry for global access
    reflect.ReflectRegistry.registerByTypeName(descriptor.typeName, descriptor, Some(factory.asInstanceOf[() => Any]))
    // Also register @JsonType annotation for polymorphic deserialization
    jfx.json.JsonTypeRegistry.register(descriptor)
    descriptor
  }

  def registerByDescriptor(descriptor: ClassDescriptor, factory: Option[() => Any] = None): Unit = {
    loader.registerByTypeName(descriptor.typeName, descriptor, factory)
  }

  def loadClass(typeName: String): Option[ClassDescriptor] =
    loader.loadClass(typeName)

  def createInstance[T](typeName: String): Option[T] =
    loader.createInstanceAs[T](typeName)

  def getAllRegistered: List[ClassDescriptor] =
    loader.getAllRegistered.toList

  def getSubTypes(superType: String): List[ClassDescriptor] =
    loader.getSubTypes(superType)
}

object PackageClassLoader {

  private val packageLoaders: mutable.Map[String, PackageClassLoader] = mutable.Map.empty

  def apply(packageName: String): PackageClassLoader = {
    packageLoaders.getOrElseUpdate(
      packageName,
      new PackageClassLoader(packageName, Reflect.classLoader)
    )
  }

  def domains: PackageClassLoader = apply("app.domain")
  def components: PackageClassLoader = apply("app.components")
  def pages: PackageClassLoader = apply("app.pages")
  def services: PackageClassLoader = apply("app.services")
}
