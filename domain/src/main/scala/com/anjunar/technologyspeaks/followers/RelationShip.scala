package com.anjunar.technologyspeaks.followers

import com.anjunar.json.mapper.provider.OwnerProvider
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.core._
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence._

import scala.beans.BeanProperty

@Entity
@Table(name = "Followers#RelationShip")
@NamedEntityGraphs(
  value = Array(
    new NamedEntityGraph(
      name = "RelationShip.full",
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
        ),
        new NamedSubgraph(
          name = "group",
          `type` = classOf[Group],
          attributeNodes = Array(
            new NamedAttributeNode("id"),
            new NamedAttributeNode("name")
          )
        )
      ),
      attributeNodes = Array(
        new NamedAttributeNode("id"),
        new NamedAttributeNode(value = "follower", subgraph = "user"),
        new NamedAttributeNode(value = "groups", subgraph = "group")
      )
    )
  )
)
class RelationShip extends AbstractEntity with OwnerProvider with EntityContext[RelationShip] {

  @ManyToOne(optional = false)
  @BeanProperty
  var user: User = null

  @ManyToOne(optional = false)
  @JsonbProperty
  @BeanProperty
  var follower: User = null

  @ManyToMany
  @JsonbProperty
  @BeanProperty
  val groups: java.util.Set[Group] = new java.util.HashSet[Group]()

  override def owner(): User = user

}

object RelationShip extends RepositoryContext[RelationShip] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  class Schema extends AbstractEntitySchema[RelationShip] {
    @JsonbProperty val follower = property[User]("follower", classOf[User], new OwnerRule[RelationShip]())
    @JsonbProperty val groups = property[java.util.Set[Group]]("groups", classOf[java.util.Set[?]], new OwnerRule[RelationShip](), classOf[Group])
  }

}
