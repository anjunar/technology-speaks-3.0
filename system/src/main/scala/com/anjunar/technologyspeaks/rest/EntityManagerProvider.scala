package com.anjunar.technologyspeaks.rest

import jakarta.persistence.EntityManager

trait EntityManagerProvider {
  
  def entityManager: EntityManager
  
  given EntityManager = entityManager

}
