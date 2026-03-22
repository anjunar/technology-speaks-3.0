package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.intermediate.{JsonGenerator, JsonParser}
import com.anjunar.json.mapper.intermediate.model.JsonNode
import org.springframework.http.{HttpInputMessage, HttpOutputMessage, MediaType}
import org.springframework.http.converter.AbstractHttpMessageConverter

import java.nio.charset.StandardCharsets

class JsonHttpMessageConverter extends AbstractHttpMessageConverter[Any](MediaType.APPLICATION_JSON) {

  override def supports(clazz: Class[?]): Boolean =
    classOf[JsonNode].isAssignableFrom(clazz)

  override def readInternal(clazz: Class[?], inputMessage: HttpInputMessage): Any = {
    val text = new String(inputMessage.getBody.readAllBytes(), StandardCharsets.UTF_8)
    JsonParser.parse(text)
  }

  override def writeInternal(body: Any, outputMessage: HttpOutputMessage): Unit = {
    val text = JsonGenerator.generate(body.asInstanceOf[JsonNode])
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
