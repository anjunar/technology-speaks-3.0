package jfx.core.meta

import com.anjunar.scala.enterprise.macros.{ClassAnnotationMacros, PropertyAccess, PropertyMacros}
import com.anjunar.scala.enterprise.macros.reflection.SimpleClass

import scala.reflect.ClassTag

class Meta[E](val properties: Seq[PropertyAccess[E, ?]])

object Meta {

  inline def apply[E]()(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    new Meta[E](properties)
  }

  inline def apply[E](factory: () => E)(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    val simpleClass = ClassAnnotationMacros.describeClass[E]
    val typeName = extractJsonTypeName(simpleClass)
    jfx.core.meta.ClassLoader.register(factory, simpleClass.copy(typeName = typeName))
    new Meta[E](properties)
  }

  inline def apply[E](factory: () => E, typeName: String)(using ClassTag[E]) : Meta[E] = {
    val properties = PropertyMacros.describeProperties[E]
    val simpleClass = ClassAnnotationMacros.describeClass[E]
    jfx.core.meta.ClassLoader.register(factory, simpleClass.copy(typeName = typeName))
    new Meta[E](properties)
  }

  private def extractJsonTypeName(simpleClass: SimpleClass[?]): String = {
    // Prüfe ob @JsonType Annotation vorhanden ist
    simpleClass.annotations.find(_.annotationClassName == "jfx.json.JsonType") match {
      case Some(ann) =>
        ann.parameters.getOrElse("value", simpleClass.typeName.split('.').last).asInstanceOf[String]
      case None =>
        // Verwende SimpleName statt FullName
        simpleClass.typeName.split('.').last
    }
  }

}