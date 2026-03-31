package com.anjunar.technologyspeaks.documents

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core.*
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, NotNull}
import org.hibernate.annotations.Type

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.compiletime.uninitialized
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.documents.Document.Schema


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
        new NamedAttributeNode("bookname"),
        new NamedAttributeNode("title")
      )
    )
  )
)
class Document(@(NotBlank @field)
               @(Column @field)(nullable = false)
               @(JsonbProperty @field)
               var title: String)
  extends AbstractEntity, EntityContext[Document], OwnerProvider {

  def this() = this(null)

  @ManyToOne(optional = false)
  @JsonbProperty
  var user: User = uninitialized

  @(Column @field)
  @JsonbProperty
  var bookname: String = null

  @NotNull
  @Column(columnDefinition = "jsonb")
  @Type(value = classOf[NodeType])
  @JsonbProperty
  var editor: Node = uninitialized

  override def owner(): EntityProvider = user

}

object Document extends RepositoryContext[Document] with SchemaProvider[Schema] {

  class Schema extends AbstractEntitySchema[Document](SpringContext.entityManager()) {
    @JsonbProperty val title = property(_.title, new OwnerRule[Document]())
    @JsonbProperty val bookname = property(_.bookname, new OwnerRule[Document]())
    @JsonbProperty val user = property(_.user, new OwnerRule[Document]())
    @JsonbProperty val editor = property(_.editor, new OwnerRule[Document]())
  }

}
