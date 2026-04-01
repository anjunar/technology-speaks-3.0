package reflect

class ReflectClassLoader {
  
  def loadClass(typeName: String): Option[ClassDescriptor] =
    ReflectRegistry.loadClass(typeName)
  
  def loadClass[T](using Manifest[T]): Option[ClassDescriptor] =
    ReflectRegistry.loadClass(manifest[T].runtimeClass.getName)
  
  def createInstance(typeName: String): Option[Any] =
    ReflectRegistry.createInstance(typeName)
  
  def createInstanceAs[T](typeName: String): Option[T] =
    ReflectRegistry.createInstance(typeName).map(_.asInstanceOf[T])
  
  def isAssignableFrom(subType: String, superType: String): Boolean =
    ReflectRegistry.isAssignableFrom(subType, superType)
  
  def getSubTypes(superType: String): List[ClassDescriptor] =
    ReflectRegistry.getSubTypes(superType)
}

object ReflectClassLoader {
  
  private val defaultInstance = new ReflectClassLoader()
  
  def default: ReflectClassLoader = defaultInstance
}
