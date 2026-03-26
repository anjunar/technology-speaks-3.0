package com.anjunar.technologyspeaks.shared.commentable

import com.anjunar.json.mapper.provider.EntityProvider
import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.AbstractEntitySchema
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{CascadeType, Entity, OneToMany, Table}
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.shared.commentable.FirstComment.Schema


@Entity
@Table(name = "Shared#FirstComment")
class FirstComment extends AbstractComment, EntityContext[FirstComment], LikeContainer.Interface {

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  val comments: java.util.List[SecondComment] = new java.util.ArrayList[SecondComment]()

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  override val likes: java.util.Set[Like] = new java.util.HashSet[Like]()

  override def owner(): EntityProvider = user

}

object FirstComment extends RepositoryContext[FirstComment] with SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[FirstComment](SpringContext.entityManager()) {
    @JsonbProperty val user = property(_.user)
    @JsonbProperty val editor = property(_.editor, new DefaultWritableRule[FirstComment]())
    @JsonbProperty val comments = property(_.comments, new DefaultWritableRule[FirstComment]())
    @JsonbProperty val likes = property(_.likes)
  }

}
