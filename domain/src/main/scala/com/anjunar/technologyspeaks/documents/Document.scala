package com.anjunar.technologyspeaks.documents

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.*
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import org.hibernate.annotations.Type

import scala.annotation.meta.field
import scala.beans.BeanProperty

@Entity
@Table(name = "Documents#Document")
@NamedEntityGraphs(
  value = Array(
    new NamedEntityGraph(
      name = "Document.full",
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
class Document(
  @Column(nullable = false) @(JsonbProperty @field) var title: String = null
) extends AbstractEntity with EntityContext[Document] with OwnerProvider {

  def this() = this(null)

  @ManyToOne(optional = false)
  @JsonbProperty
    var user: User = null

  @Column(columnDefinition = "jsonb")
  @Type(value = classOf[NodeType])
  @JsonbProperty
    var editor: Node = null

  override def owner(): EntityProvider = user

}

object Document extends RepositoryContext[Document] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[Document] {
    @JsonbProperty val title = property[String]("title", classOf[String], new OwnerRule[Document]())
    @JsonbProperty val user = property[User]("user", classOf[User], new OwnerRule[Document]())
    @JsonbProperty val editor = property[Node]("editor", classOf[Node], new OwnerRule[Document]())
  }

}
