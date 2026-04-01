package jfx.core.meta

import com.anjunar.scala.enterprise.macros.reflection.SimpleClass
import com.anjunar.scala.enterprise.macros.{ClassAnnotationMacros, MetaClassLoader}

import scala.reflect.ClassTag

type Meta[E] = SimpleClass[E]

object Meta {

  inline def apply[E](factory: () => E)(using classTag : ClassTag[E]): Meta[E] = {
    val simpleClass = ClassAnnotationMacros.describeClass[E]
    ClassLoader.register(factory, simpleClass)
    MetaClassLoader.register(simpleClass, classTag.runtimeClass, factory)
    simpleClass
  }

}
