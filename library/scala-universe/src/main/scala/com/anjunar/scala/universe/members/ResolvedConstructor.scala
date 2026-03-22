package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.ResolvedClass

import java.lang.annotation.Annotation
import java.lang.reflect.Constructor

class ResolvedConstructor(override val underlying : Constructor[?], owner : ResolvedClass) extends ResolvedExecutable(underlying, owner) {

  override lazy val declaredAnnotations: Array[Annotation] = underlying.getDeclaredAnnotations
  
  override lazy val annotations: Array[Annotation] = underlying.getDeclaredAnnotations
  
  def newInstance(args : Any*) = underlying.newInstance(args*)

  override def toString = s"ResolvedConstructor(${parameters.mkString(", ")})"
}
