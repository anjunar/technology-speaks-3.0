package com.anjunar.scala.universe

import com.anjunar.scala.universe.annotations.ResolvedAnnotation
import com.google.common.reflect.ClassPath
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.spi.{AfterDeploymentValidation, Extension, ProcessAnnotatedType}

import java.lang.annotation.Annotation
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

class ClassPathResolver extends Extension {

  val collectedClasses: ListBuffer[ResolvedClass] = ListBuffer.empty

  def collectAllClasses(@Observes pat: ProcessAnnotatedType[?]): Unit = {
    val clazz: Class[?] = pat.getAnnotatedType.getJavaClass
    collectedClasses.addOne(TypeResolver.resolve(clazz))
  }

  def afterDeployment(@Observes adv: AfterDeploymentValidation): Unit = {
    ClassPathResolver.annotationIndex(collectedClasses.toSet)
  }

}

object ClassPathResolver {

  private val annotations: mutable.Map[Class[? <: Annotation], mutable.Set[ResolvedAnnotation[?]]] =
    new mutable.HashMap[Class[? <: Annotation], mutable.Set[ResolvedAnnotation[?]]]()

  def findAnnotation[A <: Annotation](clazz: Class[A]): mutable.Set[ResolvedAnnotation[A]] = annotations(clazz).asInstanceOf[mutable.Set[ResolvedAnnotation[A]]]

  def process(packagePrefix: String, classLoader: ClassLoader): Set[ResolvedClass] = {
    val classPath = ClassPath.from(classLoader)

    val classes = classPath.getAllClasses.asScala
      .filter(info => info.getPackageName.startsWith(packagePrefix))
      .map(info => {
        val clazz = info.load()

        TypeResolver.resolve(clazz)
      })
      .toSet

    annotationIndex(classes)

    classes
  }


  private def annotationIndex(classes: Set[ResolvedClass]): Unit = {
    classes.foreach(clazz => {
      clazz.declaredAnnotations.foreach(annotation => {
        val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
        resolvedAnnotations.addOne(ResolvedAnnotation(annotation, clazz))
      })

      clazz.declaredConstructors.foreach(constructor => {
        constructor.declaredAnnotations.foreach(annotation => {
          val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
          resolvedAnnotations.addOne(ResolvedAnnotation(annotation, constructor))
        })

        constructor.parameters.foreach(parameter => {
          parameter.declaredAnnotations.foreach(annotation => {
            val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
            resolvedAnnotations.addOne(ResolvedAnnotation(annotation, parameter))
          })
        })
      })

      clazz.declaredFields.foreach(field => {
        field.declaredAnnotations.foreach(annotation => {
          val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
          resolvedAnnotations.addOne(ResolvedAnnotation(annotation, field))
        })
      })

      clazz.declaredMethods.foreach(method => {
        method.declaredAnnotations.foreach(annotation => {
          val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
          resolvedAnnotations.addOne(ResolvedAnnotation(annotation, method))
        })

        method.parameters.foreach(parameter => {
          parameter.declaredAnnotations.foreach(annotation => {
            val resolvedAnnotations = annotations.getOrElseUpdate(annotation.annotationType(), mutable.HashSet())
            resolvedAnnotations.addOne(ResolvedAnnotation(annotation, parameter))
          })
        })
      })


    })
  }
}
