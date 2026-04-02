package com.anjunar.technologyspeaks.hibernate

import jakarta.persistence.EntityManager

trait EntityContext[E] {

  def persist()(using entityManager: EntityManager): Unit = {
    entityManager.persist(this)
  }

  def merge()(using entityManager: EntityManager): E =
    entityManager.merge(this.asInstanceOf[E])

  def remove()(using entityManager: EntityManager): Unit = {
    entityManager.remove(this)
  }

}
