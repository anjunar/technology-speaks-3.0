package reflect

object ReflectionSupport {
  
  def isAssignableFrom(subTypeDescriptor: ClassDescriptor, superTypeDescriptor: ClassDescriptor): Boolean =
    subTypeDescriptor.typeName == superTypeDescriptor.typeName ||
    subTypeDescriptor.baseTypes.contains(superTypeDescriptor.typeName) ||
    subTypeDescriptor.baseTypes.exists { bt =>
      ReflectRegistry.loadClass(bt).exists { baseDesc =>
        isAssignableFrom(baseDesc, superTypeDescriptor)
      }
    }
}
