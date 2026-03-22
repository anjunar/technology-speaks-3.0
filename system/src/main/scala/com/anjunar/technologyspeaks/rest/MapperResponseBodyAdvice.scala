package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.JsonMapper
import com.anjunar.scala.universe.TypeResolver
import jakarta.persistence.EntityManager
import org.springframework.core.MethodParameter
import org.springframework.http.{MediaType, ResponseEntity}
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.{ServerHttpRequest, ServerHttpResponse}
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@ControllerAdvice
class MapperResponseBodyAdvice(val entityManager: EntityManager) extends ResponseBodyAdvice[Any] {

  override def supports(returnType: MethodParameter, converterType: Class[? <: HttpMessageConverter[?]]): Boolean =
    classOf[MapperHttpMessageConverter].isAssignableFrom(converterType)

  override def beforeBodyWrite(
    body: Any,
    returnType: MethodParameter,
    selectedContentType: MediaType,
    selectedConverterType: Class[? <: HttpMessageConverter[?]],
    request: ServerHttpRequest,
    response: ServerHttpResponse
  ): Any = {
    var resolvedClass = TypeResolver.resolve(returnType.getGenericParameterType)

    if (resolvedClass.raw == classOf[ResponseEntity[?]]) {
      resolvedClass = resolvedClass.typeArguments(0)
    }

    val annotation = returnType.getMethodAnnotation(classOf[EntityGraph])
    val entityGraph =
      if (annotation == null) null
      else entityManager.getEntityGraph(annotation.value)

    JsonMapper.serialize(body, resolvedClass, entityGraph)
  }

}
