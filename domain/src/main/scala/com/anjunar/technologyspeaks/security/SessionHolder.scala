package com.anjunar.technologyspeaks.security

import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.SessionScope

import java.util.UUID
import scala.beans.BeanProperty

@Service
@SessionScope
class SessionHolder {

    var user: UUID = null

    var credentials: UUID = null

  def invalidate(): Unit = {
    user = null
    credentials = null
  }

}
