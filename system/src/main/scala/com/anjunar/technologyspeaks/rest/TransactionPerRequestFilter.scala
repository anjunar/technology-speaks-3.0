package com.anjunar.technologyspeaks.rest

import jakarta.servlet.FilterChain
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.{PlatformTransactionManager, TransactionDefinition}
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class TransactionPerRequestFilter(private val txManager: PlatformTransactionManager)
    extends OncePerRequestFilter {

  override def shouldNotFilter(request: HttpServletRequest): Boolean = {
    val path = request.getRequestURI
    !path.startsWith("/service")
  }

  override def doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ): Unit = {
    val definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED)
//    definition.setReadOnly(request.getMethod == "GET" || request.getMethod == "HEAD")
    val status = txManager.getTransaction(definition)

    try {
      filterChain.doFilter(request, response)
      if (!status.isRollbackOnly) {
        txManager.commit(status)
      }
    } catch {
      case throwable: Throwable =>
        txManager.rollback(status)
        throw throwable
    }
  }

}
