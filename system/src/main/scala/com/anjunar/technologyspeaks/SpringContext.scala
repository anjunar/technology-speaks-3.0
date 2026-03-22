package com.anjunar.technologyspeaks

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

object SpringContext {

  def entityManager(): EntityManager =
    context.getBean(classOf[EntityManager])

  def getBean[C](clazz: Class[C]): C =
    context.getBean(clazz)

  var context: ApplicationContext = null

}
