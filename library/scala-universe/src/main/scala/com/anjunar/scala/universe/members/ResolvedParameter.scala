package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.annotations.Annotated
import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}
import com.google.common.reflect.TypeToken

import java.lang.annotation.Annotation
import java.lang.reflect.Parameter

class ResolvedParameter(val underlying : Parameter, owner : ResolvedExecutable) extends Annotated {
  
  lazy val name : String = underlying.getName
  
  lazy val parameterType : ResolvedClass = TypeResolver.resolve(TypeToken.of(owner.owner.underlying).resolveType(underlying.getParameterizedType).getType)
  
  override lazy val declaredAnnotations: Array[Annotation] = underlying.getDeclaredAnnotations

  override lazy val annotations: Array[Annotation] = owner match
    case method : ResolvedMethod => declaredAnnotations ++ method.overrides.flatMap(method => method.parameters(owner.parameters.indexOf(this)).declaredAnnotations)
    case _ => Array()


  override def toString = s"ResolvedParameter($name, $parameterType)"
}
