package com.anjunar.json.mapper.macros

import java.lang.annotation.Annotation
import java.lang.reflect.{InvocationHandler, Method, Proxy, Type as JType}
import scala.jdk.CollectionConverters.*

object RuntimeAnnotationSupport {

  final case class AnnotationValue(name: String, value: Any)

  final case class AnnotationDescriptor(annotationClassName: String, values: Array[AnnotationValue])

  def instantiateAll(descriptors: Array[AnnotationDescriptor]): Array[? <: Annotation] =
    descriptors.map(instantiate)

  def instantiate(descriptor: AnnotationDescriptor): Annotation = {
    val annotationClass =
      Class.forName(descriptor.annotationClassName).asInstanceOf[Class[? <: Annotation]]

    val valuesMap = descriptor.values.iterator.map(v => v.name -> v.value).toMap

    val handler = new InvocationHandler {
      override def invoke(proxy: Any, method: Method, args: Array[AnyRef] | Null): AnyRef = {
        method.getName match {
          case "annotationType" =>
            annotationClass

          case "toString" =>
            s"@${annotationClass.getName}(${valuesMap.mkString(", ")})"

          case "hashCode" =>
            Int.box(valuesMap.hashCode() * 31 + annotationClass.hashCode())

          case "equals" =>
            val other = if args == null || args.length == 0 then null else args(0)
            if other == null then Boolean.box(false)
            else if !annotationClass.isInstance(other) then Boolean.box(false)
            else {
              val same = annotationClass.getDeclaredMethods.forall { m =>
                val thisValue =
                  valuesMap.getOrElse(m.getName, m.getDefaultValue)
                val otherValue = m.invoke(other)
                java.util.Objects.deepEquals(thisValue.asInstanceOf[AnyRef], otherValue)
              }
              Boolean.box(same)
            }

          case memberName =>
            val value =
              valuesMap.getOrElse(memberName, method.getDefaultValue)

            if value == null then null
            else value.asInstanceOf[AnyRef]
        }
      }
    }

    Proxy
      .newProxyInstance(annotationClass.getClassLoader, Array(annotationClass), handler)
      .asInstanceOf[Annotation]
  }
}