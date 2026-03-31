package com.anjunar.technologyspeaks.documents

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.*
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.commentable.{CommentContainer, FirstComment}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, NotNull}
import org.hibernate.annotations.Type

import scala.annotation.meta.field
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.documents.Issue.Schema


@Entity
@Table(name = "Documents#Issue")
@NamedEntityGraphs(
  value = Array(
    new NamedEntityGraph(
      name = "Issue.full",
      subgraphs = Array(
        new NamedSubgraph(
          name = "image",
          `type` = classOf[Media],
          attributeNodes = Array(
            new NamedAttributeNode("id")
          )
        ),
        new NamedSubgraph(
          name = "userInfo",
          `type` = classOf[UserInfo],
          attributeNodes = Array(
            new NamedAttributeNode("id"),
            new NamedAttributeNode("firstName"),
            new NamedAttributeNode("lastName")
          )
        ),
        new NamedSubgraph(
          name = "user",
          `type` = classOf[User],
          attributeNodes = Array(
            new NamedAttributeNode("id"),
            new NamedAttributeNode("nickName"),
            new NamedAttributeNode(value = "image", subgraph = "image"),
            new NamedAttributeNode(value = "info", subgraph = "userInfo")
          )
        )
      ),
      attributeNodes = Array(
        new NamedAttributeNode("id"),
        new NamedAttributeNode("modified"),
        new NamedAttributeNode("created"),
        new NamedAttributeNode(value = "user", subgraph = "user"),
        new NamedAttributeNode("editor"),
        new NamedAttributeNode("title")
      )
    )
  )
)
class Issue(@(NotBlank @field)
            @(Column @field)(nullable = false)
            @(JsonbProperty @field)
            var title: String)
  extends AbstractEntity, OwnerProvider, EntityContext[Issue], LikeContainer.Interface, CommentContainer.Interface {

  def this() = this(null)

  @ManyToOne(optional = false)
  @JsonbProperty
  var document: Document = uninitialized

  @ManyToOne(optional = false)
  @JsonbProperty
  var user: User = uninitialized

  @NotNull
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

object Issue extends RepositoryContext[Issue] with SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[Issue](SpringContext.entityManager()) {
    @JsonbProperty val title = property(_.title, new OwnerRule[Issue]())
    @JsonbProperty val user = property(_.user, new OwnerRule[Issue]())
    @JsonbProperty val editor = property(_.editor, new OwnerRule[Issue]())
    @JsonbProperty val likes = property(_.likes, new OwnerRule[Issue]())
  }

}
