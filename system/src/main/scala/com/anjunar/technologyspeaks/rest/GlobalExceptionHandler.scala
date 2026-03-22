package com.anjunar.technologyspeaks.rest

import com.anjunar.json.mapper.{ErrorRequest, ErrorRequestException}
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.bind.annotation.{ExceptionHandler, RestControllerAdvice}

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

}
