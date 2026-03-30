package com.anjunar.scala.enterprise.macros

final case class Annotation(
  annotationClass: Class[?],
  annotations: Map[String, Any] = Map.empty
)
