package jfx.core.meta

import com.anjunar.scala.enterprise.macros.reflection.SimpleClass
import com.anjunar.scala.enterprise.macros.{ClassAnnotationMacros, MetaClassLoader}

import scala.reflect.ClassTag

type Meta[E] = SimpleClass[E]

object Meta {

  inline def apply[E]()(using ClassTag[E]): Meta[E] = {
    ClassAnnotationMacros.describeClass[E]
  }

  inline def apply[E](factory: () => E)(using ClassTag[E]): Meta[E] = {
    val simpleClass = ClassAnnotationMacros.describeClass[E]
    ClassLoader.register(factory, simpleClass)
    MetaClassLoader.register(simpleClass, factory)
    simpleClass
  }

  inline def apply[E](factory: () => E, typeName: String)(using ClassTag[E]): Meta[E] = {
    val simpleClass = ClassAnnotationMacros.describeClass[E]
    val registered = simpleClass.copy(typeName = typeName)
    ClassLoader.register(factory, registered)
    MetaClassLoader.register(simpleClass, factory)
    registered
  }

}
