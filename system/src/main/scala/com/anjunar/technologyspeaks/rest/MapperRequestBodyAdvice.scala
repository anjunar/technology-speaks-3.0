package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.intermediate.JsonParser
import com.anjunar.json.mapper.intermediate.model.JsonObject
import com.anjunar.json.mapper.provider.DTO
import com.anjunar.json.mapper.{EntityLoader, JsonMapper}
import com.anjunar.scala.universe.TypeResolver
import jakarta.persistence.EntityManager
import jakarta.validation.Validator
import org.springframework.context.ApplicationContext
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice

import java.lang.reflect.Type
import java.util.UUID

@ControllerAdvice
class MapperRequestBodyAdvice(val entityManager: EntityManager, val validator: Validator, val applicationContext: ApplicationContext) extends RequestBodyAdvice {

  override def supports(
                         methodParameter: MethodParameter,
                         targetType: Type,
                         converterType: Class[? <: HttpMessageConverter[?]]
                       ): Boolean =
    classOf[MapperHttpMessageConverter].isAssignableFrom(converterType)

  override def beforeBodyRead(
                               inputMessage: HttpInputMessage,
                               parameter: MethodParameter,
                               targetType: Type,
                               converterType: Class[? <: HttpMessageConverter[?]]
                             ): HttpInputMessage =
    inputMessage

  override def afterBodyRead(body: Any,
                             inputMessage: HttpInputMessage,
                             parameter: MethodParameter,
                             targetType: Type,
                             converterType: Class[? <: HttpMessageConverter[?]]): Any =
    body match {
      case text: String =>
        JsonParser.parse(text) match {
          case jsonObject: JsonObject =>
            val resolvedClass = TypeResolver.resolve(targetType)
            val idNode = jsonObject.value.get("id")

            val instance =
              if (idNode == null) {
                resolvedClass.raw.getConstructor().newInstance()
              } else {
                val primaryKey = UUID.fromString(idNode.value.toString)
                val entity = entityManager.find(resolvedClass.raw, primaryKey)
                if (entity != null) entity else resolvedClass.raw.getConstructor().newInstance()
              }

            val annotation = parameter.getMethodAnnotation(classOf[EntityGraph])
            val entityGraph =
              if (annotation == null) null
              else entityManager.getEntityGraph(annotation.value)

            val loader = new EntityLoader {
              override def load(id: UUID, clazz: Class[?]): Any =
                entityManager.find(clazz, id)
            }

            JsonMapper.deserialize(jsonObject, instance, resolvedClass, entityGraph, loader, [T] => (clazz: Class[T]) => applicationContext.getBean(clazz) , validator)
          case _ =>
            throw new IllegalArgumentException("body must be a json object")
        }
      case _ =>
        throw new IllegalArgumentException("body must be a string")
    }

  override def handleEmptyBody(
                                body: Any,
                                inputMessage: HttpInputMessage,
                                parameter: MethodParameter,
                                targetType: Type,
                                converterType: Class[? <: HttpMessageConverter[?]]
                              ): Any =
    body

}
