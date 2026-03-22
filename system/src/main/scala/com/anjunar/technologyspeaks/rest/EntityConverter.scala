package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.provider.EntityProvider
import com.anjunar.technologyspeaks.SpringContext
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.{ConditionalGenericConverter, GenericConverter}
import org.springframework.stereotype.Component

import java.util.{Set, UUID}

@Component
class EntityConverter extends ConditionalGenericConverter {

  override def getConvertibleTypes(): java.util.Set[GenericConverter.ConvertiblePair] =
    Set.of(new GenericConverter.ConvertiblePair(classOf[String], classOf[EntityProvider]))

  override def matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean =
    classOf[EntityProvider].isAssignableFrom(targetType.getType)

  override def convert(source: Any, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any = {
    if (source == null || source.asInstanceOf[String].isBlank) {
      return null
    }

    val entityManager = SpringContext.entityManager()
    try {
      val id = UUID.fromString(source.asInstanceOf[String])
      entityManager.find(targetType.getType, id)
    } catch {
      case _: IllegalArgumentException => null
    }
  }

}
