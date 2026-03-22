package com.anjunar.technologyspeaks.documents

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core._
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.commentable.{CommentContainer, FirstComment}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeContainer}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence._
import org.hibernate.annotations.Type

import scala.beans.BeanProperty

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
class Issue(
  @Column(nullable = false) @JsonbProperty @BeanProperty var title: String = null
) extends AbstractEntity with OwnerProvider with EntityContext[Issue] with LikeContainer.Interface with CommentContainer.Interface {

  def this() = this(null)

  @ManyToOne(optional = false)
  @JsonbProperty
  @BeanProperty
  var document: Document = null

  @ManyToOne(optional = false)
  @JsonbProperty
  @BeanProperty
  var user: User = null

  @Column(columnDefinition = "jsonb")
  @Type(value = classOf[NodeType])
  @JsonbProperty
  @BeanProperty
  var editor: Node = null

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  @BeanProperty
  override val likes: java.util.Set[Like] = new java.util.HashSet[Like]()

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @JsonbProperty
  @BeanProperty
  override val comments: java.util.Set[FirstComment] = new java.util.HashSet[FirstComment]()

  override def owner(): EntityProvider = user

}

object Issue extends RepositoryContext[Issue] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Issue] {
    @JsonbProperty val title = property[String]("title", classOf[String], new OwnerRule[Issue]())
    @JsonbProperty val user = property[User]("user", classOf[User], new OwnerRule[Issue]())
    @JsonbProperty val editor = property[Node]("editor", classOf[Node], new OwnerRule[Issue]())
    @JsonbProperty val likes = property[java.util.Set[Like]]("likes", classOf[java.util.Set[?]], new OwnerRule[Issue](), classOf[Like])
  }

}
