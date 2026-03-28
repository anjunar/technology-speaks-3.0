package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.{ErrorRequest, ErrorRequestException}
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.bind.annotation.{ExceptionHandler, RestControllerAdvice}

import java.io.{PrintWriter, StringWriter}
import java.time.OffsetDateTime
import java.util.{LinkedHashMap, Map as JavaMap}

@RestControllerAdvice
class GlobalExceptionHandler(val txManager: PlatformTransactionManager) {

  @ExceptionHandler(Array(classOf[ErrorRequestException]))
  def handleBusinessException(ex: ErrorRequestException): ResponseEntity[java.util.List[ErrorRequest]] = {
    val definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED)
    val transaction = txManager.getTransaction(definition)
    txManager.rollback(transaction)

    ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.APPLICATION_JSON)
      .body(ex.errors)
  }

  @ExceptionHandler(Array(classOf[Throwable]))
  def handleUnexpectedException(ex: Throwable, request: HttpServletRequest): ResponseEntity[JavaMap[String, Object]] = {
    rollbackCurrentTransaction()

    val body = new LinkedHashMap[String, Object]()
    body.put("timestamp", OffsetDateTime.now().toString)
    body.put("status", Int.box(HttpStatus.INTERNAL_SERVER_ERROR.value()))
    body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase)
    body.put("exception", ex.getClass.getName)
    body.put("message", Option(ex.getMessage).getOrElse(ex.getClass.getSimpleName))
    body.put("path", request.getRequestURI)
    body.put("trace", stackTraceOf(ex))

    ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .contentType(MediaType.APPLICATION_JSON)
      .body(body)
  }

  private def rollbackCurrentTransaction(): Unit = {
    val definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED)
    val transaction = txManager.getTransaction(definition)
    txManager.rollback(transaction)
  }

  private def stackTraceOf(ex: Throwable): String = {
    val writer = new StringWriter()
    val printWriter = new PrintWriter(writer)
    ex.printStackTrace(printWriter)
    printWriter.flush()
    writer.toString
  }

}
