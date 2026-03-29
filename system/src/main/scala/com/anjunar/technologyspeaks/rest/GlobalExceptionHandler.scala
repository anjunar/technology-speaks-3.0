package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.{ErrorRequest, ErrorRequestException}
import com.typesafe.scalalogging.Logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.bind.annotation.{ExceptionHandler, RestControllerAdvice}
import org.springframework.web.server.ResponseStatusException

import java.io.{PrintWriter, StringWriter}
import java.time.OffsetDateTime
import java.util
import java.util.{LinkedHashMap, Map as JavaMap}

@RestControllerAdvice
class GlobalExceptionHandler(val txManager: PlatformTransactionManager) {

  val log = Logger[GlobalExceptionHandler]

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

  @ExceptionHandler(Array(classOf[ResponseStatusException]))
  def handleResponseStatusException(ex: ResponseStatusException, request: HttpServletRequest): ResponseEntity[JavaMap[String, Object]] = {
    rollbackCurrentTransaction()

    val status = HttpStatus.valueOf(ex.getStatusCode.value())
    val body = new util.LinkedHashMap[String, Object]()
    body.put("timestamp", OffsetDateTime.now().toString)
    body.put("status", Int.box(status.value()))
    body.put("error", status.getReasonPhrase)
    body.put("exception", ex.getClass.getName)
    body.put("message", Option(ex.getReason).filter(! _.isBlank).getOrElse(status.getReasonPhrase))
    body.put("path", request.getRequestURI)

    log.error(s"[$status] ${ex.getMessage}", ex)

    ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_JSON)
      .body(body)
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

    log.error(ex.getMessage, ex)

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
