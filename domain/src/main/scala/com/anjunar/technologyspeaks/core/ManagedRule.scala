package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.scala.universe.introspector.AbstractProperty
import com.anjunar.technologyspeaks.followers.RelationShip
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.security.IdentityHolder

import scala.jdk.CollectionConverters.*

class ManagedRule[E <: OwnerProvider & EntityProvider] extends VisibilityRule[E] {

  val holder = SpringContext.getBean(classOf[IdentityHolder])

  override def isVisible(instance: E, property: AbstractProperty): Boolean = {
    if (instance == null) {
      return false
    }

    if (!holder.isAuthenticated() || holder.user == null) {
      return false
    }

    val owner = User.find(instance.owner().id)
    if (owner == null) {
      return false
    }

    if (holder.user.id == owner.id) {
      return true
    }

    var entityView = User.findViewByUser(owner)

    if (entityView == null) {
      entityView = new User.View
      entityView.user = owner
      entityView.persist()
    }

    var managedProperty = entityView.properties.stream().filter(propertyValue => propertyValue.name == property.name).findFirst().orElse(null)

    if (managedProperty == null) {
      managedProperty = new ManagedProperty(property.name, false)
      managedProperty.view = entityView
      entityView.properties.add(managedProperty)
      managedProperty.persist()
    }

    if (managedProperty.visibleForAll) {
      return true
    }

    if (managedProperty.users.stream().anyMatch(user => user.id == holder.user.id)) {
      return true
    }

    if (managedProperty.groups.isEmpty) {
      return false
    }

    val relationShip = RelationShip.query("follower" -> holder.user, "user" -> owner)
    if (relationShip == null) {
      return false
    }

    val allowedGroupIds = managedProperty.groups.asScala.map(_.id).toSet
    relationShip.groups.asScala.exists(group => allowedGroupIds.contains(group.id))
  }

  override def isWriteable(instance: E, property: AbstractProperty): Boolean = {
    if (instance.version == -1L) {
      true
    } else {
      holder.user.id == instance.owner().id
    }
  }

}
