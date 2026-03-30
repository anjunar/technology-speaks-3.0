package com.anjunar.scala.enterprise.macros

final case class Annotation(
  annotationClassName: String,
  parameters: Map[String, Any] = Map.empty
)
