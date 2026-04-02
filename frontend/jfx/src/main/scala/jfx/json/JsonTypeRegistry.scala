package jfx.json

import reflect.{ClassDescriptor, TypeDescriptor}
import scala.collection.mutable

object JsonTypeRegistry {

  private val typeByName: mutable.Map[String, ClassDescriptor] = mutable.Map.empty
  private val nameByType: mutable.Map[String, String] = mutable.Map.empty

  def register(descriptor: ClassDescriptor): Unit = {
    descriptor.annotations.find(_.annotationClassName == "jfx.json.JsonType") match {
      case Some(ann) =>
        val typeName = ann.parameters.getOrElse("value", "").toString
        if (typeName.nonEmpty) {
          typeByName += typeName -> descriptor
          nameByType += descriptor.typeName -> typeName
        }
      case None =>
    }
  }

  def resolveType(jsonTypeName: String): Option[ClassDescriptor] =
    typeByName.get(jsonTypeName)

  def getTypeName(typeName: String): Option[String] =
    nameByType.get(typeName)

  def getAllRegistered: Iterable[ClassDescriptor] = typeByName.values
}
