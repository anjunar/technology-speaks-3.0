package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}
import com.google.common.reflect.TypeToken

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Member}

class ResolvedField(override val underlying: Field, owner : ResolvedClass) extends ResolvedMember(underlying, owner) {
  
  lazy val name : String = underlying.getName
  
  lazy val fieldType : ResolvedClass = TypeResolver.resolve(TypeToken.of(owner.underlying).resolveType(underlying.getGenericType).getType)
  
  lazy val hidden : Array[ResolvedField] = owner.hierarchy.drop(1).flatMap(aClass => aClass.fields.filter(field => field.name == name))
  
  def get(instance : AnyRef) : Any = {
    underlying.setAccessible(true)
    underlying.get(instance)
  }
  
  def set(instance : AnyRef, value : Any) : Unit = {
    underlying.setAccessible(true)
    underlying.set(instance, value)
  }

  override lazy val declaredAnnotations: Array[Annotation] = underlying.getDeclaredAnnotations
  
  override lazy val annotations: Array[Annotation] = declaredAnnotations ++ hidden.flatMap(field => field.declaredAnnotations)
  
  override def toString = s"ResolvedField($name, $fieldType)"
}
