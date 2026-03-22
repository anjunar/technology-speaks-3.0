package com.anjunar.scala.universe.annotations

import java.lang.annotation.Annotation

case class ResolvedAnnotation[A <: Annotation](annotation: A, target : Annotated) 
