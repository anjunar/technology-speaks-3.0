package jfx.core.meta

import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}

import scala.reflect.ClassTag

class Meta[E](val properties: Seq[PropertyAccess[E, ?]])

object Meta {

  inline def apply[E]()(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    new Meta[E](properties)
  }

  inline def apply[E](factory: () => E)(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    val typeName = JsonTypeMacro.getJsonTypeName[E]
    ClassLoader.classes.put(summon[ClassTag[E]].runtimeClass, (factory, typeName))
    new Meta[E](properties)
  }

  inline def apply[E](factory: () => E, typeName: String)(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    ClassLoader.classes.put(summon[ClassTag[E]].runtimeClass, (factory, typeName))
    new Meta[E](properties)
  }

}