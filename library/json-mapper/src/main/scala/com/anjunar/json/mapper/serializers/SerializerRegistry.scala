package com.anjunar.json.mapper.serializers

import java.time.temporal.{Temporal, TemporalAmount}
import java.util.{Locale, UUID}

object SerializerRegistry {

  def find[T](clazz: Class[T], instance: Any): Serializer[T] =
    (instance match {
      case _: String => new StringSerializer
      case _: java.util.Collection[?] => new ArraySerializer
      case _: Boolean => new BooleanSerializer
      case _: Array[Byte] => new ByteArraySerializer
      case _: Enum[?] => new EnumSerializer
      case _: Locale => new LocaleSerializer
      case _: java.util.Map[?, ?] => new MapSerializer
      case _: UUID => new UUIDSerializer
      case _: Number => new NumberSerializer
      case _: TemporalAmount => new TemporalAmountSerializer
      case _: Temporal => new TemporalSerializer
      case _ => new BeanSerializer
    }).asInstanceOf[Serializer[T]]

}
