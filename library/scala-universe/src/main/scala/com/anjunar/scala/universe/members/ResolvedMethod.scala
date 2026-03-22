package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}
import com.google.common.reflect.TypeToken

import java.lang.annotation.Annotation
import java.lang.reflect.Method

class ResolvedMethod(override val underlying : Method, owner : ResolvedClass) extends ResolvedExecutable(underlying, owner) {

  lazy val name : String = underlying.getName
  
  lazy val overrides : Array[ResolvedMethod] = owner.hierarchy.drop(1)
    .filter(aClass => try
      aClass.findMethod(name, underlying.getParameterTypes *) != null
    catch
      case e: NoSuchMethodException => false
    )
    .map(aClass => aClass.findMethod(name, underlying.getParameterTypes*))
  
  lazy val returnType : ResolvedClass = TypeResolver.resolve(TypeToken.of(owner.underlying).resolveType(underlying.getGenericReturnType).getType)
  
  def invoke(instance : AnyRef, args : Any*) : Any = {
    underlying.setAccessible(true)
    underlying.invoke(instance, args*)
  }
  
  override lazy val declaredAnnotations: Array[Annotation] = underlying.getDeclaredAnnotations
  
  override lazy val annotations: Array[Annotation] = declaredAnnotations ++ overrides.flatMap(method => method.declaredAnnotations)
  
  override def toString = s"ResolvedMethod($name, $returnType, ${parameters.mkString(", ")})"
}
