package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.EntityContext
import jakarta.persistence.{Entity, Inheritance, InheritanceType, ManyToOne, OneToMany, Table}

import scala.beans.BeanProperty

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "Core#EntityView")
class EntityView extends AbstractEntity with EntityContext[EntityView] {

  @ManyToOne(optional = false)
    var user: User = null

  @OneToMany
    val properties: java.util.Set[ManagedProperty] = new java.util.HashSet[ManagedProperty]()

}
