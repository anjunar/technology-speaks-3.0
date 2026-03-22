package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.provider.DTO
import org.springframework.http.{HttpInputMessage, HttpOutputMessage, MediaType}
import org.springframework.http.converter.AbstractHttpMessageConverter

import java.nio.charset.StandardCharsets

class MapperHttpMessageConverter extends AbstractHttpMessageConverter[Any](MediaType.APPLICATION_JSON) {

  override def supports(clazz: Class[?]): Boolean =
    classOf[DTO].isAssignableFrom(clazz) || classOf[java.util.Collection[?]].isAssignableFrom(clazz)

  override def readInternal(clazz: Class[?], inputMessage: HttpInputMessage): Any =
    new String(inputMessage.getBody.readAllBytes(), StandardCharsets.UTF_8)

  override def writeInternal(body: Any, outputMessage: HttpOutputMessage): Unit = {
    val text =
      body match {
        case value: String => value
        case _ => body.toString
      }

    val bytes = text.getBytes(StandardCharsets.UTF_8)
    outputMessage.getHeaders.setContentType(MediaType.APPLICATION_JSON)
    val stream = outputMessage.getBody
    try {
      stream.write(bytes)
    } finally {
      stream.close()
    }
  }

}
