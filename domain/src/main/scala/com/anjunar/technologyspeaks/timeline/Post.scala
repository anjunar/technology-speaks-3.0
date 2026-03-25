package com.anjunar.technologyspeaks.timeline

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, Property, SchemaProvider}
import com.anjunar.technologyspeaks.core.*
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.commentable.{CommentContainer, FirstComment}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import org.hibernate.annotations.Type

import java.util
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import java.util

@Entity
@Table(name = "Timeline#Post")
@NamedEntityGraphs(
  value = Array(
    new NamedEntityGraph(
      name = "Post.full",
      subgraphs = Array(
        new NamedSubgraph(
          name = "image",
          `type` = classOf[Media],
          attributeNodes = Array(
            new NamedAttributeNode("id")
          )
        ),
        new NamedSubgraph(
          name = "user",
          `type` = classOf[User],
          attributeNodes = Array(
            new NamedAttributeNode("id"),
            new NamedAttributeNode("nickName"),
            new NamedAttributeNode("image", subgraph = "image"),
            new NamedAttributeNode("info")
          )
        ),
        new NamedSubgraph(
          name = "likes",
          `type` = classOf[Like],
          attributeNodes = Array(
            new NamedAttributeNode("id"),
            new NamedAttributeNode("user", subgraph = "user")
          )
        )
      ),
      attributeNodes = Array(
        new NamedAttributeNode("id"),
        new NamedAttributeNode("created"),
        new NamedAttributeNode("modified"),
        new NamedAttributeNode("user", subgraph = "user"),
        new NamedAttributeNode("editor"),
        new NamedAttributeNode("likes", subgraph = "likes")
      )
    )
  )
)
class Post extends AbstractEntity, EntityContext[Post], OwnerProvider, LikeContainer.Interface, CommentContainer.Interface {

  @ManyToOne(optional = false)
  @JsonbProperty
  var user: User = uninitialized

  @Column(columnDefinition = "jsonb")
  @Type(value = classOf[NodeType])
  @JsonbProperty
  var editor: Node = uninitialized

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  override val likes: java.util.Set[Like] = new java.util.HashSet[Like]()

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  override val comments: java.util.Set[FirstComment] = new java.util.HashSet[FirstComment]()

  override def owner(): EntityProvider = user

}

object Post extends RepositoryContext[Post] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Post] {
    @JsonbProperty
    val user: Property[Post, User] = property(_.user, new OwnerRule[Post]())

    @JsonbProperty
    val editor: Property[Post, Node] = property(_.editor, new OwnerRule[Post]())

    @JsonbProperty
    val likes: Property[Post, util.Set[Like]] = property(_.likes, new OwnerRule[Post]())
  }

}
