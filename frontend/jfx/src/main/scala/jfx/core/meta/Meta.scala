package jfx.core.meta

import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}

class Meta[E](val properties: Seq[PropertyAccess[E, ?]])

object Meta {

  inline def apply[E]() : Meta[E] = new Meta[E](PropertyMacros.describeProperties[E])

}