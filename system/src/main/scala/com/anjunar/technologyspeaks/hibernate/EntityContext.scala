package com.anjunar.technologyspeaks.hibernate

import com.anjunar.technologyspeaks.SpringContext
import jakarta.persistence.EntityManager

trait EntityContext[E] {

  def entityManager(): EntityManager = SpringContext.entityManager()

  def persist(): Unit = {
    entityManager().persist(this)
  }

  def merge(): E =
    entityManager().merge(this.asInstanceOf[E])

  def remove(): Unit = {
    entityManager().remove(this)
  }

}
