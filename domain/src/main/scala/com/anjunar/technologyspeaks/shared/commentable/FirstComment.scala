package com.anjunar.technologyspeaks.shared.commentable

import com.anjunar.json.mapper.provider.EntityProvider
import com.anjunar.json.mapper.schema.{DefaultWritableRule, EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.AbstractEntitySchema
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{CascadeType, Entity, OneToMany, Table}

import scala.beans.BeanProperty

@Entity
@Table(name = "Shared#FirstComment")
class FirstComment extends AbstractComment with EntityContext[FirstComment] with LikeContainer.Interface {

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
    val comments: java.util.List[SecondComment] = new java.util.ArrayList[SecondComment]()

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
    override val likes: java.util.Set[Like] = new java.util.HashSet[Like]()

  override def owner(): EntityProvider = user

}

object FirstComment extends RepositoryContext[FirstComment] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[FirstComment] {
    @JsonbProperty val user = property[com.anjunar.technologyspeaks.core.User]("user", classOf[com.anjunar.technologyspeaks.core.User])
    @JsonbProperty val editor = property[com.anjunar.technologyspeaks.shared.editor.Node]("editor", classOf[com.anjunar.technologyspeaks.shared.editor.Node], new DefaultWritableRule[FirstComment]())
    @JsonbProperty val comments = property[java.util.List[SecondComment]]("comments", classOf[java.util.List[?]], new DefaultWritableRule[FirstComment](), classOf[SecondComment])
    @JsonbProperty val likes = property[java.util.Set[Like]]("likes", classOf[java.util.Set[?]], collectionType = classOf[Like])
  }

}
