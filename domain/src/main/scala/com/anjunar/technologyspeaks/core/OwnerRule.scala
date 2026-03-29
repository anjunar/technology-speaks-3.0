package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.scala.universe.introspector.AbstractProperty
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.security.IdentityHolder

class OwnerRule[E <: OwnerProvider & EntityProvider] extends VisibilityRule[E] {

  val holder = SpringContext.getBean(classOf[IdentityHolder])

  override def isVisible(instance: E, property: AbstractProperty): Boolean = true

  override def isWriteable(instance: E, property: AbstractProperty): Boolean = {
    if (instance.version == -1L) {
      true
    } else {
      holder.isAuthenticated && holder.user != null && holder.user.id == instance.owner().id
    }
  }

}
