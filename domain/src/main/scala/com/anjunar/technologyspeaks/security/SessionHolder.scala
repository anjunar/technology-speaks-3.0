package com.anjunar.technologyspeaks.security

import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.SessionScope

import java.util.UUID
import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Service
@SessionScope
class SessionHolder {

  var user: UUID = uninitialized

  var credentials: UUID = uninitialized

  def invalidate(): Unit = {
    user = null
    credentials = null
  }

}
