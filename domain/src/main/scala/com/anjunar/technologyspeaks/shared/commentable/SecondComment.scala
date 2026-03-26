package com.anjunar.technologyspeaks.shared.commentable

import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.AbstractEntitySchema
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{CascadeType, Entity, OneToMany, Table}

import scala.beans.BeanProperty
import com.anjunar.technologyspeaks.SpringContext


@Entity
@Table(name = "Shared#SecondComment")
class SecondComment extends AbstractComment with EntityContext[SecondComment] with LikeContainer.Interface {

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
    override val likes: java.util.Set[Like] = new java.util.HashSet[Like]()

}

object SecondComment extends RepositoryContext[SecondComment] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[SecondComment](SpringContext.entityManager()) {
    @JsonbProperty val user = property(_.user)
    @JsonbProperty val editor = property(_.editor, new DefaultWritableRule[SecondComment]())
    @JsonbProperty val likes = property(_.likes)
  }

}
