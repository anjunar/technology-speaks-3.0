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

  def newInstance(): Option[Any] =
    createInstance().orElse(ClassDescriptor.maybeResolve(this).flatMap(_.createInstance()))

  def requireCreateInstance(): Any =
    newInstance().getOrElse(throw new IllegalArgumentException(s"Cannot instantiate $typeName"))

  def resolved: ClassDescriptor =
    ClassDescriptor.resolve(this)
  
  lazy val propertyNames: Array[String] = properties.map(_.name)
  lazy val typeParameterNames: Array[String] =
    typeParameters.collect { case variable: TypeVariableDescriptor => variable.name }
  
  def getProperty(name: String): Option[PropertyDescriptor] =
    properties.find(_.name == name)

  def getPropertyAccessor(name: String): Option[PropertyAccessor[Any, Any]] =
    getProperty(name).flatMap(_.accessor)
      .orElse(resolved.getProperty(name).flatMap(_.accessor))

  def requirePropertyAccessor(name: String): PropertyAccessor[Any, Any] =
    getPropertyAccessor(name).getOrElse(
      throw new IllegalArgumentException(s"Missing accessor for $typeName.$name")
    )
  
  def getWriteableProperties: Array[PropertyDescriptor] =
    properties.filter(_.isWriteable)
  
  def getReadableProperties: Array[PropertyDescriptor] =
    properties.filter(_.isReadable)
  
  def hasProperty(name: String): Boolean = propertyNames.contains(name)
  
  def isSubTypeOf(otherTypeName: String): Boolean =
    baseTypes.contains(otherTypeName) || ClassDescriptor.isAssignableFrom(typeName, otherTypeName)

  def isAssignableTo(otherTypeName: String): Boolean =
    ClassDescriptor.isAssignableFrom(typeName, otherTypeName)
}

object ClassDescriptor {

  def forName(typeName: String): ClassDescriptor =
    ReflectRegistry.loadClass(typeName).getOrElse(
      throw new IllegalArgumentException(s"Cannot load class descriptor: $typeName")
    )

  def maybeForName(typeName: String): Option[ClassDescriptor] =
    ReflectRegistry.loadClass(typeName)

  def forSimpleName(simpleName: String): ClassDescriptor =
    maybeForSimpleName(simpleName).getOrElse(
      throw new IllegalArgumentException(s"Cannot load class descriptor by simple name: $simpleName")
    )

  def maybeForSimpleName(simpleName: String): Option[ClassDescriptor] =
    ReflectRegistry.loadClassBySimpleName(simpleName)

  def resolve(descriptor: ClassDescriptor): ClassDescriptor =
    maybeResolve(descriptor).getOrElse(
      throw new IllegalArgumentException(s"Missing class descriptor for ${descriptor.typeName}")
    )

  def maybeResolve(descriptor: ClassDescriptor): Option[ClassDescriptor] =
    maybeForName(descriptor.typeName)
      .orElse(maybeForName(descriptor.simpleName))
      .orElse(Option.when(descriptor.properties.nonEmpty || descriptor.typeParameters.nonEmpty)(descriptor))

  def all: Iterable[ClassDescriptor] =
    ReflectRegistry.getAllRegistered

  def subTypesOf(superTypeName: String): List[ClassDescriptor] =
    ReflectRegistry.getSubTypes(superTypeName)

  def isAssignableFrom(subType: String, superType: String): Boolean =
    ReflectRegistry.isAssignableFrom(subType, superType)
}
