package reflect

final case class ClassDescriptor(
  typeName: String,
  simpleName: String,
  annotations: Array[Annotation],
  properties: Array[PropertyDescriptor],
  baseTypes: Array[String],
  typeParameters: Array[TypeDescriptor],
  constructors: Array[ConstructorDescriptor],
  isAbstract: Boolean,
  isFinal: Boolean,
  isCaseClass: Boolean
) extends TypeDescriptor {

  private var boundRuntimeClass: Option[Class[?]] = None
  private var boundFactory: Option[() => Any] = None

  def runtimeClass: Option[Class[?]] = boundRuntimeClass
  def factory: Option[() => Any] = boundFactory

  def bindRuntimeClass(value: Class[?]): ClassDescriptor = {
    boundRuntimeClass = Option(value)
    this
  }

  def bindFactory(value: () => Any): ClassDescriptor = {
    boundFactory = Option(value)
    this
  }

  def requireRuntimeClass: Class[?] =
    boundRuntimeClass.getOrElse(throw new IllegalStateException(s"Runtime class not bound for $typeName"))

  def createInstance(): Option[Any] =
    boundFactory.map(_())
  
  lazy val propertyNames: Array[String] = properties.map(_.name)
  lazy val typeParameterNames: Array[String] =
    typeParameters.collect { case variable: TypeVariableDescriptor => variable.name }
  
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
