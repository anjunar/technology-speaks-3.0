package com.anjunar.technologyspeaks.core

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.{Propagation, Transactional}

@Service
class ManagedPropertyBootstrapService {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  def findOrCreateManagedProperty(ownerId: java.util.UUID, propertyName: String): ManagedProperty = {
    if (ownerId == null || propertyName == null || propertyName.trim.isEmpty) {
      return null
    }

    val owner = User.find(ownerId)
    if (owner == null) {
      return null
    }

    val entityView = findOrCreateEntityView(owner)
    var managedProperty = entityView.properties.stream().filter(_.name == propertyName).findFirst().orElse(null)

    if (managedProperty == null) {
      managedProperty = new ManagedProperty(propertyName, false)
      managedProperty.view = entityView
      entityView.properties.add(managedProperty)
      managedProperty.persist()
    }

    managedProperty
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  def findOrCreateEntityView(ownerId: java.util.UUID): EntityView = {
    if (ownerId == null) {
      return null
    }

    val owner = User.find(ownerId)
    if (owner == null) {
      return null
    }

    findOrCreateEntityView(owner)
  }

  private def findOrCreateEntityView(owner: User): EntityView = {
    var entityView = User.findViewByUser(owner)

    if (entityView == null) {
      entityView = new User.View
      entityView.user = owner
      entityView.persist()
    }

    entityView
  }
}
