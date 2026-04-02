package reflect

import scala.compiletime.uninitialized

final case class ClassDescriptor(
  typeName: String,
  simpleName: String,
  annotations: Array[Annotation],
  properties: Array[PropertyDescriptor],
  baseTypes: Array[String],
  typeParameters: Array[String],
  constructors: Array[ConstructorDescriptor],
  isAbstract: Boolean,
  isFinal: Boolean,
  isCaseClass: Boolean
) extends TypeDescriptor {

  var runtimeClass : Class[?] = uninitialized
  
  lazy val propertyNames: Array[String] = properties.map(_.name)
  
  def getProperty(name: String): Option[PropertyDescriptor] =
    properties.find(_.name == name)
  
  def getWriteableProperties: Array[PropertyDescriptor] =
    properties.filter(_.isWriteable)
  
  def getReadableProperties: Array[PropertyDescriptor] =
    properties.filter(_.isReadable)
  
  def hasProperty(name: String): Boolean = propertyNames.contains(name)
  
  def isSubTypeOf(otherTypeName: String): Boolean =
    baseTypes.contains(otherTypeName)
}
