package com.anjunar.scala.universe.annotations

import java.lang.annotation.Annotation

trait Annotated {

  lazy val declaredAnnotations: Array[Annotation]

  lazy val annotations: Array[Annotation]

  def findAnnotation[A <: Annotation](aClass: Class[A]): A = annotations.find(anno => anno.annotationType() == aClass).orNull.asInstanceOf[A]

  def findDeclaredAnnotation[A <: Annotation](aClass: Class[A]): A = declaredAnnotations.find(anno => anno.annotationType() == aClass).orNull.asInstanceOf[A]

}
