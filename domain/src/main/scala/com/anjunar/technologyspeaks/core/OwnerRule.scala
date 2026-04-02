package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.VisibilityRule
import com.anjunar.scala.universe.introspector.AbstractProperty
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.security.IdentityHolder
import org.springframework.stereotype.Component

@Component
class OwnerRule[E <: OwnerProvider & EntityProvider](val holder : IdentityHolder) extends VisibilityRule[E] {

  override def isVisible(instance: E, property: AbstractProperty): Boolean = true

  override def isWriteable(instance: E, property: AbstractProperty): Boolean = {
    if (instance.version == -1L) {
      true
    } else {
      holder.isAuthenticated && holder.user != null && holder.user.id == instance.owner().id
    }
  }

}
